package com.onlytechtake.repairapp.core.demo

import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoModeFullTest {
    @Test
    fun fullFlavorCannotEnableDemoMode() {
        DemoMode.setEnabled(enabled = true)

        assertFalse(DemoMode.canEnable())
        assertFalse(DemoMode.isEnabled)
    }

    @Test
    fun fullFlavorDoesNotExposeDemoEntities() {
        val customerError =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.getDemoCustomer(mockk())
            }
        val categoriesError =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.getDemoCategories(mockk())
            }

        assertFalse(DemoMode.canEnable())
        org.junit.Assert.assertEquals("DemoMode not available", customerError.message)
        org.junit.Assert.assertEquals("DemoMode not available", categoriesError.message)
    }
}
