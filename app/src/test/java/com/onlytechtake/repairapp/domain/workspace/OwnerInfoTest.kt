package com.onlytechtake.repairapp.domain.workspace

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerInfoTest {
    @Test
    fun `isComplete returns true when mandatory fields are present`() {
        val info =
            OwnerInfo(
                name = "Test Lab",
                vatNumber = "IT123456789",
                address = "Test Street 1",
            )
        assertTrue(info.isComplete())
    }

    @Test
    fun `isComplete returns false when name is missing`() {
        val info =
            OwnerInfo(
                name = "",
                vatNumber = "IT123456789",
                address = "Test Street 1",
            )
        assertFalse(info.isComplete())
    }

    @Test
    fun `isComplete returns false when vatNumber is missing`() {
        val info =
            OwnerInfo(
                name = "Test Lab",
                vatNumber = "",
                address = "Test Street 1",
            )
        assertFalse(info.isComplete())
    }

    @Test
    fun `isComplete returns false when address is missing`() {
        val info =
            OwnerInfo(
                name = "Test Lab",
                vatNumber = "IT123456789",
                address = "",
            )
        assertFalse(info.isComplete())
    }

    @Test
    fun `isComplete returns false for empty info`() {
        assertFalse(OwnerInfo().isComplete())
    }
}
