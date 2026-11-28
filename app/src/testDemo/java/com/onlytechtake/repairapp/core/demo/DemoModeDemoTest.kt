package com.onlytechtake.repairapp.core.demo

import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoModeDemoTest {
    @Test
    fun demoFlavorCanEnableDemoMode() {
        DemoMode.setEnabled(enabled = true)

        assertTrue(DemoMode.canEnable())
        assertTrue(DemoMode.isEnabled)
    }

    @Test
    fun demoFlavorExposesDemoEntities() {
        val customer = DemoMode.getDemoCustomer(mockk(relaxed = true))
        val categories = DemoMode.getDemoCategories(mockk(relaxed = true))

        assertNotNull(customer)
        assertTrue(categories.isNotEmpty())
    }
}
