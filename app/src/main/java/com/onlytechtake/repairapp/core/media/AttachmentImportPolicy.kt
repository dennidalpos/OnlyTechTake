package com.onlytechtake.repairapp.core.media

import android.content.Context
import com.onlytechtake.repairapp.R
import java.util.Locale

class AttachmentImportPolicy(
    val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
    private val allowedMimeTypes: Set<String> = DEFAULT_ALLOWED_MIME_TYPES,
) {
    init {
        require(maxSizeBytes > 0L) { "Dimensione massima allegato non valida" }
        require(allowedMimeTypes.isNotEmpty()) { "Allowlist allegati vuota" }
    }

    fun requireAllowed(
        context: Context,
        rawMimeType: String?,
        declaredSizeBytes: Long?,
    ) {
        val mimeType = normalize(rawMimeType)
        require(mimeType.isNotBlank()) { context.getString(R.string.error_unrecognized_attachment_type) }
        require(mimeType in allowedMimeTypes) {
            context.getString(
                R.string.error_attachment_type_not_allowed,
                mimeType
            )
        }

        if (declaredSizeBytes != null && declaredSizeBytes >= 0L) {
            require(declaredSizeBytes <= maxSizeBytes) {
                context.getString(
                    R.string.error_attachment_too_large,
                    maxSizeBytes.toMiBLabel(context)
                )
            }
        }
    }

    fun normalize(rawMimeType: String?): String =
        rawMimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()

    fun isImage(rawMimeType: String?): Boolean = normalize(rawMimeType) in IMAGE_MIME_TYPES

    companion object {
        const val DEFAULT_MAX_SIZE_BYTES = 25L * 1024L * 1024L

        private val IMAGE_MIME_TYPES =
            setOf(
                "image/jpeg",
                "image/png",
                "image/webp",
            )

        val DEFAULT_ALLOWED_MIME_TYPES: Set<String> =
            IMAGE_MIME_TYPES +
                setOf(
                    "application/pdf",
                    "text/plain",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                )
    }
}

fun Long.toMiBLabel(context: Context): String =
    if (this < 1024L * 1024L) {
        "$this ${context.getString(R.string.label_bytes)}"
    } else {
        "${this / (1024L * 1024L)} MiB"
    }
