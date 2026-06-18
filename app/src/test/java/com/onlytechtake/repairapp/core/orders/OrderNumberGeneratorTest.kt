package com.onlytechtake.repairapp.core.orders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class OrderNumberGeneratorTest {
    private val generator = OrderNumberGenerator(ZoneId.of("UTC"))

    @Test
    fun yearPrefixUsesRipYearFormat() {
        assertEquals("RIP-2026-", generator.yearPrefix(1_767_225_600_000L))
    }

    @Test
    fun nextStartsAtOneWhenNoPreviousOrderExists() {
        assertEquals("RIP-2026-000001", generator.next("RIP-2026-", null))
    }

    @Test
    fun nextIncrementsLatestSequence() {
        assertEquals("RIP-2026-000043", generator.next("RIP-2026-", "RIP-2026-000042"))
    }
}
