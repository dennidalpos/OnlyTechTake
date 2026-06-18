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
        val error =
            assertThrows(IllegalStateException::class.java) {
                DemoMode.demoOrder
            }

        assertFalse(DemoMode.canEnable())
        org.junit.Assert.assertEquals("DemoMode non disponibile", error.message)
    }
}
