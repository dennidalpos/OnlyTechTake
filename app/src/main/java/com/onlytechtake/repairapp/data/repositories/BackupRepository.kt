package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.OrderExportDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.storage.StoragePolicy
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import java.io.File
import java.io.IOException

class BackupRepository(
    private val context: Context,
    private val categoryDao: OrderCategoryDao,
    private val templateDao: MessageTemplateDao,
    private val settingDao: AppSettingDao,
    private val auditLogDao: AuditLogDao,
    private val backupReminderPreferences: com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences,
    private val securityPreferences: SecurityPreferences,
    private val orderExportDao: OrderExportDao,
    private val database: RepairAppDatabase,
    private val authPreferences: AuthPreferences,
    private val sessionStore: InMemorySessionStore,
    private val storageStatsService: com.onlytechtake.repairapp.data.services.StorageStatsService,
    private val mediaStorageProvider: MediaStorageProvider,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(context, auditLogDao = auditLogDao)

    private val metadataService =
        BackupMetadataService(categoryDao, templateDao, settingDao, clockMillis)
    private val dbBackupService = DatabaseBackupFileService(context)

    suspend fun exportConfiguration(
        actor: AuthenticatedUser,
        uri: Uri,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EXPORT_DATA)
            withContext(Dispatchers.IO) {
                writeText(uri, metadataService.buildConfigurationJson())
            }
            eventWriter.dataExported(actor.id, clockMillis())
        }

    suspend fun exportDatabase(
        actor: AuthenticatedUser,
        uri: Uri,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EXPORT_DATA)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException(context.getString(R.string.error_feature_pro_only))
            }
            withContext(Dispatchers.IO) {
                val dbFile = dbBackupService.databaseFile()
                require(dbFile.exists()) { context.getString(R.string.error_db_not_created) }
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    dbFile.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IOException(context.getString(R.string.error_destination_unavailable))
            }
            eventWriter.dataExported(actor.id, clockMillis())
        }

    suspend fun exportCompleteBackup(
        actor: AuthenticatedUser,
        uri: Uri,
    ): Result<Unit> =
        runCatching {
            StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
            PermissionService.requirePermission(actor, UserPermission.BACKUP_RESTORE)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException(context.getString(R.string.error_feature_pro_only))
            }

            val securitySettings = securityPreferences.settings.first()
            val password =
                securitySettings.backupEncryptionPassword.takeIf { securitySettings.isBackupEncryptionEnabled && it.isNotBlank() }

            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    ZipOutputStream(output, password?.toCharArray()).use { zip ->
                        zip.putTextEntry(
                            "metadata/backup.json",
                            metadataService.buildBackupMetadataJson(),
                            password,
                        )
                        zip.putTextEntry(
                            "config/configuration.json",
                            metadataService.buildConfigurationJson(),
                            password,
                        )
                        dbBackupService.addDatabaseFiles(zip, password)
                        BackupArchive.addDirectory(
                            zip = zip,
                            directory = mediaStorageProvider.getBaseMediaDir(),
                            entryDirectory = "files/${BackupArchive.CANONICAL_MEDIA_DIRECTORY}",
                            password = password,
                        )
                    }
                } ?: throw IOException(context.getString(R.string.error_destination_unavailable))
            }

            val fileName = displayName(uri)
            backupReminderPreferences.recordSuccessfulFullBackup(fileName)
            eventWriter.dataExported(actor.id, clockMillis())
        }.onFailure {
            backupReminderPreferences.recordFailedFullBackup()
        }

    suspend fun restoreCompleteBackup(
        actor: AuthenticatedUser,
        uri: Uri,
    ): Result<Unit> =
        runCatching {
            StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
            PermissionService.requirePermission(actor, UserPermission.BACKUP_RESTORE)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException(context.getString(R.string.error_feature_pro_only))
            }

            val securitySettings = securityPreferences.settings.first()
            val password = securitySettings.backupEncryptionPassword.takeIf { it.isNotBlank() }

            withContext(Dispatchers.IO) {
                val stagingDir =
                    File(context.cacheDir, RESTORE_WORK_DIR).also {
                        it.deleteRecursively()
                        check(it.mkdirs()) { context.getString(R.string.error_restore_area_unavailable) }
                    }
                try {
                    val extracted =
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            BackupArchive.extract(
                                context,
                                input,
                                stagingDir,
                                password?.toCharArray()
                            )
                        } ?: throw IOException(context.getString(R.string.error_backup_unavailable))
                    require(extracted.metadataText.contains("\"databaseVersion\": ${RepairAppDatabase.DATABASE_VERSION}")) {
                        context.getString(R.string.error_backup_incompatible)
                    }
                    val backupDbFile =
                        extracted.databaseFiles[RepairAppDatabase.DATABASE_NAME]
                            ?: throw IOException(context.getString(R.string.error_db_missing_in_backup))
                    dbBackupService.validateDatabase(backupDbFile)

                    // Atomic replacement: close, move current to backup, move new to current, delete backup
                    database.close()
                    RepairAppDatabase.closeInstance()

                    val currentDbFile = dbBackupService.databaseFile()
                    val oldDbBackupDir =
                        File(context.cacheDir, "db_backup_pre_restore").also {
                            it.deleteRecursively()
                            it.mkdirs()
                        }

                    // Snapshot current DB files
                    currentDbFile.parentFile
                        ?.listFiles { file ->
                            (file.name == currentDbFile.name) || file.name.startsWith("${currentDbFile.name}-")
                        }?.forEach { it.copyTo(File(oldDbBackupDir, it.name)) }

                    // Snapshot current media for rollback
                    val mediaDir = mediaStorageProvider.getBaseMediaDir()
                    val oldMediaBackupDir = File(context.cacheDir, "media_backup_pre_restore")
                    oldMediaBackupDir.deleteRecursively()
                    if (mediaDir.exists()) {
                        if (!mediaDir.renameTo(oldMediaBackupDir)) {
                            mediaDir.copyRecursively(oldMediaBackupDir)
                        }
                    }

                    try {
                        dbBackupService.replaceDatabaseFiles(extracted.databaseFiles)
                        replacePrivateFiles(stagingDir)

                        authPreferences.clear()
                        sessionStore.clear()

                        val now = clockMillis()
                        val restoredDatabase = RepairAppDatabase.getInstance(context)
                        try {
                            // Verify if actor exists in restored database
                            val actorExists = restoredDatabase.userDao().findById(actor.id) != null
                            val finalActorId = if (actorExists) actor.id else null

                            restoredDatabase.auditLogDao().upsert(
                                com.onlytechtake.repairapp.data.local.entities.AuditLogEntity(
                                    id =
                                        java.util.UUID
                                            .randomUUID()
                                            .toString(),
                                    action = com.onlytechtake.repairapp.data.local.entities.AuditAction.DATA_RESET,
                                    actorUserId = finalActorId,
                                    orderId = null,
                                    customerId = null,
                                    detailsJson = """{"operation":"backup_restored","at":"${
                                        formatTime(
                                            now,
                                        )
                                    }"}""",
                                    createdAt = now,
                                ),
                            )
                        } catch (auditError: Exception) {
                            // Don't let audit log failure invalidate a successful restore
                            android.util.Log.e(
                                "BackupRepository",
                                "Failed to write audit log after restore",
                                auditError,
                            )
                        } finally {
                            RepairAppDatabase.closeInstance()
                        }
                    } catch (e: Exception) {
                        // Rollback DB
                        oldDbBackupDir.listFiles()?.forEach {
                            it.copyTo(File(currentDbFile.parentFile, it.name), overwrite = true)
                        }
                        // Rollback Media
                        if (oldMediaBackupDir.exists()) {
                            mediaDir.deleteRecursively()
                            if (!oldMediaBackupDir.renameTo(mediaDir)) {
                                oldMediaBackupDir.copyRecursively(mediaDir)
                            }
                        }
                        throw e
                    } finally {
                        oldDbBackupDir.deleteRecursively()
                        oldMediaBackupDir.deleteRecursively()
                    }
                } finally {
                    stagingDir.deleteRecursively()
                }
            }
        }

    suspend fun exportOrdersXlsx(
        actor: AuthenticatedUser,
        uri: Uri,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EXPORT_DATA)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException(context.getString(R.string.error_feature_pro_only))
            }

            val securitySettings = securityPreferences.settings.first()
            val password =
                securitySettings.backupEncryptionPassword.takeIf { securitySettings.isBackupEncryptionEnabled && it.isNotBlank() }

            val exportDao = orderExportDao
            val labels =
                OrderExportLabels(
                    headers =
                        listOf(
                            context.getString(R.string.export_xlsx_header_order_number),
                            context.getString(R.string.export_xlsx_header_title),
                            context.getString(R.string.export_xlsx_header_status),
                            context.getString(R.string.export_xlsx_header_priority),
                            context.getString(R.string.export_xlsx_header_customer),
                            context.getString(R.string.export_xlsx_header_customer_type),
                            context.getString(R.string.export_xlsx_header_phone),
                            context.getString(R.string.export_xlsx_header_email),
                            context.getString(R.string.export_xlsx_header_category),
                            context.getString(R.string.export_xlsx_header_operator),
                            context.getString(R.string.export_xlsx_header_signer),
                            context.getString(R.string.export_xlsx_header_description),
                            context.getString(R.string.export_xlsx_header_internal_notes),
                            context.getString(R.string.export_xlsx_header_diagnosis_outcome),
                            context.getString(R.string.export_xlsx_header_payment),
                            context.getString(R.string.export_xlsx_header_estimate_amount),
                            context.getString(R.string.export_xlsx_header_final_amount),
                            context.getString(R.string.export_xlsx_header_created_at),
                            context.getString(R.string.export_xlsx_header_updated_at),
                            context.getString(R.string.export_xlsx_header_closed_at),
                            context.getString(R.string.export_xlsx_header_closure_reason),
                        ),
                    statusLabels = OrderStatus.entries.associateWith { it.label(context) },
                    priorityLabels = OrderPriority.entries.associateWith { it.label(context) },
                    customerTypeLabels = CustomerType.entries.associateWith { it.label(context) },
                    diagnosisOutcomeLabels =
                        DiagnosisOutcome.entries.associateWith {
                            it.label(
                                context,
                            )
                        },
                    closureReasonLabels = ClosureReason.entries.associateWith { it.label(context) },
                    paidLabel = context.getString(R.string.export_xlsx_value_paid),
                    notPaidLabel = context.getString(R.string.export_xlsx_value_not_paid),
                )

            withContext(Dispatchers.IO) {
                val rowSequence =
                    sequence {
                        var offset = 0
                        val limit = 100
                        while (true) {
                            val batch =
                                kotlinx.coroutines.runBlocking {
                                    exportDao.exportOrdersPaged(limit, offset)
                                }
                            if (batch.isEmpty()) break
                            yieldAll(batch)
                            offset += limit
                        }
                    }
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    OrderExportXlsxWriter(labels).write(rowSequence.asIterable(), output, password)
                } ?: throw IOException(context.getString(R.string.error_destination_unavailable))
            }
            eventWriter.dataExported(actor.id, clockMillis())
        }

    suspend fun exportOrderSummaryPdf(
        actor: AuthenticatedUser,
        detail: OrderDetail,
        uri: Uri,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EXPORT_DATA)
            withContext(Dispatchers.IO) {
                val document = PdfDocument()
                try {
                    val writer = PdfTextWriter(document)
                    if (BuildConfig.IS_DEMO_VERSION) {
                        writer.setWatermark(context.getString(R.string.demo_version_banner))
                    }
                    writer.title(
                        context.getString(
                            R.string.backup_pdf_order_summary,
                            detail.order.orderNumber
                        )
                    )
                    writer.line(
                        context.getString(
                            R.string.backup_pdf_number,
                            detail.order.orderNumber
                        )
                    )
                    writer.line(context.getString(R.string.backup_pdf_title, detail.order.title))
                    writer.line(
                        context.getString(
                            R.string.backup_pdf_status,
                            detail.order.status.label(context)
                        )
                    )
                    detail.order.closureReason?.let { reason ->
                        writer.line(
                            context.getString(
                                R.string.backup_pdf_closure_reason,
                                reason.label(context)
                            )
                        )
                    }
                    writer.line(
                        context.getString(
                            R.string.backup_pdf_customer,
                            detail.customer?.displayName(context) ?: ""
                        )
                    )
                    writer.line(
                        context.getString(
                            R.string.backup_pdf_category,
                            detail.category
                                ?.name
                                .orEmpty()
                                .ifBlank { context.getString(R.string.backup_pdf_none) },
                        ),
                    )
                    writer.line(
                        context.getString(
                            R.string.backup_pdf_operator,
                            detail.assignedUser
                                ?.displayName
                                .orEmpty()
                                .ifBlank { context.getString(R.string.unassigned) },
                        ),
                    )
                    writer.line(
                        context.getString(
                            R.string.backup_pdf_created_at,
                            formatTime(detail.order.createdAt)
                        )
                    )
                    writer.line(
                        context.getString(
                            R.string.backup_pdf_description,
                            detail.order.description
                                .orEmpty()
                                .ifBlank { context.getString(R.string.backup_pdf_none) },
                        ),
                    )
                    writer.section(context.getString(R.string.backup_pdf_timeline))
                    if (detail.timeline.isEmpty()) {
                        writer.line(context.getString(R.string.backup_pdf_no_events))
                    } else {
                        detail.timeline
                            .sortedBy { it.createdAt }
                            .forEach { event ->
                                writer.line(formatTimelineEvent(context, event))
                            }
                    }
                    writer.section(context.getString(R.string.backup_pdf_signatures))
                    if (detail.signatures.isEmpty()) {
                        writer.line(context.getString(R.string.backup_pdf_no_signatures))
                    } else {
                        detail.signatures.forEach { signature ->
                            writer.line("${signature.signerName} - ${formatTime(signature.signedAt)} - ${signature.fileName}")
                        }
                        detail.signatures.firstOrNull()?.let { signature ->
                            writer.section(context.getString(R.string.backup_pdf_signed_conditions))
                            writer.line(
                                context.getString(
                                    R.string.backup_pdf_privacy_version,
                                    signature.privacyTemplateVersion,
                                    signature.privacyTextSnapshot
                                )
                            )
                            writer.line(
                                context.getString(
                                    R.string.backup_pdf_terms_version,
                                    signature.termsTemplateVersion,
                                    signature.termsTextSnapshot
                                )
                            )
                        }
                    }
                    writer.finish()

                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        document.writeTo(output)
                    }
                        ?: throw IOException(context.getString(R.string.error_destination_unavailable))
                } finally {
                    document.close()
                }
            }
            eventWriter.dataExported(actor.id, clockMillis())
        }

    fun databaseFile(): File = dbBackupService.databaseFile()

    fun displayName(uri: Uri): String? =
        runCatching {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    } else {
                        null
                    }
                }?.takeIf { it.isNotBlank() }
        }.getOrNull()

    private fun replacePrivateFiles(restoreRoot: File) {
        val destination = mediaStorageProvider.getBaseMediaDir()
        BackupArchive.restoreAppMedia(File(restoreRoot, "files"), destination)
    }

    private fun writeText(
        uri: Uri,
        text: String,
    ) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw IOException(context.getString(R.string.error_destination_unavailable))
    }

    private companion object {
        const val RESTORE_WORK_DIR = "onlytechtake_restore"
    }
}

private fun ZipOutputStream.putTextEntry(
    name: String,
    text: String,
    password: String?,
) {
    val params =
        net.lingala.zip4j.model
            .ZipParameters()
    params.fileNameInZip = name
    if (password != null) {
        params.isEncryptFiles = true
        params.encryptionMethod = net.lingala.zip4j.model.enums.EncryptionMethod.AES
        params.aesKeyStrength = net.lingala.zip4j.model.enums.AesKeyStrength.KEY_STRENGTH_256
    }
    putNextEntry(params)
    write(text.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun formatTimelineEvent(
    context: Context,
    event: TimelineEventEntity,
): String {
    val parts =
        buildList {
            add("${formatTime(event.createdAt)} - ${event.title}")
            event.description?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (!event.oldStatus.isNullOrBlank() || !event.newStatus.isNullOrBlank()) {
                add(
                    context.getString(
                        R.string.backup_pdf_status_change,
                        event.oldStatus.orEmpty().ifBlank { "-" },
                        event.newStatus.orEmpty().ifBlank { "-" },
                    ),
                )
            }
            event.expectedFeedbackType?.let {
                add(
                    context.getString(
                        R.string.backup_pdf_expected_feedback,
                        it.name
                    )
                )
            }
            event.customerFeedback?.let {
                add(
                    context.getString(
                        R.string.backup_pdf_customer_feedback,
                        it.name
                    )
                )
            }
            event.customerFeedbackNote
                ?.takeIf { it.isNotBlank() }
                ?.let { add(context.getString(R.string.backup_pdf_feedback_note, it)) }
            event.customerFeedbackAt?.let {
                add(
                    context.getString(
                        R.string.backup_pdf_feedback_recorded,
                        formatTime(it)
                    )
                )
            }
        }
    return parts.joinToString(" | ")
}
