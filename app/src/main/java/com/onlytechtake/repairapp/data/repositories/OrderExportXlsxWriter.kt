package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.utils.formatEuroCentsForExport
import com.onlytechtake.repairapp.data.local.dao.OrderExportRow
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class OrderExportXlsxWriter(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun write(rows: List<OrderExportRow>, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            zip.putTextEntry("[Content_Types].xml", contentTypesXml)
            zip.putTextEntry("_rels/.rels", rootRelationshipsXml)
            zip.putTextEntry("xl/workbook.xml", workbookXml)
            zip.putTextEntry("xl/_rels/workbook.xml.rels", workbookRelationshipsXml)
            zip.putTextEntry("xl/styles.xml", stylesXml)
            zip.putTextEntry("xl/worksheets/sheet1.xml", worksheetXml(rows))
        }
    }

    private fun worksheetXml(rows: List<OrderExportRow>): String {
        val allRows = listOf(headers) + rows.map(::rowValues)
        val lastColumn = columnName(headers.size)
        val lastRow = allRows.size.coerceAtLeast(1)
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<dimension ref="A1:$lastColumn$lastRow"/>""")
            append("<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>")
            append("<sheetFormatPr defaultRowHeight=\"15\"/>")
            append("<sheetData>")
            allRows.forEachIndexed { rowIndex, values ->
                val rowNumber = rowIndex + 1
                append("""<row r="$rowNumber">""")
                values.forEachIndexed { columnIndex, value ->
                    appendCell(columnName(columnIndex + 1), rowNumber, value)
                }
                append("</row>")
            }
            append("</sheetData>")
            append("</worksheet>")
        }
    }

    private fun StringBuilder.appendCell(column: String, rowNumber: Int, value: String) {
        append("""<c r="$column$rowNumber" t="inlineStr"><is><t>""")
        append(value.xmlEscaped())
        append("</t></is></c>")
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
            row.estimateAmountCents.formatEuroCentsForExport(),
            row.finalAmountCents.formatEuroCentsForExport(),
            row.createdAt.formatTime(),
            row.updatedAt.formatTime(),
            row.closedAt.formatTime(),
            row.closureReason.label(),
            row.deletedAt.formatTime(),
            row.deleteReason.orEmpty(),
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

private fun ZipOutputStream.putTextEntry(name: String, text: String) {
    putNextEntry(ZipEntry(name))
    write(text.trimIndent().toByteArray(Charsets.UTF_8))
    closeEntry()
}

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
    "Importo preventivo",
    "Importo finale",
    "Creata",
    "Aggiornata",
    "Chiusa",
    "Motivo chiusura",
    "Archiviata",
    "Motivo archiviazione",
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
