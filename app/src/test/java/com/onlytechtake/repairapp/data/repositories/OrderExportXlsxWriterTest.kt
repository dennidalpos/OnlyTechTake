package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.OrderExportRow
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderExportXlsxWriterTest {
    @Test
    fun writesValidXlsxEntriesAndEscapesCellText() {
        val output = ByteArrayOutputStream()
        val row = OrderExportRow(
            orderNumber = "2026-0001",
            title = "Schermo & vetro <rotto>",
            status = OrderStatus.OPEN,
            priority = OrderPriority.HIGH,
            customerName = "Mario Rossi",
            customerType = CustomerType.PRIVATE,
            phone = "3330000000",
            email = "mario@example.test",
            categoryName = "Smartphone",
            assignedUserName = "Operatore",
            description = "Display \"nero\"",
            internalNotes = "Nota con apostrofo '",
            estimateAmountCents = 12345,
            finalAmountCents = null,
            createdAt = 1_717_171_717_000L,
            updatedAt = 1_717_171_717_000L,
            closedAt = null,
            closureReason = null,
            deletedAt = null,
            deleteReason = null,
        )

        OrderExportXlsxWriter(ZoneId.of("UTC")).write(listOf(row), output)

        val entries = unzip(output.toByteArray())
        assertNotNull(entries["[Content_Types].xml"])
        assertNotNull(entries["_rels/.rels"])
        assertNotNull(entries["xl/workbook.xml"])
        assertNotNull(entries["xl/_rels/workbook.xml.rels"])
        val sheet = entries["xl/worksheets/sheet1.xml"]
        assertNotNull(sheet)
        assertTrue(sheet!!.contains("Numero commessa"))
        assertTrue(sheet.contains("2026-0001"))
        assertTrue(sheet.contains("Schermo &amp; vetro &lt;rotto&gt;"))
        assertTrue(sheet.contains("Display &quot;nero&quot;"))
        assertTrue(sheet.contains("Nota con apostrofo &apos;"))
        assertTrue(sheet.contains("EUR 123,45"))
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        assertEquals(false, entries.isEmpty())
        return entries
    }
}
