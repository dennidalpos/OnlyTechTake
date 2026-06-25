package com.onlytechtake.repairapp.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats a timestamp in milliseconds into a UI-friendly date-time string.
 * Uses the provided [zoneId] (defaults to system) and [locale] (defaults to system).
 */
fun formatUiDateTime(
    millis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
    return formatter.format(Instant.ofEpochMilli(millis).atZone(zoneId))
}
