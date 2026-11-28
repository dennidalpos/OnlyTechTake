package com.onlytechtake.repairapp.core.orders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class OrderNumberGeneratorTest {
    private val generator = OrderNumberGenerator(ZoneId.of("UTC"))

    @Test
    fun yearPrefixUsesOttYearFormat() {
        assertEquals("OTT-2026-", generator.yearPrefix(1_767_225_600_000L))
    }

    @Test
    fun nextStartsAtOneWhenNoPreviousOrderExists() {
        assertEquals("OTT-2026-000001", generator.next("OTT-2026-", null))
    }

    @Test
    fun nextIncrementsLatestSequence() {
        assertEquals("OTT-2026-000043", generator.next("OTT-2026-", "OTT-2026-000042"))
    }
}
