package com.onlytechtake.repairapp

import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun appPackageIsStable() {
        val appId = BuildConfig.APPLICATION_ID
        val isDemo = appId.contains(".demo")
        val isDebug = appId.endsWith(".debug")

        val base = "com.onlytechtake.repairapp"
        val expected = when {
            isDemo && isDebug -> "$base.demo.debug"
            isDemo -> "$base.demo"
            isDebug -> "$base.debug"
            else -> base
        }
        assertEquals("APPLICATION_ID non corrisponde alla configurazione attesa", expected, appId)
    }

    @Test
    fun coreDatabaseEnumsKeepStableCodes() {
        assertEquals("OPEN", OrderStatus.OPEN.name)
        assertEquals("ACCEPT", CustomerFeedbackType.ACCEPT.name)
        assertEquals("ORDER_STATUS_CHANGED", AuditAction.ORDER_STATUS_CHANGED.name)
    }
}
