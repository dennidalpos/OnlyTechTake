package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first

class BackupRepository(
    private val context: Context,
    private val categoryDao: OrderCategoryDao,
    private val templateDao: MessageTemplateDao,
    private val settingDao: AppSettingDao,
    private val auditLogDao: AuditLogDao,
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
                writer.line("Stato: ${detail.order.status.name}")
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
