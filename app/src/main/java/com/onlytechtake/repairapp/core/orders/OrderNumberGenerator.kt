package com.onlytechtake.repairapp.core.orders

import java.time.Instant
import java.time.ZoneId

class OrderNumberGenerator(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun yearPrefix(nowMillis: Long): String {
        val year = Instant.ofEpochMilli(nowMillis).atZone(zoneId).year
        return "$PREFIX-$year-"
    }

    fun next(
        yearPrefix: String,
        latestOrderNumber: String?,
    ): String {
        val latestSequence =
            latestOrderNumber
                ?.takeIf { it.startsWith(yearPrefix) }
                ?.substringAfterLast("-")
                ?.toIntOrNull()
                ?: 0

        return yearPrefix + (latestSequence + 1).toString().padStart(SEQUENCE_WIDTH, '0')
    }

    private companion object {
        const val PREFIX = "OTT"
        const val SEQUENCE_WIDTH = 6
    }
}
