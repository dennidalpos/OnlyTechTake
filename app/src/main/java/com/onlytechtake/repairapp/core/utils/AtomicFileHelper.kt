package com.onlytechtake.repairapp.core.utils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object AtomicFileHelper {
    /**
     * Writes an input stream to a destination file atomically using a temporary file.
     */
    fun writeAtomically(
        destination: File,
        inputStream: InputStream,
    ) {
        val parent =
            destination.parentFile
                ?: throw IOException("No parent directory for ${destination.path}")
        if (!parent.exists()) {
            if (!parent.mkdirs()) throw IOException("Could not create directory ${parent.path}")
        }

        val tempFile = File(parent, "${destination.name}.tmp_${System.currentTimeMillis()}")
        try {
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            if (destination.exists()) {
                if (!destination.delete()) throw IOException("Could not delete existing file ${destination.path}")
            }

            if (!tempFile.renameTo(destination)) {
                throw IOException("Could not rename ${tempFile.path} to ${destination.path}")
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    /**
     * Executes a block that writes to a temporary file, then renames it to destination.
     */
    fun writeAtomically(
        destination: File,
        block: (File) -> Unit,
    ) {
        val parent =
            destination.parentFile
                ?: throw IOException("No parent directory for ${destination.path}")
        if (!parent.exists()) {
            if (!parent.mkdirs()) throw IOException("Could not create directory ${parent.path}")
        }

        val tempFile = File(parent, "${destination.name}.tmp_${System.currentTimeMillis()}")
        try {
            block(tempFile)

            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw IOException("Write operation produced empty or missing file")
            }

            if (destination.exists()) {
                if (!destination.delete()) throw IOException("Could not delete existing file ${destination.path}")
            }

            if (!tempFile.renameTo(destination)) {
                throw IOException("Could not rename ${tempFile.path} to ${destination.path}")
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }
}
