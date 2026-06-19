package com.onlytechtake.repairapp.core.demo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoModeFullTest {
    @Test
    fun fullFlavorCannotEnableDemoMode() {
        DemoMode.setEnabled(true)

        assertFalse(DemoMode.canEnable())
        assertFalse(DemoMode.isEnabled)
    }

    @Test
    fun fullFlavorDoesNotExposeDemoEntities() {
        val customerError =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.demoCustomer
            }
        val categoriesError =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.demoCategories
            }

        assertFalse(DemoMode.canEnable())
        org.junit.Assert.assertEquals("DemoMode non disponibile", customerError.message)
        org.junit.Assert.assertEquals("DemoMode non disponibile", categoriesError.message)
    }
}
