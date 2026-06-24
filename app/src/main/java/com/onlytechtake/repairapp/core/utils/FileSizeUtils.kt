package com.onlytechtake.repairapp.core.utils

import java.util.Locale

fun formatFileSize(bytes: Long): String =
    when {
        bytes < 0L -> "0 B"
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        bytes < 1024L * 1024L * 1024L ->
            String.format(
                Locale.getDefault(),
                "%.1f MB",
                bytes / (1024.0 * 1024.0),
            )

        else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
