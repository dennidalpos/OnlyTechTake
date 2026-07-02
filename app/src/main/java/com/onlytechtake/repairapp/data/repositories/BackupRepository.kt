package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.permissions.PermissionService
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
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.storage.StoragePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import java.io.File
import java.io.IOException

class BackupRepository(
    private val context: Context,
    categoryDao: OrderCategoryDao,
    templateDao: MessageTemplateDao,
    settingDao: AppSettingDao,
    private val auditLogDao: AuditLogDao,
    private val backupReminderPreferences: com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences,
    private val securityPreferences: SecurityPreferences,
    private val orderExportDao: OrderExportDao,
    private val database: RepairAppDatabase,
    private val authPreferences: AuthPreferences,
    private val sessionStore: InMemorySessionStore,
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
                        BackupArchive.addDirectory(
                            zip = zip,
                            directory = File(context.filesDir, "datastore"),
                            entryDirectory = "files/datastore",
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
        password: String? = null,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.BACKUP_RESTORE)
            performRestore(actor.id, uri, password).getOrThrow()
        }

    suspend fun restoreInitialBackup(
        uri: Uri,
        password: String? = null,
    ): Result<Unit> =
        runCatching {
            // Only allow if not configured
            val adminCount = database.userDao().findAllAdmins().size
            if (adminCount > 0) {
                throw SecurityException(context.getString(R.string.error_feature_pro_only))
            }
            performRestore(null, uri, password).getOrThrow()
        }

    private suspend fun performRestore(
        actorId: String?,
        uri: Uri,
        password: String? = null,
    ): Result<Unit> =
        runCatching {
            StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException(context.getString(R.string.error_feature_pro_only))
            }

            val securitySettings = securityPreferences.settings.first()
            val finalPassword =
                password ?: securitySettings.backupEncryptionPassword.takeIf { it.isNotBlank() }

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
                                finalPassword?.toCharArray(),
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
                            val finalActorId =
                                actorId?.takeIf {
                                    restoredDatabase.userDao().findById(it) != null
                                }

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
                    sheetName = context.getString(R.string.dashboard_orders),
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
                val allOrders = exportDao.exportAllOrders()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    OrderExportXlsxWriter(labels).write(allOrders, output, password)
                } ?: throw IOException(context.getString(R.string.error_destination_unavailable))
            }
            eventWriter.dataExported(actor.id, clockMillis())
        }

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
        BackupArchive.restoreAppFiles(File(restoreRoot, "files"), context.filesDir)
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
