package com.onlytechtake.repairapp.core.error

interface ErrorHandler {
    fun handleError(error: Throwable, context: String? = null)
    fun getErrorMessage(error: Throwable): String
}
