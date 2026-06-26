package com.onlytechtake.repairapp.core.error

import android.content.Context
import android.util.Log
import com.onlytechtake.repairapp.R

class DefaultErrorHandler(
    private val context: Context,
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

    override fun getErrorMessage(error: Throwable): String = error.message ?: context.getString(R.string.error_unknown_generic)
}
