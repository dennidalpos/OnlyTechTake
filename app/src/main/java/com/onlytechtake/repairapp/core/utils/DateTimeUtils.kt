package com.onlytechtake.repairapp.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val uiDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

fun formatUiDateTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    uiDateTimeFormatter.format(Instant.ofEpochMilli(millis).atZone(zoneId))
