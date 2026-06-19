package com.onlytechtake.repairapp.core.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoModeShowcaseTest {
    @Test
    fun showcaseFlavorCannotEnableDemoMode() {
        DemoMode.setEnabled(true)

        assertFalse(DemoMode.canEnable())
        assertFalse(DemoMode.isEnabled)
    }

    @Test
    fun showcaseFlavorDoesNotExposeDemoEntities() {
        val customerError =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.demoCustomer
            }
        val categoriesError =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.demoCategories
            }

        assertEquals("DemoMode non disponibile", customerError.message)
        assertEquals("DemoMode non disponibile", categoriesError.message)
    }
}
