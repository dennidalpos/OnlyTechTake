package com.onlytechtake.repairapp.core.demo

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoModeShowcaseTest {
    @Test
    fun showcaseFlavorCannotEnableDemoMode() {
        DemoMode.setEnabled(enabled = true)

        assertFalse(DemoMode.canEnable())
        assertFalse(DemoMode.isEnabled)
    }

    @Test
    fun showcaseFlavorDoesNotExposeDemoEntities() {
        val customerError =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.getDemoCustomer(mockk())
            }
        val categoriesError =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.getDemoCategories(mockk())
            }

        assertEquals("DemoMode not available", customerError.message)
        assertEquals("DemoMode not available", categoriesError.message)
    }
}
