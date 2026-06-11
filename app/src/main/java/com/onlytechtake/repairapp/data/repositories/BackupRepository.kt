package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderExportDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupRepository(
    private val context: Context,
    private val categoryDao: OrderCategoryDao,
    private val templateDao: MessageTemplateDao,
    private val settingDao: AppSettingDao,
    private val auditLogDao: AuditLogDao,
    private val orderExportDao: OrderExportDao? = null,
    private val database: RepairAppDatabase? = null,
    private val authPreferences: AuthPreferences? = null,
    private val sessionStore: InMemorySessionStore? = null,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(context, auditLogDao = auditLogDao)

    suspend fun exportConfiguration(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            writeText(uri, buildConfigurationJson())
            eventWriter.dataExported(actor.id, clockMillis())
        }

    suspend fun exportDatabase(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            val dbFile = databaseFile()
            require(dbFile.exists()) { "Database non ancora creato" }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                dbFile.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Destinazione non disponibile")
            eventWriter.dataExported(actor.id, clockMillis())
        }

    suspend fun exportCompleteBackup(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output).use { zip ->
                    zip.putTextEntry("metadata/backup.json", buildBackupMetadataJson())
                    zip.putTextEntry("config/configuration.json", buildConfigurationJson())
                    addDatabaseFiles(zip)
                    addAppFiles(zip, directoryName = "orders")
                    addAppFiles(zip, directoryName = "order_media")
                }
            } ?: throw IOException("Destinazione non disponibile")
            eventWriter.dataExported(actor.id, clockMillis())
        }

    suspend fun restoreCompleteBackup(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            withContext(Dispatchers.IO) {
                val restoreRoot = File(context.cacheDir, RESTORE_WORK_DIR).also {
                    it.deleteRecursively()
                    check(it.mkdirs()) { "Area ripristino non disponibile" }
                }
                try {
                    val extracted = extractBackup(uri, restoreRoot)
                    require(extracted.metadataText.contains("\"databaseVersion\": ${RepairAppDatabase.DATABASE_VERSION}")) {
                        "Versione backup non compatibile"
                    }
                    validateDatabase(extracted.databaseFiles.getValue(RepairAppDatabase.DATABASE_NAME))
                    database?.close()
                    RepairAppDatabase.closeInstance()
                    replaceDatabaseFiles(extracted.databaseFiles)
                    replacePrivateFiles(restoreRoot)
                    authPreferences?.clear()
                    sessionStore?.clear()
                } finally {
                    restoreRoot.deleteRecursively()
                }
            }
        }

    suspend fun exportOrdersXlsx(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            val exportDao = requireNotNull(orderExportDao) { "Export commesse non configurato" }
            withContext(Dispatchers.IO) {
                val rows = exportDao.exportAllOrders()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    OrderExportXlsxWriter().write(rows, output)
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
            require((detail.order.deletedAt == null) || (actor.role == UserRole.ADMIN)) { "Commessa archiviata non esportabile" }
            val document = PdfDocument()
            try {
                val writer = PdfTextWriter(document)
                writer.title("Riepilogo commessa ${detail.order.orderNumber}")
                writer.line("Numero: ${detail.order.orderNumber}")
                writer.line("Titolo: ${detail.order.title}")
                writer.line("Stato: ${detail.order.status.label(context)}")
                detail.order.closureReason?.let { reason ->
                    writer.line("Motivo chiusura: ${reason.label(context)}")
                }
                writer.line("Cliente: ${detail.customer.displayName(context)}")
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

    private fun extractBackup(uri: Uri, restoreRoot: File): ExtractedBackup {
        val metadata = StringBuilder()
        val databaseFiles = mutableMapOf<String, File>()
        var hasMetadata = false

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    try {
                        if (entry.isDirectory) continue
                        requireSafeZipEntry(entry.name)
                        when {
                            entry.name == BACKUP_METADATA_ENTRY -> {
                                hasMetadata = true
                                metadata.append(zip.readBytes().toString(Charsets.UTF_8))
                            }
                            entry.name.startsWith(DATABASE_ENTRY_PREFIX) -> {
                                val fileName = entry.name.removePrefix(DATABASE_ENTRY_PREFIX)
                                require(fileName.isNotBlank() && !fileName.contains('/')) { "Backup database non valido" }
                                val destination = File(restoreRoot, "database/$fileName")
                                destination.parentFile?.mkdirs()
                                destination.outputStream().use { output -> zip.copyTo(output) }
                                databaseFiles[fileName] = destination
                            }
                            entry.name.startsWith(FILES_ENTRY_PREFIX) -> {
                                val relativePath = entry.name.removePrefix(FILES_ENTRY_PREFIX)
                                require(relativePath.startsWith("orders/") || relativePath.startsWith("order_media/")) {
                                    "Backup contiene file app non supportati"
                                }
                                val destination = safeChild(File(restoreRoot, "files"), relativePath)
                                destination.parentFile?.mkdirs()
                                destination.outputStream().use { output -> zip.copyTo(output) }
                            }
                        }
                    } finally {
                        zip.closeEntry()
                    }
                }
            }
        } ?: throw IOException("Backup non disponibile")

        require(hasMetadata) { "Metadata backup mancanti" }
        require(databaseFiles.containsKey(RepairAppDatabase.DATABASE_NAME)) { "Database backup mancante" }
        return ExtractedBackup(metadata.toString(), databaseFiles)
    }

    private fun requireSafeZipEntry(name: String) {
        require(name.isNotBlank()) { "Backup ZIP non valido" }
        require(!name.startsWith("/") && !name.startsWith("\\")) { "Percorso backup non valido" }
        val parts = name.replace('\\', '/').split('/')
        require(parts.none { (it == "..") || it.isBlank() }) { "Percorso backup non valido" }
    }

    private fun safeChild(root: File, relativePath: String): File {
        val destination = File(root, relativePath)
        val rootPath = root.canonicalPath + File.separator
        val destinationPath = destination.canonicalPath
        require(destinationPath.startsWith(rootPath)) { "Percorso backup non valido" }
        return destination
    }

    private fun validateDatabase(file: File) {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'users'", null).use { cursor ->
                require(cursor.moveToFirst()) { "Database backup non valido" }
            }
        }
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
        listOf("orders", "order_media", "intake_media", "signatures").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
        val restoredFiles = File(restoreRoot, "files")
        listOf("orders", "order_media").forEach { name ->
            val source = File(restoredFiles, name)
            if (source.exists()) {
                source.copyRecursively(File(context.filesDir, name), overwrite = true)
            }
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
            appendLine("  \"includes\": [\"database\", \"configuration\", \"order_media\", \"signatures\"]")
            appendLine("}")
        }

    private fun addDatabaseFiles(zip: ZipOutputStream) {
        val dbFile = databaseFile()
        require(dbFile.exists()) { "Database non ancora creato" }
        val databaseFiles = dbFile.parentFile
            ?.listFiles { file -> (file.name == dbFile.name) || file.name.startsWith("${dbFile.name}-") }
            ?.sortedBy { it.name }
            .orEmpty()
        databaseFiles.forEach { file ->
            zip.putFileEntry(file, "database/${file.name}")
        }
    }

    private fun addAppFiles(zip: ZipOutputStream, directoryName: String) {
        val directory = File(context.filesDir, directoryName)
        if (!directory.exists()) return
        directory.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.relativeTo(directory).invariantSeparatorsPath
                zip.putFileEntry(file, "files/$directoryName/$relativePath")
            }
    }

    private fun writeText(uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Destinazione non disponibile")
    }

    private companion object {
        const val RESTORE_WORK_DIR = "onlytechtake_restore"
        const val BACKUP_METADATA_ENTRY = "metadata/backup.json"
        const val DATABASE_ENTRY_PREFIX = "database/"
        const val FILES_ENTRY_PREFIX = "files/"
    }

}

private data class ExtractedBackup(
    val metadataText: String,
    val databaseFiles: Map<String, File>,
)

private fun ZipOutputStream.putTextEntry(name: String, text: String) {
    putNextEntry(ZipEntry(name))
    write(text.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun ZipOutputStream.putFileEntry(file: File, entryName: String) {
    putNextEntry(ZipEntry(entryName))
    file.inputStream().use { input -> input.copyTo(this) }
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
