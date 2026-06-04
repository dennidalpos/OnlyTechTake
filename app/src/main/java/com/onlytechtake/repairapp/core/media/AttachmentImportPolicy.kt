package com.onlytechtake.repairapp.core.media

import java.util.Locale

class AttachmentImportPolicy(
    val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
    private val allowedMimeTypes: Set<String> = DEFAULT_ALLOWED_MIME_TYPES,
) {
    init {
        require(maxSizeBytes > 0L) { "Dimensione massima allegato non valida" }
        require(allowedMimeTypes.isNotEmpty()) { "Allowlist allegati vuota" }
    }

    fun requireAllowed(rawMimeType: String?, declaredSizeBytes: Long?) {
        val mimeType = normalize(rawMimeType)
        require(mimeType.isNotBlank()) { "Tipo allegato non riconosciuto" }
        require(mimeType in allowedMimeTypes) { "Tipo allegato non consentito: $mimeType" }

        if (declaredSizeBytes != null && declaredSizeBytes >= 0L) {
            require(declaredSizeBytes <= maxSizeBytes) {
                "Allegato troppo grande: massimo ${maxSizeBytes.toMiBLabel()}"
            }
        }
    }

    fun normalize(rawMimeType: String?): String =
        rawMimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()

    fun isImage(rawMimeType: String?): Boolean =
        normalize(rawMimeType) in IMAGE_MIME_TYPES

    companion object {
        const val DEFAULT_MAX_SIZE_BYTES = 25L * 1024L * 1024L

        private val IMAGE_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
        )

        val DEFAULT_ALLOWED_MIME_TYPES: Set<String> = IMAGE_MIME_TYPES + setOf(
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        )
    }
}

fun Long.toMiBLabel(): String =
    if (this < 1024L * 1024L) {
        "$this bytes"
    } else {
        "${this / (1024L * 1024L)} MiB"
    }
