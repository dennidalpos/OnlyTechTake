package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class ExtractedBackup(
    val metadataText: String,
    val databaseFiles: Map<String, File>,
)

internal object BackupArchive {
    const val METADATA_ENTRY = "metadata/backup.json"
    const val DATABASE_ENTRY_PREFIX = "database/"
    const val FILES_ENTRY_PREFIX = "files/"
    const val CANONICAL_MEDIA_DIRECTORY = "app_media"

    fun extract(input: InputStream, restoreRoot: File): ExtractedBackup {
        val metadata = StringBuilder()
        val databaseFiles = mutableMapOf<String, File>()
        var hasMetadata = false

        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                try {
                    if (entry.isDirectory) continue
                    requireSafeEntry(entry.name)
                    when {
                        entry.name == METADATA_ENTRY -> {
                            hasMetadata = true
                            metadata.append(zip.readBytes().toString(Charsets.UTF_8))
                        }

                        entry.name.startsWith(DATABASE_ENTRY_PREFIX) -> {
                            val fileName = entry.name.removePrefix(DATABASE_ENTRY_PREFIX)
                            require(fileName.isNotBlank() && !fileName.contains('/')) {
                                "Backup database non valido"
                            }
                            val destination = safeChild(File(restoreRoot, "database"), fileName)
                            destination.parentFile?.mkdirs()
                            destination.outputStream().use(zip::copyTo)
                            databaseFiles[fileName] = destination
                        }

                        entry.name.startsWith(FILES_ENTRY_PREFIX) -> {
                            val relativePath = entry.name.removePrefix(FILES_ENTRY_PREFIX)
                            require(isSupportedAppFile(relativePath)) {
                                "Backup contiene file app non supportati"
                            }
                            val destination = safeChild(File(restoreRoot, "files"), relativePath)
                            destination.parentFile?.mkdirs()
                            destination.outputStream().use(zip::copyTo)
                        }
                    }
                } finally {
                    zip.closeEntry()
                }
            }
        }

        require(hasMetadata) { "Metadata backup mancanti" }
        require(databaseFiles.containsKey(RepairAppDatabase.DATABASE_NAME)) {
            "Database backup mancante"
        }
        return ExtractedBackup(metadata.toString(), databaseFiles)
    }

    fun restoreAppMedia(stagedFilesRoot: File, destination: File) {
        destination.deleteRecursively()
        destination.mkdirs()

        val canonicalSource = File(stagedFilesRoot, CANONICAL_MEDIA_DIRECTORY)
        if (canonicalSource.exists()) {
            canonicalSource.copyRecursively(destination, overwrite = true)
        }

        copyMissingTree(
            source = File(stagedFilesRoot, "order_media"),
            destination = File(destination, "order_media"),
        )
        restoreLegacyOrderTrees(
            source = File(stagedFilesRoot, "orders"),
            destination = destination,
        )
    }

    fun addDirectory(zip: ZipOutputStream, directory: File, entryDirectory: String) {
        if (!directory.exists()) return
        directory.walkTopDown()
            .filter(File::isFile)
            .forEach { file ->
                val relativePath = file.relativeTo(directory).invariantSeparatorsPath
                zip.putFileEntry(file, "$entryDirectory/$relativePath")
            }
    }

    private fun restoreLegacyOrderTrees(source: File, destination: File) {
        source.listFiles(File::isDirectory).orEmpty().forEach { orderDirectory ->
            val orderId = orderDirectory.name
            copyMissingTree(
                source = File(orderDirectory, "signatures"),
                destination = File(destination, "signatures/$orderId"),
            )
            copyMissingTree(
                source = File(orderDirectory, "documents"),
                destination = File(destination, "documents/$orderId"),
            )
            copyMissingTree(
                source = File(orderDirectory, "media"),
                destination = File(destination, "order_media/$orderId"),
            )
        }
    }

    private fun copyMissingTree(source: File, destination: File) {
        if (!source.exists()) return
        source.walkTopDown()
            .filter(File::isFile)
            .forEach { file ->
                val target = safeChild(destination, file.relativeTo(source).invariantSeparatorsPath)
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    file.copyTo(target)
                }
            }
    }

    private fun isSupportedAppFile(relativePath: String): Boolean =
        relativePath.startsWith("$CANONICAL_MEDIA_DIRECTORY/") ||
            relativePath.startsWith("orders/") ||
            relativePath.startsWith("order_media/")

    private fun requireSafeEntry(name: String) {
        require(name.isNotBlank()) { "Backup ZIP non valido" }
        require(!name.startsWith("/") && !name.startsWith("\\") && '\\' !in name) {
            "Percorso backup non valido"
        }
        val parts = name.split('/')
        require(parts.none { it == ".." || it.isBlank() }) { "Percorso backup non valido" }
    }

    private fun safeChild(root: File, relativePath: String): File {
        val destination = File(root, relativePath)
        val rootPath = root.canonicalPath + File.separator
        val destinationPath = destination.canonicalPath
        require(destinationPath.startsWith(rootPath)) { "Percorso backup non valido" }
        return destination
    }
}

internal fun ZipOutputStream.putFileEntry(file: File, entryName: String) {
    putNextEntry(ZipEntry(entryName))
    file.inputStream().use { input -> input.copyTo(this) }
    closeEntry()
}
