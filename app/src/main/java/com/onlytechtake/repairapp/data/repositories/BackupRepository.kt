package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.core.security.DatabaseKeyManager
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderExportDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.storage.StoragePolicy
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.core.permissions.PermissionService
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupRepository(
    private val context: Context,
    private val categoryDao: OrderCategoryDao,
    private val templateDao: MessageTemplateDao,
    private val settingDao: AppSettingDao,
    private val auditLogDao: AuditLogDao,
    private val backupReminderPreferences: com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences? = null,
    private val securityPreferences: SecurityPreferences? = null,
    private val orderExportDao: OrderExportDao? = null,
    private val database: RepairAppDatabase? = null,
    private val authPreferences: AuthPreferences? = null,
    private val sessionStore: InMemorySessionStore? = null,
    private val storageStatsService: com.onlytechtake.repairapp.data.services.StorageStatsService? = null,
    private val mediaStorageProvider: MediaStorageProvider? = null,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(context, auditLogDao = auditLogDao)

    suspend fun exportConfiguration(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EXPORT_DATA)
            writeText(uri, buildConfigurationJson())
            eventWriter.dataExported(actor.id, clockMillis())
        }

    suspend fun exportDatabase(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EXPORT_DATA)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException("Funzione disponibile nella versione Full")
            }
            val dbFile = databaseFile()
            require(dbFile.exists()) { "Database non ancora creato" }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                dbFile.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Destinazione non disponibile")
            eventWriter.dataExported(actor.id, clockMillis())
        }

    suspend fun exportCompleteBackup(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
            PermissionService.requirePermission(actor, UserPermission.BACKUP_RESTORE)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException("Funzione disponibile nella versione Full")
            }

            val securitySettings = securityPreferences?.settings?.first()
            val password = securitySettings?.backupEncryptionPassword?.takeIf { securitySettings.isBackupEncryptionEnabled && it.isNotBlank() }

            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output, password?.toCharArray()).use { zip ->
                    zip.putTextEntry("metadata/backup.json", buildBackupMetadataJson(), password)
                    zip.putTextEntry("config/configuration.json", buildConfigurationJson(), password)
                    addDatabaseFiles(zip, password)
                    BackupArchive.addDirectory(
                        zip = zip,
                        directory = mediaStorageProvider?.getBaseMediaDir()
                            ?: File(context.filesDir, BackupArchive.CANONICAL_MEDIA_DIRECTORY),
                        entryDirectory = "files/${BackupArchive.CANONICAL_MEDIA_DIRECTORY}",
                        password = password,
                    )
                }
            } ?: throw IOException("Destinazione non disponibile")
            
            val fileName = displayName(uri)
            backupReminderPreferences?.recordSuccessfulFullBackup(fileName)
            eventWriter.dataExported(actor.id, clockMillis())
        }.onFailure {
            backupReminderPreferences?.recordFailedFullBackup()
        }

    suspend fun restoreCompleteBackup(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
            PermissionService.requirePermission(actor, UserPermission.BACKUP_RESTORE)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException("Funzione disponibile nella versione Full")
            }
            
            val securitySettings = securityPreferences?.settings?.first()
            val password = securitySettings?.backupEncryptionPassword?.takeIf { it.isNotBlank() }

            withContext(Dispatchers.IO) {
                val stagingDir = File(context.cacheDir, RESTORE_WORK_DIR).also {
                    it.deleteRecursively()
                    check(it.mkdirs()) { "Area ripristino non disponibile" }
                }
                try {
                    val extracted = context.contentResolver.openInputStream(uri)?.use { input ->
                        BackupArchive.extract(input, stagingDir, password?.toCharArray())
                    } ?: throw IOException("Backup non disponibile")
                    require(extracted.metadataText.contains("\"databaseVersion\": ${RepairAppDatabase.DATABASE_VERSION}")) {
                        "Versione backup non compatibile"
                    }
                    val backupDbFile = extracted.databaseFiles[RepairAppDatabase.DATABASE_NAME]
                        ?: throw IOException("Database mancante nel backup")
                    validateDatabase(backupDbFile)
                    
                    // Atomic replacement: close, move current to backup, move new to current, delete backup
                    database?.close()
                    RepairAppDatabase.closeInstance()
                    
                    val currentDbFile = databaseFile()
                    val oldDbBackupDir = File(context.cacheDir, "db_backup_pre_restore").also {
                        it.deleteRecursively()
                        it.mkdirs()
                    }
                    
                    // Snapshot current DB files
                    currentDbFile.parentFile?.listFiles { file -> 
                        (file.name == currentDbFile.name) || file.name.startsWith("${currentDbFile.name}-") 
                    }?.forEach { it.copyTo(File(oldDbBackupDir, it.name)) }

                    try {
                        replaceDatabaseFiles(extracted.databaseFiles)
                        replacePrivateFiles(stagingDir)
                        
                        authPreferences?.clear()
                        sessionStore?.clear()

                        val now = clockMillis()
                        val restoredDatabase = RepairAppDatabase.getInstance(context)
                        try {
                            restoredDatabase.auditLogDao().upsert(
                                com.onlytechtake.repairapp.data.local.entities.AuditLogEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    action = com.onlytechtake.repairapp.data.local.entities.AuditAction.DATA_RESET,
                                    actorUserId = actor.id,
                                    orderId = null,
                                    customerId = null,
                                    detailsJson = """{"operation":"backup_restored","at":"${formatTime(now)}"}""",
                                    createdAt = now,
                                ),
                            )
                        } finally {
                            RepairAppDatabase.closeInstance()
                        }
                    } catch (e: Exception) {
                        // Rollback DB if possible (partial restore of files is harder)
                        oldDbBackupDir.listFiles()?.forEach { 
                            it.copyTo(File(currentDbFile.parentFile, it.name), overwrite = true)
                        }
                        throw e
                    } finally {
                        oldDbBackupDir.deleteRecursively()
                    }
                } finally {
                    stagingDir.deleteRecursively()
                }
            }
        }

    suspend fun exportOrdersXlsx(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EXPORT_DATA)
            if (BuildConfig.IS_DEMO_VERSION) {
                throw IllegalStateException("Funzione disponibile nella versione Full")
            }
            
            val securitySettings = securityPreferences?.settings?.first()
            val password = securitySettings?.backupEncryptionPassword?.takeIf { securitySettings.isBackupEncryptionEnabled && it.isNotBlank() }

            val exportDao = requireNotNull(orderExportDao) { "Export commesse non configurato" }
            withContext(Dispatchers.IO) {
                val rows = exportDao.exportAllOrders()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    OrderExportXlsxWriter().write(rows, output, password)
                } ?: throw IOException("Destinazione non disponibile")
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
            val document = PdfDocument()
            try {
                val writer = PdfTextWriter(document)
                if (BuildConfig.IS_DEMO_VERSION) {
                    writer.setWatermark("VERSIONE DEMO")
                }
                writer.title("Riepilogo commessa ${detail.order.orderNumber}")
                writer.line("Numero: ${detail.order.orderNumber}")
                writer.line("Titolo: ${detail.order.title}")
                writer.line("Stato: ${detail.order.status.label(context)}")
                detail.order.closureReason?.let { reason ->
                    writer.line("Motivo chiusura: ${reason.label(context)}")
                }
                writer.line("Cliente: ${detail.customer?.displayName(context) ?: ""}")
                writer.line("Categoria: ${detail.category?.name.orEmpty().ifBlank { "Nessuna" }}")
                writer.line("Operatore: ${detail.assignedUser?.displayName.orEmpty().ifBlank { "Non assegnato" }}")
                writer.line("Data creazione: ${formatTime(detail.order.createdAt)}")
                writer.line("Descrizione: ${detail.order.description.orEmpty().ifBlank { "Nessuna" }}")
                writer.section("Timeline")
                if (detail.timeline.isEmpty()) {
                    writer.line("Nessun evento registrato.")
                } else {
                    detail.timeline
                        .sortedBy { it.createdAt }
                        .forEach { event ->
                            writer.line(formatTimelineEvent(event))
                        }
                }
                writer.section("Firme")
                if (detail.signatures.isEmpty()) {
                    writer.line("Nessuna firma acquisita.")
                } else {
                    detail.signatures.forEach { signature ->
                        writer.line("${signature.signerName} - ${formatTime(signature.signedAt)} - ${signature.fileName}")
                    }
                    detail.signatures.firstOrNull()?.let { signature ->
                        writer.section("Condizioni firmate")
                        writer.line("Privacy v${signature.privacyTemplateVersion}: ${signature.privacyTextSnapshot}")
                        writer.line("Condizioni v${signature.termsTemplateVersion}: ${signature.termsTextSnapshot}")
                    }
                }
                writer.finish()

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    document.writeTo(output)
                } ?: throw IOException("Destinazione non disponibile")
            } finally {
                document.close()
            }
            eventWriter.dataExported(actor.id, clockMillis())
        }

    fun databaseFile(): File = context.getDatabasePath(RepairAppDatabase.DATABASE_NAME)

    fun displayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    } else {
                        null
                    }
                }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

    fun isCloudProviderAvailable(): Boolean {
        val cloudPackages = listOf(
            "com.google.android.apps.docs", // Drive
            "com.microsoft.skydrive",      // OneDrive
            "com.dropbox.android",         // Dropbox
            "com.box.android",             // Box
        )
        val pm = context.packageManager
        return cloudPackages.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun validateDatabase(file: File) {
        // Validation skipped for encrypted database during migration.
        // The restore process will fail later if the key is wrong when Room tries to open it.
        if (!file.exists()) throw IOException("File database non trovato")
    }

    private fun replaceDatabaseFiles(databaseFiles: Map<String, File>) {
        val dbFile = databaseFile()
        dbFile.parentFile?.mkdirs()
        dbFile.parentFile
            ?.listFiles { file -> (file.name == dbFile.name) || file.name.startsWith("${dbFile.name}-") }
            ?.forEach { it.delete() }
        databaseFiles.forEach { (fileName, source) ->
            source.copyTo(File(dbFile.parentFile, fileName), overwrite = true)
        }
    }

    private fun replacePrivateFiles(restoreRoot: File) {
        val destination = mediaStorageProvider?.getBaseMediaDir()
            ?: File(context.filesDir, BackupArchive.CANONICAL_MEDIA_DIRECTORY)
        BackupArchive.restoreAppMedia(File(restoreRoot, "files"), destination)

        listOf("orders", "order_media", "intake_media", "signatures").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
    }

    private suspend fun buildConfigurationJson(): String {
        val categories = categoryDao.observeAll().first()
        val templates = templateDao.observeAll().first()
        val settings = settingDao.observeAll().first()
        return buildString {
            appendLine("{")
            appendLine("  \"exportedAt\": ${jsonString(formatTime(clockMillis()))},")
            appendLine("  \"categories\": [")
            appendLine(
                categories.joinToString(",\n") { category ->
                    "    {\"id\": ${jsonString(category.id)}, \"parentId\": ${jsonString(category.parentId)}, \"level\": ${category.level}, \"name\": ${jsonString(category.name)}, \"active\": ${category.active}}"
                },
            )
            appendLine("  ],")
            appendLine("  \"templates\": [")
            appendLine(
                templates.joinToString(",\n") { template ->
                    "    {\"code\": ${jsonString(template.code)}, \"title\": ${jsonString(template.title)}, \"channel\": ${jsonString(template.channel.name)}, \"active\": ${template.active}, \"version\": ${template.version}, \"body\": ${jsonString(template.body)}}"
                },
            )
            appendLine("  ],")
            appendLine("  \"settings\": [")
            appendLine(
                settings.joinToString(",\n") { setting ->
                    "    {\"key\": ${jsonString(setting.key)}, \"value\": ${jsonString(setting.value)}, \"updatedAt\": ${setting.updatedAt}}"
                },
            )
            appendLine("  ]")
            appendLine("}")
        }
    }

    private fun buildBackupMetadataJson(): String =
        buildString {
            appendLine("{")
            appendLine("  \"createdAt\": ${jsonString(formatTime(clockMillis()))},")
            appendLine("  \"databaseName\": ${jsonString(RepairAppDatabase.DATABASE_NAME)},")
            appendLine("  \"databaseVersion\": ${RepairAppDatabase.DATABASE_VERSION},")
            appendLine("  \"includes\": [\"database\", \"configuration\", \"app_media\"]")
            appendLine("}")
        }

    private fun addDatabaseFiles(zip: ZipOutputStream, password: String?) {
        val dbFile = databaseFile()
        require(dbFile.exists()) { "Database non ancora creato" }
        val databaseFiles = dbFile.parentFile
            ?.listFiles { file -> (file.name == dbFile.name) || file.name.startsWith("${dbFile.name}-") }
            ?.sortedBy { it.name }
            .orEmpty()
        databaseFiles.forEach { file ->
            zip.putFileEntry(file, "database/${file.name}", password)
        }
    }

    private fun writeText(uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Destinazione non disponibile")
    }

    private companion object {
        const val RESTORE_WORK_DIR = "onlytechtake_restore"
    }

}

private fun ZipOutputStream.putTextEntry(name: String, text: String, password: String?) {
    val params = ZipParameters()
    params.fileNameInZip = name
    if (password != null) {
        params.isEncryptFiles = true
        params.encryptionMethod = EncryptionMethod.AES
        params.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
    }
    putNextEntry(params)
    write(text.toByteArray(Charsets.UTF_8))
    closeEntry()
}


private fun formatTimelineEvent(event: TimelineEventEntity): String {
    val parts = buildList {
        add("${formatTime(event.createdAt)} - ${event.title}")
        event.description?.takeIf { it.isNotBlank() }?.let { add(it) }
        if (!event.oldStatus.isNullOrBlank() || !event.newStatus.isNullOrBlank()) {
            add("Cambio stato: ${event.oldStatus.orEmpty().ifBlank { "-" }} -> ${event.newStatus.orEmpty().ifBlank { "-" }}")
        }
        event.expectedFeedbackType?.let { add("Feedback atteso: ${it.name}") }
        event.customerFeedback?.let { add("Feedback cliente: ${it.name}") }
        event.customerFeedbackNote?.takeIf { it.isNotBlank() }?.let { add("Nota feedback: $it") }
        event.customerFeedbackAt?.let { add("Feedback registrato: ${formatTime(it)}") }
    }
    return parts.joinToString(" | ")
}

private fun formatTime(millis: Long): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()),
    )

private fun jsonString(value: String?): String =
    value
        ?.replace("\\", "\\\\")
        ?.replace("\"", "\\\"")
        ?.replace("\r", "\\r")
        ?.replace("\n", "\\n")
        ?.let { "\"$it\"" }
        ?: "null"
