package com.onlytechtake.repairapp.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val formatterCache = ConcurrentHashMap<Pair<String, Locale>, DateTimeFormatter>()

private fun getFormatter(
    pattern: String,
    locale: Locale,
): DateTimeFormatter =
    formatterCache.getOrPut(pattern to locale) {
        DateTimeFormatter.ofPattern(pattern, locale)
    }

/**
 * Formats a timestamp in milliseconds into a UI-friendly date-time string.
 * Uses the provided [pattern], [zoneId] (defaults to system) and [locale] (defaults to system).
 */
fun formatUiDateTime(
    millis: Long,
    pattern: String = "dd/MM/yyyy HH:mm",
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val formatter = getFormatter(pattern, locale)
    return formatter.format(Instant.ofEpochMilli(millis).atZone(zoneId))
}
