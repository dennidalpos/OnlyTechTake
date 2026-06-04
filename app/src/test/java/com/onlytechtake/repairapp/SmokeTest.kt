package com.onlytechtake.repairapp

import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun appPackageIsStable() {
        assertEquals("com.onlytechtake.repairapp", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun coreDatabaseEnumsKeepStableCodes() {
        assertEquals("PRESA_IN_CARICO", OrderStatus.PRESA_IN_CARICO.name)
        assertEquals("ACCEPT", CustomerFeedbackType.ACCEPT.name)
        assertEquals("ORDER_STATUS_CHANGED", AuditAction.ORDER_STATUS_CHANGED.name)
    }
}
