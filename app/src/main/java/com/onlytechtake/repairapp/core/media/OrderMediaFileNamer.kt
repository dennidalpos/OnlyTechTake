package com.onlytechtake.repairapp.core.media

class OrderMediaFileNamer {
    fun photoFileName(orderNumber: String, timestampMillis: Long): String {
        val safeOrderNumber = sanitize(orderNumber).ifBlank { "commessa" }
        return "${safeOrderNumber}_${timestampMillis}.jpg"
    }

    fun sanitize(value: String): String =
        value
            .trim()
            .replace(unsafeCharacters, "_")
            .replace(duplicateUnderscores, "_")
            .trim('_')

    private companion object {
        val unsafeCharacters = Regex("[^A-Za-z0-9._-]+")
        val duplicateUnderscores = Regex("_+")
    }
}
