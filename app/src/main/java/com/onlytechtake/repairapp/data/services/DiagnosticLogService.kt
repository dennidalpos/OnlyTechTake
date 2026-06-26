package com.onlytechtake.repairapp.data.services

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import net.lingala.zip4j.ZipFile
import java.io.File
import java.util.UUID

class DiagnosticLogService(
    private val context: Context,
    private val diagnosticLogger: DiagnosticLogger,
) {
    fun clearLogs() = diagnosticLogger.clearLogs()

    fun collectAndPrepareZip(password: String? = null): File? {
        val logFile = diagnosticLogger.getLogFile()
        if (!logFile.exists() || logFile.length() == 0L) {
            // Create a dummy log if none exists to avoid empty zip error
            diagnosticLogger.logError(
                "DiagnosticLogService",
                "Automatic log generation for export",
                null,
            )
        }

        val sharedCacheDir = File(context.cacheDir, "shared")
        if (!sharedCacheDir.exists()) {
            sharedCacheDir.mkdirs()
        }

        val zipFile = File(sharedCacheDir, "diagnostic_logs_${UUID.randomUUID()}.zip")
        try {
            val zip = ZipFile(zipFile, password?.toCharArray())
            val params =
                net.lingala.zip4j.model
                    .ZipParameters()
            if (password != null) {
                params.isEncryptFiles = true
                params.encryptionMethod = net.lingala.zip4j.model.enums.EncryptionMethod.AES
                params.aesKeyStrength = net.lingala.zip4j.model.enums.AesKeyStrength.KEY_STRENGTH_256
            }
            zip.addFile(logFile, params)
            return zipFile
        } catch (e: Exception) {
            return null
        }
    }

    fun createShareIntent(zipFile: File): Intent {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile,
            )

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Diagnostic Logs - OnlyTechTake")
                putExtra(Intent.EXTRA_TEXT, "Diagnostic logs for OnlyTechTake Repair App.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        return Intent.createChooser(intent, context.getString(R.string.order_share))
    }
}
