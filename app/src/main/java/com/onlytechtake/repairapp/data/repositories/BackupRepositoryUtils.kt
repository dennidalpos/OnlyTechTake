package com.onlytechtake.repairapp.data.repositories

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatTime(millis: Long): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()),
    )

internal fun jsonString(value: String?): String =
    value
        ?.replace("\\", "\\\\")
        ?.replace("\"", "\\\"")
        ?.replace("\r", "\\r")
        ?.replace("\n", "\\n")
        ?.let { "\"$it\"" }
        ?: "null"
