package com.onlytechtake.repairapp.data.repositories

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
        input: InputStream,
        restoreRoot: File,
        password: CharArray? = null,
    ): ExtractedBackup {
        val metadata = StringBuilder()
        val databaseFiles = mutableMapOf<String, File>()
        var hasMetadata = false

        ZipInputStream(input, password).use { zip ->
            while (true) {
                val entry = zip.getNextEntry() ?: break
                try {
                    if (entry.isDirectory) continue
                    requireSafeEntry(entry.fileName)
                    when {
                        entry.fileName == METADATA_ENTRY -> {
                            hasMetadata = true
                            metadata.append(zip.readBytes().toString(Charsets.UTF_8))
                        }

                        entry.fileName.startsWith(DATABASE_ENTRY_PREFIX) -> {
                            val fileName = entry.fileName.removePrefix(DATABASE_ENTRY_PREFIX)
                            require(fileName.isNotBlank() && !fileName.contains('/')) {
                                "Backup database non valido"
                            }
                            val destination = safeChild(File(restoreRoot, "database"), fileName)
                            destination.parentFile?.mkdirs()
                            destination.outputStream().use(zip::copyTo)
                            databaseFiles[fileName] = destination
                        }

                        entry.fileName.startsWith(FILES_ENTRY_PREFIX) -> {
                            val relativePath = entry.fileName.removePrefix(FILES_ENTRY_PREFIX)
                            require(isSupportedAppFile(relativePath)) {
                                "Backup contiene file app non supportati"
                            }
                            val destination = safeChild(File(restoreRoot, "files"), relativePath)
                            destination.parentFile?.mkdirs()
                            destination.outputStream().use(zip::copyTo)
                        }
                    }
                } finally {
                    // zip4j does not have closeEntry for input stream
                }
            }
        }

        require(hasMetadata) { "Metadata backup mancanti" }
        require(databaseFiles.containsKey(RepairAppDatabase.DATABASE_NAME)) {
            "Database backup mancante"
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

    private fun isSupportedAppFile(relativePath: String): Boolean = relativePath.startsWith("$CANONICAL_MEDIA_DIRECTORY/")

    private fun requireSafeEntry(name: String) {
        require(name.isNotBlank()) { "Backup ZIP non valido" }
        require(!name.startsWith("/") && !name.startsWith("\\") && '\\' !in name) {
            "Percorso backup non valido"
        }
        val parts = name.split('/')
        require(parts.none { it == ".." || it.isBlank() }) { "Percorso backup non valido" }
    }

    private fun safeChild(
        root: File,
        relativePath: String,
    ): File {
        val destination = File(root, relativePath)
        val rootPath = root.canonicalPath + File.separator
        val destinationPath = destination.canonicalPath
        require(destinationPath.startsWith(rootPath)) { "Percorso backup non valido" }
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
