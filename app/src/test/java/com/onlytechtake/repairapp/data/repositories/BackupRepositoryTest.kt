package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRepositoryTest {
    private lateinit var testRoot: File

    @Before
    fun setUp() {
        testRoot = File("build/tmp/backup-${UUID.randomUUID()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        testRoot.deleteRecursively()
    }

    @Test
    fun exportIncludesCanonicalAppMediaPath() {
        val mediaRoot = File(testRoot, "app_media")
        File(mediaRoot, "order_media/order-1/photo.jpg").also {
            it.parentFile?.mkdirs()
            it.writeText("photo")
        }
        val output = ByteArrayOutputStream()

        ZipOutputStream(output).use { zip ->
            BackupArchive.addDirectory(zip, mediaRoot, "files/app_media")
        }

        assertEquals(
            mapOf("files/app_media/order_media/order-1/photo.jpg" to "photo"),
            unzip(output.toByteArray()),
        )
    }

    @Test
    fun restoreAcceptsCanonicalAppMedia() {
        val extracted = extract(
            mapOf(
                "files/app_media/order_media/order-1/photo.jpg" to "photo",
                "files/app_media/signatures/order-1/signature.png" to "signature",
            ),
        )
        val destination = File(testRoot, "restored")

        BackupArchive.restoreAppMedia(File(extracted, "files"), destination)

        assertEquals("photo", File(destination, "order_media/order-1/photo.jpg").readText())
        assertEquals("signature", File(destination, "signatures/order-1/signature.png").readText())
    }

    @Test
    fun extractionRejectsUnsafeZipPaths() {
        listOf(
            "../outside.txt",
            "/absolute.txt",
            "files/app_media/../../outside.txt",
            "files\\app_media\\photo.jpg",
        ).forEach { unsafePath ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                BackupArchive.extract(
                    ByteArrayInputStream(backupZip(mapOf(unsafePath to "unsafe"))),
                    File(testRoot, "unsafe-${unsafePath.hashCode()}"),
                )
            }
            assertEquals("Percorso backup non valido", error.message)
        }
    }

    private fun extract(files: Map<String, String>): File {
        val destination = File(testRoot, "extract-${UUID.randomUUID()}")
        BackupArchive.extract(ByteArrayInputStream(backupZip(files)), destination)
        return destination
    }

    private fun backupZip(files: Map<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            val requiredEntries = linkedMapOf(
                BackupArchive.METADATA_ENTRY to
                    """{"databaseVersion": ${RepairAppDatabase.DATABASE_VERSION}}""",
                "${BackupArchive.DATABASE_ENTRY_PREFIX}${RepairAppDatabase.DATABASE_NAME}" to "database",
            )
            (requiredEntries + files).forEach { (name, content) ->
                val params = ZipParameters().apply { fileNameInZip = name }
                zip.putNextEntry(params)
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.getNextEntry() ?: break
                entries[entry.fileName] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return entries
    }
}
