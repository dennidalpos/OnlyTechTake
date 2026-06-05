package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderCleanupPolicyTest {
    @Test
    fun cleanupSelectsDiscardedReasonsOnly() {
        assertTrue(ClosureReason.CUSTOMER_DECLINED.isDiscardedForCleanup())
        assertTrue(ClosureReason.CANCELLED.isDiscardedForCleanup())
        assertTrue(ClosureReason.NOT_REPAIRABLE.isDiscardedForCleanup())

        assertFalse(ClosureReason.COMPLETED.isDiscardedForCleanup())
        assertFalse(ClosureReason.OTHER.isDiscardedForCleanup())
        assertFalse(null.isDiscardedForCleanup())
    }
}
