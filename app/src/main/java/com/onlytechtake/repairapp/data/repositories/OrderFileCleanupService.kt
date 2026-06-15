package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import java.io.File

class OrderFileCleanupService(
    private val context: Context,
    private val mediaStorageProvider: MediaStorageProvider,
) {

    /**
     * Deletes all files and directories associated with an order.
     */
    fun deleteOrderFiles(orderId: String) {
        deleteRecursively(getOrderRootDataDir(orderId))
    }

    /**
     * Deletes a specific media or signature file.
     */
    fun deleteSpecificFile(localPath: String) {
        val file = File(localPath)
        if (file.exists()) {
            file.delete()
        }
        // Attempt to delete parent if empty (to keep things clean)
        file.parentFile?.deleteIfEmpty()
    }

    /**
     * Deletes intake (staging) media for a given session.
     */
    fun deleteIntakeMedia(sessionId: String) {
        deleteRecursively(getIntakeMediaDir(sessionId))
    }

    /**
     * Cleans up all staging and cache directories.
     */
    fun cleanupSharedCache(maxAgeMillis: Long = 24 * 60 * 60 * 1000L) {
        val sharedDir = getSharedCacheDir()
        if (!sharedDir.exists()) return

        val now = System.currentTimeMillis()
        sharedDir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > maxAgeMillis) {
                file.deleteRecursively()
            }
        }
    }

    /**
     * Cleans up orphaned files that are no longer referenced in the database.
     * This should be called with caution and typically by an admin.
     */
    fun cleanupOrphanedFiles(referencedPaths: Set<String>) {
        // Implementation for deep cleanup if needed
    }

    private fun getOrderRootDataDir(orderId: String) = File(mediaStorageProvider.getBaseMediaDir(), "orders/$orderId")
    private fun getIntakeMediaDir(sessionId: String) = mediaStorageProvider.getIntakeMediaDir(sessionId)
    private fun getSharedCacheDir() = File(context.cacheDir, "shared")

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        if (file.exists()) {
            file.delete()
        }
    }

    private fun File.deleteIfEmpty() {
        if (exists() && isDirectory && listFiles().isNullOrEmpty()) {
            delete()
            parentFile?.deleteIfEmpty()
        }
    }
}
