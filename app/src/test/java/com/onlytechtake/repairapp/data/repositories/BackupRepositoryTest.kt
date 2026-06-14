package com.onlytechtake.repairapp.data.repositories

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream

class BackupRepositoryTest {
    private val context = mockk<Context>(relaxed = true)
    private val contentResolver = mockk<ContentResolver>()
    private val repository = BackupRepository(
        context = context,
        categoryDao = mockk(),
        templateDao = mockk(),
        settingDao = mockk(),
        auditLogDao = mockk(relaxed = true),
    )

    @Before
    fun setUp() {
        every { context.contentResolver } returns contentResolver
        every { context.cacheDir } returns File("build/tmp/test_restore").also { it.mkdirs() }
        every { context.filesDir } returns File("build/tmp/test_files").also { it.mkdirs() }
    }

    @Test
    fun restoreCompleteBackupFailsWithIncompatibleVersion() = runBlocking {
        val actor = mockAdmin()
        val uri = mockk<Uri>()
        
        // Create a fake ZIP with wrong databaseVersion
        val zipContent = createFakeBackupZip(databaseVersion = 0)
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(zipContent)
        
        val result = repository.restoreCompleteBackup(actor, uri)
        
        assertTrue(result.isFailure)
        val expectedMessage = if (BuildConfig.IS_DEMO_VERSION) {
            "Funzione disponibile nella versione Full"
        } else {
            "Versione backup non compatibile"
        }
        assertEquals(expectedMessage, result.exceptionOrNull()?.message)
    }

    private fun createFakeBackupZip(databaseVersion: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            zip.putNextEntry(ZipEntry("metadata/backup.json"))
            zip.write("""{"databaseVersion": $databaseVersion}""".toByteArray())
            zip.closeEntry()
            
            zip.putNextEntry(ZipEntry("database/${RepairAppDatabase.DATABASE_NAME}"))
            zip.write("fake db content".toByteArray())
            zip.closeEntry()
        }
        return bos.toByteArray()
    }

    private fun mockAdmin() = AuthenticatedUser(
        id = "admin-1",
        displayName = "Admin",
        username = "admin",
        role = UserRole.ADMIN,
        permissions = UserPermission.entries.toSet()
    )
}
