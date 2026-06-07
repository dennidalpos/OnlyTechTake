package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderStatusPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class StatusWorkflowTest {
    @Test
    fun openOrderStartsWithProgressOrClose() {
        assertEquals(
            listOf(
                OrderStatusAction.START_PROGRESS,
                OrderStatusAction.CLOSE_ORDER,
            ),
            availableOrderStatusActions(OrderStatus.OPEN),
        )
    }

    @Test
    fun diagnosisCanRecordResultOrClose() {
        val actions = availableOrderStatusActions(OrderStatus.DIAGNOSIS)

        assertTrue(actions.contains(OrderStatusAction.RECORD_RESULT))
        assertTrue(actions.contains(OrderStatusAction.CLOSE_ORDER))
    }

    @Test
    fun closedStatusesHaveNoOperatorActions() {
        assertTrue(availableOrderStatusActions(OrderStatus.CLOSED).isEmpty())
    }

    @Test
    fun onlyClosingStatusActionRequiresConfirmation() {
        assertTrue(OrderStatusAction.CLOSE_ORDER.requiresConfirmation())
        assertTrue(!OrderStatusAction.START_DIAGNOSIS.requiresConfirmation())
    }

    @Test
    fun closeActionRequiresManualClosureReason() {
        assertEquals(null, OrderStatusAction.CLOSE_ORDER.closureReason)
    }

    @Test
    fun closingRequiresClosureReason() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderStatusPolicy.requireAllowed(OrderStatus.READY_FOR_PICKUP, OrderStatus.CLOSED)
        }
    }
}
