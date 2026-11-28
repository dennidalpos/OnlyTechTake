package com.onlytechtake.repairapp.core.utils

import java.util.Locale

/**
 * Formats a file size in bytes into a human-readable string (B, KB, MB, GB).
 * Uses the provided [locale] for decimal formatting (defaults to [Locale.getDefault]).
 */
fun formatFileSize(
    bytes: Long,
    locale: Locale = Locale.getDefault(),
): String =
    when {
        bytes < 0L -> {
            "0 B"
        }

        bytes < 1024L -> {
            "$bytes B"
        }

        bytes < 1024L * 1024L -> {
            "${bytes / 1024L} KB"
        }

        bytes < 1024L * 1024L * 1024L -> {
            String.format(
                locale,
                "%.1f MB",
                bytes / (1024.0 * 1024.0),
            )
        }

        else -> {
            String.format(locale, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
