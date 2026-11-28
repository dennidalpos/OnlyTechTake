package com.onlytechtake.repairapp

import com.onlytechtake.repairapp.domain.models.AuditAction
import com.onlytechtake.repairapp.domain.models.CustomerFeedbackType
import com.onlytechtake.repairapp.domain.models.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun appPackageIsStable() {
        val appId = BuildConfig.APPLICATION_ID
        val isDemo = appId.contains(".demo")
        val isShowcase = appId.contains(".showcase")
        val isDebug = appId.endsWith(".debug")

        val base = "com.onlytechtake.repairapp"
        val expected =
            when {
                isDemo && isDebug -> "$base.demo.debug"
                isDemo -> "$base.demo"
                isShowcase && isDebug -> "$base.showcase.debug"
                isShowcase -> "$base.showcase"
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
