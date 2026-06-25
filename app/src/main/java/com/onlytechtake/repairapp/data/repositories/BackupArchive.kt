package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.InputStream

internal data class ExtractedBackup(
    val metadataText: String,
    val databaseFiles: Map<String, File>,
)

internal object BackupArchive {
    const val METADATA_ENTRY = "metadata/backup.json"
    const val DATABASE_ENTRY_PREFIX = "database/"
    const val FILES_ENTRY_PREFIX = "files/"
    const val CANONICAL_MEDIA_DIRECTORY = "app_media"

    fun extract(
        context: Context,
        input: InputStream,
        restoreRoot: File,
        password: CharArray? = null,
    ): ExtractedBackup {
        val metadata = StringBuilder()
        val databaseFiles = mutableMapOf<String, File>()
        var hasMetadata = false

        ZipInputStream(input, password).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                try {
                    if (entry.isDirectory) continue
                    requireSafeEntry(context, entry.fileName)
                    when {
                        entry.fileName == METADATA_ENTRY -> {
                            hasMetadata = true
                            metadata.append(zip.readBytes().toString(Charsets.UTF_8))
                        }

                        entry.fileName.startsWith(DATABASE_ENTRY_PREFIX) -> {
                            val fileName = entry.fileName.removePrefix(DATABASE_ENTRY_PREFIX)
                            require(fileName.isNotBlank() && !fileName.contains('/')) {
                                context.getString(R.string.error_invalid_db_backup)
                            }
                            val destination =
                                safeChild(context, File(restoreRoot, "database"), fileName)
                            destination.parentFile?.mkdirs()
                            destination.outputStream().use(zip::copyTo)
                            databaseFiles[fileName] = destination
                        }

                        entry.fileName.startsWith(FILES_ENTRY_PREFIX) -> {
                            val relativePath = entry.fileName.removePrefix(FILES_ENTRY_PREFIX)
                            require(isSupportedAppFile(relativePath)) {
                                context.getString(R.string.error_unsupported_app_files)
                            }
                            val destination =
                                safeChild(context, File(restoreRoot, "files"), relativePath)
                            destination.parentFile?.mkdirs()
                            destination.outputStream().use(zip::copyTo)
                        }
                    }
                } finally {
                    // zip4j does not have closeEntry for input stream
                }
            }
        }

        require(hasMetadata) { context.getString(R.string.error_backup_metadata_missing) }
        require(databaseFiles.containsKey(RepairAppDatabase.DATABASE_NAME)) {
            context.getString(R.string.error_backup_db_missing)
        }
        return ExtractedBackup(metadata.toString(), databaseFiles)
    }

    fun restoreAppMedia(
        stagedFilesRoot: File,
        destination: File,
    ) {
        destination.deleteRecursively()
        destination.mkdirs()

        val canonicalSource = File(stagedFilesRoot, CANONICAL_MEDIA_DIRECTORY)
        if (canonicalSource.exists()) {
            canonicalSource.copyRecursively(destination, overwrite = true)
        }
    }

    fun addDirectory(
        zip: ZipOutputStream,
        directory: File,
        entryDirectory: String,
        password: String? = null,
    ) {
        if (!directory.exists()) return
        directory
            .walkTopDown()
            .filter(File::isFile)
            .forEach { file ->
                val relativePath = file.relativeTo(directory).invariantSeparatorsPath
                zip.putFileEntry(file, "$entryDirectory/$relativePath", password)
            }
    }

    private fun isSupportedAppFile(relativePath: String): Boolean =
        relativePath.startsWith("$CANONICAL_MEDIA_DIRECTORY/")

    private fun requireSafeEntry(
        context: Context,
        name: String,
    ) {
        require(name.isNotBlank()) { context.getString(R.string.error_invalid_zip_backup) }
        require(!name.startsWith("/") && !name.startsWith("\\") && '\\' !in name) {
            context.getString(R.string.error_invalid_backup_path)
        }
        val parts = name.split('/')
        require(parts.none { it == ".." || it.isBlank() }) { context.getString(R.string.error_invalid_backup_path) }
    }

    private fun safeChild(
        context: Context,
        root: File,
        relativePath: String,
    ): File {
        val destination = File(root, relativePath)
        val rootPath = root.canonicalPath + File.separator
        val destinationPath = destination.canonicalPath
        require(destinationPath.startsWith(rootPath)) { context.getString(R.string.error_invalid_backup_path) }
        return destination
    }
}

internal fun ZipOutputStream.putFileEntry(
    file: File,
    entryName: String,
    password: String? = null,
) {
    val params = ZipParameters()
    params.fileNameInZip = entryName
    if (password != null) {
        params.isEncryptFiles = true
        params.encryptionMethod = EncryptionMethod.AES
        params.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
    }
    putNextEntry(params)
    file.inputStream().use { input -> input.copyTo(this) }
    closeEntry()
}
