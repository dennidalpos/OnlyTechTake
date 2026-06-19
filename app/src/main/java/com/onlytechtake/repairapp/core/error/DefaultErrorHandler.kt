package com.onlytechtake.repairapp.core.error

import android.util.Log

class DefaultErrorHandler : ErrorHandler {
    override fun handleError(
        error: Throwable,
        context: String?,
    ) {
        Log.e(context ?: "ErrorHandler", "Error: ${error.message}", error)
    }

    override fun getErrorMessage(error: Throwable): String =
        error.message ?: "Si è verificato un errore"
}
