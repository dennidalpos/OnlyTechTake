package com.onlytechtake.repairapp.data.services

import android.content.Context
import android.os.StatFs
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import java.io.File

data class StorageStats(
    val freeDeviceBytes: Long,
    val totalDeviceBytes: Long,
    val appUsedBytes: Long,
    val databaseBytes: Long,
    val mediaBytes: Long,
    val documentsBytes: Long,
    val cacheBytes: Long,
    val logsBytes: Long,
)

class StorageStatsService(
    private val context: Context,
    private val mediaStorageProvider: MediaStorageProvider,
) {
    fun getStorageStats(): StorageStats {
        val stat = StatFs(context.filesDir.path)
        val freeBytes = stat.availableBytes
        val totalBytes = stat.totalBytes

        val dbFile = context.getDatabasePath("repair_app.db")
        val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
        val dbJournal = File(dbFile.path + "-journal")
        val dbWal = File(dbFile.path + "-wal")
        val totalDbBytes =
            dbBytes + (if (dbJournal.exists()) dbJournal.length() else 0L) + (if (dbWal.exists()) dbWal.length() else 0L)

        val baseMediaDir = mediaStorageProvider.getBaseMediaDir()

        val mediaBytes = getDirectorySize(File(baseMediaDir, "order_media"))
        val signatureBytes = getDirectorySize(File(baseMediaDir, "signatures"))
        val documentBytes = getDirectorySize(File(baseMediaDir, "documents"))
        val intakeBytes = getDirectorySize(File(baseMediaDir, "intake_media"))

        val cacheBytes = getDirectorySize(context.cacheDir)
        val logsBytes = getDirectorySize(File(context.filesDir, "logs"))

        val appUsedBytes = getDirectorySize(context.filesDir) + totalDbBytes + cacheBytes

        return StorageStats(
            freeDeviceBytes = freeBytes,
            totalDeviceBytes = totalBytes,
            appUsedBytes = appUsedBytes,
            databaseBytes = totalDbBytes,
            mediaBytes = mediaBytes + intakeBytes,
            documentsBytes = documentBytes + signatureBytes,
            cacheBytes = cacheBytes,
            logsBytes = logsBytes,
        )
    }

    private fun getDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        if (directory.isFile) return directory.length()
        var size = 0L
        directory.listFiles()?.forEach { size += getDirectorySize(it) }
        return size
    }
}
