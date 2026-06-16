package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.utils.formatEuroCentsForExport
import com.onlytechtake.repairapp.data.local.dao.OrderExportRow
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.OutputStream
import java.io.Writer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal class OrderExportXlsxWriter(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    /**
     * Scrive l'export in formato XLSX in modo streaming per gestire dataset di grandi dimensioni.
     */
    fun write(rows: List<OrderExportRow>, output: OutputStream, password: String? = null) {
        ZipOutputStream(output, password?.toCharArray()).use { zip ->
            zip.putTextEntry("[Content_Types].xml", contentTypesXml, password)
            zip.putTextEntry("_rels/.rels", rootRelationshipsXml, password)
            zip.putTextEntry("xl/workbook.xml", workbookXml, password)
            zip.putTextEntry("xl/_rels/workbook.xml.rels", workbookRelationshipsXml, password)
            zip.putTextEntry("xl/styles.xml", stylesXml, password)
            
            val sheetParams = createZipParameters("xl/worksheets/sheet1.xml", password)
            zip.putNextEntry(sheetParams)
            writeWorksheet(rows, zip)
            zip.closeEntry()
        }
    }

    private fun writeWorksheet(rows: List<OrderExportRow>, zip: ZipOutputStream) {
        val writer = zip.writer()
        writer.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        writer.write("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        // Dimension omitted as it's optional for most readers and hard to know in advance when streaming
        writer.write("<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>")
        writer.write("<sheetFormatPr defaultRowHeight=\"15\"/>")
        writer.write("<sheetData>")
        
        // Intestazioni (Riga 1)
        writeRow(writer, 1, headers)
        
        // Dati (Righe 2+)
        rows.forEachIndexed { index, row ->
            writeRow(writer, index + 2, rowValues(row))
        }
        
        writer.write("</sheetData>")
        writer.write("</worksheet>")
        writer.flush()
    }

    private fun writeRow(writer: Writer, rowNumber: Int, values: List<String>) {
        writer.write("""<row r="$rowNumber">""")
        values.forEachIndexed { columnIndex, value ->
            val colName = columnName(columnIndex + 1)
            writer.write("""<c r="$colName$rowNumber" t="inlineStr"><is><t>""")
            writer.write(value.xmlEscaped())
            writer.write("</t></is></c>")
        }
        writer.write("</row>")
    }

    private fun rowValues(row: OrderExportRow): List<String> =
        listOf(
            row.orderNumber,
            row.title,
            row.status.label(),
            row.priority.label(),
            row.customerName.orEmpty(),
            row.customerType.label(),
            row.phone.orEmpty(),
            row.email.orEmpty(),
            row.categoryName.orEmpty(),
            row.assignedUserName.orEmpty(),
            row.description.orEmpty(),
            row.internalNotes.orEmpty(),
            row.diagnosisOutcome.label(),
            row.isPaid.paymentLabel(),
            row.estimateAmountCents.formatEuroCentsForExport(),
            row.finalAmountCents.formatEuroCentsForExport(),
            row.createdAt.formatTime(),
            row.updatedAt.formatTime(),
            row.closedAt.formatTime(),
            row.closureReason.label(),
        )

    private fun Long?.formatTime(): String =
        this?.let {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(it).atZone(zoneId))
        }.orEmpty()

    private fun CustomerType?.label(): String =
        when (this) {
            CustomerType.PRIVATE -> "Privato"
            CustomerType.BUSINESS -> "Azienda"
            null -> ""
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

    private fun OrderPriority.label(): String =
        when (this) {
            OrderPriority.LOW -> "Bassa"
            OrderPriority.NORMAL -> "Normale"
            OrderPriority.HIGH -> "Alta"
            OrderPriority.URGENT -> "Urgente"
        }

    private fun DiagnosisOutcome?.label(): String =
        when (this) {
            DiagnosisOutcome.REPAIRABLE -> "Riparabile"
            DiagnosisOutcome.NOT_REPAIRABLE -> "Non riparabile"
            null -> ""
        }

    private fun Boolean.paymentLabel(): String =
        if (this) "Pagato" else "Non pagato"

    private fun ClosureReason?.label(): String =
        when (this) {
            ClosureReason.COMPLETED -> "Completata"
            ClosureReason.CUSTOMER_DECLINED -> "Rifiutata dal cliente"
            ClosureReason.NOT_REPAIRABLE -> "Non riparabile"
            ClosureReason.CANCELLED -> "Annullata"
            ClosureReason.OTHER -> "Altro"
            null -> ""
        }

    private fun columnName(index: Int): String {
        var remaining = index
        val name = StringBuilder()
        while (remaining > 0) {
            remaining--
            name.insert(0, 'A' + (remaining % 26))
            remaining /= 26
        }
        return name.toString()
    }

    private fun String.xmlEscaped(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}

private fun ZipOutputStream.putTextEntry(name: String, text: String, password: String?) {
    val params = createZipParameters(name, password)
    putNextEntry(params)
    write(text.trimIndent().toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun createZipParameters(fileName: String, password: String?): ZipParameters {
    val params = ZipParameters()
    params.fileNameInZip = fileName
    if (password != null) {
        params.isEncryptFiles = true
        params.encryptionMethod = EncryptionMethod.AES
        params.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
    }
    return params
}

private fun ZipOutputStream.writer(): Writer =
    java.io.OutputStreamWriter(this, Charsets.UTF_8)

private val headers = listOf(
    "Numero commessa",
    "Titolo",
    "Stato",
    "Priorita",
    "Cliente",
    "Tipo cliente",
    "Telefono",
    "Email",
    "Categoria",
    "Operatore",
    "Descrizione",
    "Note interne",
    "Esito diagnosi",
    "Pagamento",
    "Importo preventivo",
    "Importo finale",
    "Creata",
    "Aggiornata",
    "Chiusa",
    "Motivo chiusura",
)

private const val contentTypesXml = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>
"""

private const val rootRelationshipsXml = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>
"""

private const val workbookXml = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Commesse" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>
"""

private const val workbookRelationshipsXml = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
"""

private const val stylesXml = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>
"""
