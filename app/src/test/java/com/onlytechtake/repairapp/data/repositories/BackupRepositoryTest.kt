package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import io.mockk.every
import io.mockk.mockk
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class BackupRepositoryTest {
    private lateinit var testRoot: File
    private val context =
        mockk<Context>(relaxed = true) {
            every { getString(any()) } returns "error"
        }

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
        val extracted =
            extract(
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
            val error =
                assertThrows(IllegalArgumentException::class.java) {
                    BackupArchive.extract(
                        context,
                        ByteArrayInputStream(backupZip(mapOf(unsafePath to "unsafe"))),
                        File(testRoot, "unsafe-${unsafePath.hashCode()}"),
                    )
                }
            assertEquals("error", error.message)
        }
    }

    @Test
    fun extractionRequiresMetadataAndMainDatabase() {
        val missingMetadata =
            assertThrows(IllegalArgumentException::class.java) {
                BackupArchive.extract(
                    context,
                    ByteArrayInputStream(
                        rawZip(
                            mapOf("${BackupArchive.DATABASE_ENTRY_PREFIX}${RepairAppDatabase.DATABASE_NAME}" to "database"),
                        ),
                    ),
                    File(testRoot, "missing-metadata"),
                )
            }
        assertEquals("error", missingMetadata.message)

        val missingDatabase =
            assertThrows(IllegalArgumentException::class.java) {
                BackupArchive.extract(
                    context,
                    ByteArrayInputStream(
                        rawZip(mapOf(BackupArchive.METADATA_ENTRY to """{"databaseVersion": ${RepairAppDatabase.DATABASE_VERSION}}""")),
                    ),
                    File(testRoot, "missing-database"),
                )
            }
        assertEquals("error", missingDatabase.message)
    }

    @Test
    fun extractionRejectsFilesOutsideCanonicalAppMedia() {
        listOf(
            "files/databases/${RepairAppDatabase.DATABASE_NAME}",
            "files/shared_prefs/database_security_prefs.xml",
            "files/cache/shared/document.pdf",
        ).forEach { unsupportedPath ->
            val error =
                assertThrows(IllegalArgumentException::class.java) {
                    BackupArchive.extract(
                        context,
                        ByteArrayInputStream(backupZip(mapOf(unsupportedPath to "data"))),
                        File(testRoot, "unsupported-${unsupportedPath.hashCode()}"),
                    )
                }
            assertEquals("error", error.message)
        }
    }

    @Test
    fun restoreWithoutCanonicalAppMediaClearsDestinationOnly() {
        val stagedFilesRoot = File(testRoot, "staged-files").apply { mkdirs() }
        val destination = File(testRoot, "destination")
        File(destination, "order_media/order-1/photo.jpg").also {
            it.parentFile?.mkdirs()
            it.writeText("old photo")
        }

        BackupArchive.restoreAppMedia(stagedFilesRoot, destination)

        assertTrue(destination.exists())
        assertFalse(File(destination, "order_media/order-1/photo.jpg").exists())
    }

    private fun extract(files: Map<String, String>): File {
        val destination = File(testRoot, "extract-${UUID.randomUUID()}")
        BackupArchive.extract(context, ByteArrayInputStream(backupZip(files)), destination)
        return destination
    }

    private fun backupZip(files: Map<String, String>): ByteArray =
        rawZip(
            linkedMapOf(
                BackupArchive.METADATA_ENTRY to
                    """{"databaseVersion": ${RepairAppDatabase.DATABASE_VERSION}}""",
                "${BackupArchive.DATABASE_ENTRY_PREFIX}${RepairAppDatabase.DATABASE_NAME}" to "database",
            ) + files,
        )

    private fun rawZip(files: Map<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { (name, content) ->
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
                val entry = zip.nextEntry ?: break
                entries[entry.fileName] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return entries
    }
}
