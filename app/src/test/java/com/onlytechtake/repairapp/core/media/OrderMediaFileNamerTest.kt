package com.onlytechtake.repairapp.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderMediaFileNamerTest {
    private val namer = OrderMediaFileNamer()

    @Test
    fun photoFileNameUsesSafeOrderNumberAndTimestamp() {
        assertEquals("OTT_2026_0001_42.jpg", namer.photoFileName("OTT/2026 0001", 42L))
    }

    @Test
    fun photoFileNameFallsBackWhenOrderNumberHasNoSafeCharacters() {
        assertEquals("commessa_42.jpg", namer.photoFileName(" /// ", 42L))
    }
}
