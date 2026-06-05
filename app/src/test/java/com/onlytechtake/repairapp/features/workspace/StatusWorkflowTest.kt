package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderStatusPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class StatusWorkflowTest {
    @Test
    fun newOrderStartsWithDiagnosisOrClose() {
        assertEquals(
            listOf(
                OrderStatusAction.START_DIAGNOSIS,
                OrderStatusAction.CLOSE_COMPLETED,
                OrderStatusAction.CLOSE_REJECTED,
                OrderStatusAction.CLOSE_OTHER,
            ),
            availableOrderStatusActions(OrderStatus.NEW),
        )
    }

    @Test
    fun waitingCustomerCanStartWorkOrClose() {
        val actions = availableOrderStatusActions(OrderStatus.WAITING_CUSTOMER)

        assertTrue(actions.contains(OrderStatusAction.START_WORK))
        assertTrue(actions.contains(OrderStatusAction.CLOSE_COMPLETED))
        assertTrue(actions.contains(OrderStatusAction.CLOSE_REJECTED))
        assertTrue(actions.contains(OrderStatusAction.CLOSE_OTHER))
    }

    @Test
    fun closedStatusesHaveNoOperatorActions() {
        assertTrue(availableOrderStatusActions(OrderStatus.CLOSED).isEmpty())
    }

    @Test
    fun onlyClosingStatusActionRequiresConfirmation() {
        assertTrue(OrderStatusAction.CLOSE_COMPLETED.requiresConfirmation())
        assertTrue(OrderStatusAction.CLOSE_REJECTED.requiresConfirmation())
        assertTrue(OrderStatusAction.CLOSE_OTHER.requiresConfirmation())
        assertTrue(!OrderStatusAction.START_DIAGNOSIS.requiresConfirmation())
    }

    @Test
    fun directClosingActionsCarryClosureReason() {
        assertEquals(
            com.onlytechtake.repairapp.data.local.entities.ClosureReason.COMPLETED,
            OrderStatusAction.CLOSE_COMPLETED.closureReason,
        )
        assertEquals(
            com.onlytechtake.repairapp.data.local.entities.ClosureReason.CUSTOMER_DECLINED,
            OrderStatusAction.CLOSE_REJECTED.closureReason,
        )
    }

    @Test
    fun closingRequiresClosureReason() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderStatusPolicy.requireAllowed(OrderStatus.READY_FOR_PICKUP, OrderStatus.CLOSED)
        }
    }
}
