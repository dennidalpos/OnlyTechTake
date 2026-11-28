package com.onlytechtake.repairapp.data.orders

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
        deleteRecursively(mediaStorageProvider.getOrderMediaDir(orderId))
        deleteRecursively(mediaStorageProvider.getOrderSignaturesDir(orderId))
        deleteRecursively(mediaStorageProvider.getOrderDocumentsDir(orderId))
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
        val baseDir = mediaStorageProvider.getBaseMediaDir()
        if (!baseDir.exists()) return

        baseDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val path = file.absolutePath
                if (!referencedPaths.contains(path) && !path.contains("intake_media")) {
                    file.delete()
                }
            }
        }

        // Clean up empty directories
        baseDir.walkBottomUp().forEach { file ->
            if (file.isDirectory && file != baseDir && !file.absolutePath.contains("intake_media")) {
                file.deleteIfEmpty()
            }
        }
    }

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
