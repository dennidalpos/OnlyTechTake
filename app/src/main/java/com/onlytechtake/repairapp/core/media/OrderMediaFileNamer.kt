package com.onlytechtake.repairapp.core.media

import android.content.Context
import com.onlytechtake.repairapp.R

class OrderMediaFileNamer {
    fun photoFileName(
        context: Context,
        orderNumber: String,
        timestampMillis: Long,
    ): String {
        val safeOrderNumber =
            sanitize(orderNumber).ifBlank { context.getString(R.string.label_order_fallback) }
        return "${safeOrderNumber}_$timestampMillis.jpg"
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
