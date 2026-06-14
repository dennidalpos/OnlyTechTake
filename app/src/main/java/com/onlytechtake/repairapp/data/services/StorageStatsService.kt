package com.onlytechtake.repairapp.data.services

import android.content.Context
import android.os.StatFs
import java.io.File

data class StorageStats(
    val freeDeviceBytes: Long,
    val totalDeviceBytes: Long,
    val appUsedBytes: Long,
    val databaseBytes: Long,
    val mediaBytes: Long,
    val documentsBytes: Long,
    val cacheBytes: Long,
)

class StorageStatsService(private val context: Context) {
    fun getStorageStats(): StorageStats {
        val stat = StatFs(context.filesDir.path)
        val freeBytes = stat.availableBytes
        val totalBytes = stat.totalBytes
        
        val dbFile = context.getDatabasePath("repair_app.db")
        val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
        val dbJournal = File(dbFile.path + "-journal")
        val dbWal = File(dbFile.path + "-wal")
        val totalDbBytes = dbBytes + (if (dbJournal.exists()) dbJournal.length() else 0L) + (if (dbWal.exists()) dbWal.length() else 0L)

        val orderMediaDir = File(context.filesDir, "order_media")
        val mediaBytes = getDirectorySize(orderMediaDir)
        
        val ordersDir = File(context.filesDir, "orders")
        val ordersBytes = getDirectorySize(ordersDir)
        
        val intakeMediaDir = File(context.filesDir, "intake_media")
        val intakeBytes = getDirectorySize(intakeMediaDir)
        
        val cacheBytes = getDirectorySize(context.cacheDir)
        
        val appUsedBytes = getDirectorySize(context.filesDir) + totalDbBytes + cacheBytes

        return StorageStats(
            freeDeviceBytes = freeBytes,
            totalDeviceBytes = totalBytes,
            appUsedBytes = appUsedBytes,
            databaseBytes = totalDbBytes,
            mediaBytes = mediaBytes + intakeBytes,
            documentsBytes = ordersBytes,
            cacheBytes = cacheBytes
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
