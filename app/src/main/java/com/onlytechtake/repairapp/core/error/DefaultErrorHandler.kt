package com.onlytechtake.repairapp.core.error

import android.util.Log

class DefaultErrorHandler(
    private val diagnosticLogger: DiagnosticLogger,
) : ErrorHandler {
    override fun handleError(
        error: Throwable,
        context: String?,
    ) {
        Log.e(context ?: "ErrorHandler", "Error: ${error.message}", error)
        diagnosticLogger.logError(context, error.message ?: "Unknown error", error)
    }

    override fun handleWarning(
        message: String,
        context: String?,
    ) {
        Log.w(context ?: "ErrorHandler", "Warning: $message")
        diagnosticLogger.logWarning(context, message)
    }

    override fun getErrorMessage(error: Throwable): String = error.message ?: "Si è verificato un errore"
}
