package com.onlytechtake.repairapp

import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun appPackageIsStable() {
        val expectedId = if (BuildConfig.APPLICATION_ID.endsWith(".demo")) {
            "com.onlytechtake.repairapp.demo"
        } else {
            "com.onlytechtake.repairapp"
        }
        assertEquals(expectedId, BuildConfig.APPLICATION_ID)
    }

    @Test
    fun coreDatabaseEnumsKeepStableCodes() {
        assertEquals("OPEN", OrderStatus.OPEN.name)
        assertEquals("ACCEPT", CustomerFeedbackType.ACCEPT.name)
        assertEquals("ORDER_STATUS_CHANGED", AuditAction.ORDER_STATUS_CHANGED.name)
    }
}
