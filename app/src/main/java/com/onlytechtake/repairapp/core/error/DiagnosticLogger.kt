package com.onlytechtake.repairapp.core.error

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticLogger(
    context: Context,
) {
    private val logDir = File(context.filesDir, "logs")
    private val logFile = File(logDir, "error_logs.txt")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    init {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }

    fun logError(
        contextTag: String?,
        message: String,
        throwable: Throwable?,
    ) {
        val timestamp = dateFormat.format(Date())
        val logLine =
            buildString {
                append("[$timestamp] ")
                append("[$contextTag] ")
                append(message)
                if (throwable != null) {
                    append("\n")
                    append(Log.getStackTraceString(throwable))
                }
                append("\n---\n")
            }

        try {
            FileOutputStream(logFile, true).use { output ->
                output.write(logLine.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("DiagnosticLogger", "Failed to write log", e)
        }
    }

    fun getLogSize(): Long = if (logFile.exists()) logFile.length() else 0L

    fun clearLogs() {
        if (logFile.exists()) {
            logFile.delete()
        }
    }

    fun getLogFile(): File = logFile
}
