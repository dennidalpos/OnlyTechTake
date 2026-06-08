package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderExportDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
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
import java.util.UUID
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
    private val eventWriter = OrderWorkflowEventWriter(auditLogDao = auditLogDao)

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

    suspend fun importCompleteBackup(actor: AuthenticatedUser, uri: Uri): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            val roomDatabase = requireNotNull(database) { "Ripristino non configurato" }

            withContext(Dispatchers.IO) {
                val restoreDirectory = File(context.cacheDir, "backup_restore_${UUID.randomUUID()}").apply {
                    mkdirs()
                }
                val rollbackDirectory = File(context.cacheDir, "backup_restore_rollback_${UUID.randomUUID()}")
                try {
                    val extracted = extractBackup(uri, restoreDirectory)
                    restorePrivateFiles(extracted.filesDirectory, rollbackDirectory)
                    try {
                        restoreDatabase(roomDatabase, extracted.databaseFile)
                    } catch (error: Throwable) {
                        restorePrivateFiles(rollbackDirectory, File(context.cacheDir, "backup_restore_unused_${UUID.randomUUID()}"))
                        throw error
                    }
                } finally {
                    restoreDirectory.deleteRecursively()
                    rollbackDirectory.deleteRecursively()
                }
            }

            eventWriter.dataImported(clockMillis())
            authPreferences?.markConfigured()
            sessionStore?.clear()
        }

    suspend fun exportOrderSummaryPdf(
        actor: AuthenticatedUser,
        detail: OrderDetail,
        uri: Uri,
    ): Result<Unit> =
        runCatching {
            require(detail.order.deletedAt == null || actor.role == UserRole.ADMIN) { "Commessa archiviata non esportabile" }
            val document = PdfDocument()
            try {
                val writer = PdfTextWriter(document)
                writer.title("Riepilogo commessa ${detail.order.orderNumber}")
                writer.line("Numero: ${detail.order.orderNumber}")
                writer.line("Titolo: ${detail.order.title}")
                writer.line("Stato: ${detail.order.status.label()}")
                detail.order.closureReason?.let { reason ->
                    writer.line("Motivo chiusura: ${reason.label()}")
                }
                writer.line("Cliente: ${detail.customer.displayName()}")
                writer.line("Categoria: ${detail.category?.name.orEmpty().ifBlank { "Nessuna" }}")
                writer.line("Operatore: ${detail.assignedUser?.displayName.orEmpty().ifBlank { "Non assegnato" }}")
                writer.line("Data creazione: ${formatTime(detail.order.createdAt)}")
                writer.line("Descrizione: ${detail.order.description.orEmpty().ifBlank { "Nessuna" }}")
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

    private fun extractBackup(uri: Uri, restoreDirectory: File): ExtractedBackup {
        val databaseDirectory = File(restoreDirectory, "database").apply { mkdirs() }
        val filesDirectory = File(restoreDirectory, "files").apply { mkdirs() }
        val databaseFile = File(databaseDirectory, RepairAppDatabase.DATABASE_NAME)
        var hasMetadata = false

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val entryName = entry.name
                    validateZipEntryName(entryName)
                    when {
                        entry.isDirectory -> Unit
                        entryName == "metadata/backup.json" -> {
                            hasMetadata = true
                        }
                        entryName.startsWith("database/") -> {
                            val fileName = entryName.removePrefix("database/")
                            require(fileName == RepairAppDatabase.DATABASE_NAME || fileName.startsWith("${RepairAppDatabase.DATABASE_NAME}-")) {
                                "Backup database non valido"
                            }
                            File(databaseDirectory, fileName).outputStream().use { output -> zip.copyTo(output) }
                        }
                        entryName.startsWith("files/orders/") || entryName.startsWith("files/order_media/") -> {
                            val target = File(filesDirectory, entryName.removePrefix("files/"))
                            target.parentFile?.mkdirs()
                            target.outputStream().use { output -> zip.copyTo(output) }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: throw IOException("Backup non disponibile")

        require(hasMetadata) { "File backup non valido" }
        require(databaseFile.exists()) { "Database backup mancante" }
        return ExtractedBackup(databaseFile = databaseFile, filesDirectory = filesDirectory)
    }

    private fun restoreDatabase(roomDatabase: RepairAppDatabase, backupDatabaseFile: File) {
        val db = roomDatabase.openHelper.writableDatabase
        db.execSQL("ATTACH DATABASE ? AS backup", arrayOf(backupDatabaseFile.absolutePath))
        try {
            val backupVersion = db.query("PRAGMA backup.user_version").use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            require(backupVersion == RepairAppDatabase.DATABASE_VERSION) {
                "Versione backup non supportata"
            }
            DATABASE_TABLES.forEach { table ->
                require(db.query("SELECT name FROM backup.sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use { it.moveToFirst() }) {
                    "Tabella backup mancante: $table"
                }
            }
            val activeAdminCount = db.query("SELECT COUNT(*) FROM backup.users WHERE role = 'ADMIN' AND active = 1").use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            require(activeAdminCount > 0) { "Il backup non contiene admin attivi" }

            db.execSQL("PRAGMA foreign_keys=OFF")
            db.beginTransaction()
            try {
                DELETE_TABLES.forEach { table -> db.execSQL("DELETE FROM $table") }
                INSERT_TABLES.forEach { table -> db.execSQL("INSERT INTO $table SELECT * FROM backup.$table") }
                val foreignKeyErrors = db.query("PRAGMA foreign_key_check").use { cursor -> cursor.count }
                require(foreignKeyErrors == 0) { "Backup con riferimenti non validi" }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        } finally {
            db.execSQL("DETACH DATABASE backup")
        }
    }

    private fun restorePrivateFiles(filesDirectory: File, rollbackDirectory: File) {
        rollbackDirectory.deleteRecursively()
        rollbackDirectory.mkdirs()
        RESTORED_FILE_DIRECTORIES.forEach { directoryName ->
            val current = File(context.filesDir, directoryName)
            val rollback = File(rollbackDirectory, directoryName)
            if (current.exists()) {
                current.copyRecursively(rollback, overwrite = true)
            }
            current.deleteRecursively()
            val restored = File(filesDirectory, directoryName)
            if (restored.exists()) {
                restored.copyRecursively(current, overwrite = true)
            }
        }
        File(context.filesDir, "intake_media").deleteRecursively()
    }

    private fun validateZipEntryName(name: String) {
        val segments = name.split("/")
        require(name.isNotBlank() && !name.startsWith("/") && "\\" !in name && ".." !in segments) {
            "Backup ZIP non valido"
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
            appendLine(categories.joinToString(",\n") { category ->
                "    {\"id\": ${jsonString(category.id)}, \"name\": ${jsonString(category.name)}, \"active\": ${category.active}, \"sortOrder\": ${category.sortOrder}}"
            })
            appendLine("  ],")
            appendLine("  \"templates\": [")
            appendLine(templates.joinToString(",\n") { template ->
                "    {\"code\": ${jsonString(template.code)}, \"title\": ${jsonString(template.title)}, \"channel\": ${jsonString(template.channel.name)}, \"active\": ${template.active}, \"version\": ${template.version}, \"body\": ${jsonString(template.body)}}"
            })
            appendLine("  ],")
            appendLine("  \"settings\": [")
            appendLine(settings.joinToString(",\n") { setting ->
                "    {\"key\": ${jsonString(setting.key)}, \"value\": ${jsonString(setting.value)}, \"updatedAt\": ${setting.updatedAt}}"
            })
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
            ?.listFiles { file -> file.name == dbFile.name || file.name.startsWith("${dbFile.name}-") }
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

}

private data class ExtractedBackup(
    val databaseFile: File,
    val filesDirectory: File,
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

private fun CustomerEntity?.displayName(): String =
    when (val customer = this) {
        null -> "Cliente non disponibile"
        else -> when (customer.type) {
            CustomerType.PRIVATE -> listOfNotNull(customer.firstName, customer.lastName)
                .joinToString(" ")
                .ifBlank { "Privato senza nome" }
            CustomerType.BUSINESS -> customer.companyName.orEmpty().ifBlank { "Azienda senza nome" }
        }
    }

private fun OrderStatus.label(): String =
    when (this) {
        OrderStatus.OPEN -> "Aperta"
        OrderStatus.IN_PROGRESS -> "In lavorazione"
        OrderStatus.DIAGNOSIS -> "In diagnosi"
        OrderStatus.RESULT -> "Risultato"
        OrderStatus.REPAIRING -> "In riparazione"
        OrderStatus.READY_FOR_PICKUP -> "Pronta al ritiro"
        OrderStatus.CLOSED -> "Chiusa"
    }

private fun ClosureReason.label(): String =
    when (this) {
        ClosureReason.COMPLETED -> "Completata"
        ClosureReason.CUSTOMER_DECLINED -> "Rifiutata dal cliente"
        ClosureReason.NOT_REPAIRABLE -> "Non riparabile"
        ClosureReason.CANCELLED -> "Annullata"
        ClosureReason.OTHER -> "Altro"
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

private val DATABASE_TABLES = listOf(
    "users",
    "customers",
    "order_categories",
    "repair_orders",
    "timeline_events",
    "message_templates",
    "message_logs",
    "app_settings",
    "audit_logs",
    "order_media",
    "signatures",
)

private val DELETE_TABLES = listOf(
    "message_logs",
    "order_media",
    "signatures",
    "timeline_events",
    "audit_logs",
    "repair_orders",
    "customers",
    "order_categories",
    "message_templates",
    "app_settings",
    "users",
)

private val INSERT_TABLES = listOf(
    "users",
    "customers",
    "order_categories",
    "message_templates",
    "app_settings",
    "repair_orders",
    "timeline_events",
    "message_logs",
    "audit_logs",
    "order_media",
    "signatures",
)

private val RESTORED_FILE_DIRECTORIES = listOf("orders", "order_media")
