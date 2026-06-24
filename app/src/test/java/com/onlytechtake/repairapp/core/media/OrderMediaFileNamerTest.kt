package com.onlytechtake.repairapp.core.media

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderMediaFileNamerTest {
    private val namer = OrderMediaFileNamer()
    private val context =
        mockk<Context> {
            every { getString(any()) } returns "commessa"
        }

    @Test
    fun photoFileNameUsesSafeOrderNumberAndTimestamp() {
        assertEquals("OTT_2026_0001_42.jpg", namer.photoFileName(context, "OTT/2026 0001", 42L))
    }

    @Test
    fun photoFileNameFallsBackWhenOrderNumberHasNoSafeCharacters() {
        assertEquals("commessa_42.jpg", namer.photoFileName(context, " /// ", 42L))
    }
}
