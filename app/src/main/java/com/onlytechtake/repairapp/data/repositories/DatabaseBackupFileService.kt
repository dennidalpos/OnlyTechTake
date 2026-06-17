package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.core.security.DatabaseKeyManager
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import java.io.File
import java.io.IOException

internal class DatabaseBackupFileService(
    private val context: Context,
) {
    fun databaseFile(): File = context.getDatabasePath(RepairAppDatabase.DATABASE_NAME)

    fun addDatabaseFiles(zip: ZipOutputStream, password: String?) {
        val dbFile = databaseFile()
        require(dbFile.exists()) { "Database non ancora creato" }
        val databaseFiles = dbFile.parentFile
            ?.listFiles { file -> (file.name == dbFile.name) || file.name.startsWith("${dbFile.name}-") }
            ?.sortedBy { it.name }
            .orEmpty()
        databaseFiles.forEach { file ->
            zip.putFileEntry(file, "database/${file.name}", password)
        }
    }

    fun validateDatabase(file: File) {
        if (!file.exists()) throw IOException("File database non trovato")

        val key = DatabaseKeyManager(context).getDatabaseKey()
        try {
            val db = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                file.absolutePath,
                key,
                null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                null,
                null
            )
            try {
                db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val result = cursor.getString(0)
                        if (result != "ok") {
                            throw IOException("Integrità database compromessa: $result")
                        }
                    } else {
                        throw IOException("Impossibile verificare integrità database")
                    }
                }
            } finally {
                db.close()
            }
        } catch (e: Exception) {
            throw IOException("Validazione database fallita (chiave errata o file corrotto): ${e.message}", e)
        }
    }

    fun replaceDatabaseFiles(databaseFiles: Map<String, File>) {
        val dbFile = databaseFile()
        dbFile.parentFile?.mkdirs()
        dbFile.parentFile
            ?.listFiles { file -> (file.name == dbFile.name) || file.name.startsWith("${dbFile.name}-") }
            ?.forEach { it.delete() }
        databaseFiles.forEach { (fileName, source) ->
            source.copyTo(File(dbFile.parentFile, fileName), overwrite = true)
        }
    }
}
