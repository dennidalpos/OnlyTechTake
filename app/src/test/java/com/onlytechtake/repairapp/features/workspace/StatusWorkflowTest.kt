package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusWorkflowTest {
    @Test
    fun presaInCaricoStartsWithDiagnosisOrCancel() {
        assertEquals(
            listOf(OrderStatusAction.START_DIAGNOSIS, OrderStatusAction.CANCEL),
            availableOrderStatusActions(OrderStatus.PRESA_IN_CARICO),
        )
    }

    @Test
    fun waitingCustomerCanRecordEstimateAnswer() {
        val actions = availableOrderStatusActions(OrderStatus.IN_ATTESA_CLIENTE)

        assertTrue(actions.contains(OrderStatusAction.ACCEPT_ESTIMATE))
        assertTrue(actions.contains(OrderStatusAction.REJECT_ESTIMATE))
    }

    @Test
    fun closedStatusesHaveNoOperatorActions() {
        assertTrue(availableOrderStatusActions(OrderStatus.CHIUSO).isEmpty())
        assertTrue(availableOrderStatusActions(OrderStatus.ANNULLATO).isEmpty())
    }

    @Test
    fun onlyRiskyStatusActionsRequireConfirmation() {
        assertTrue(OrderStatusAction.CLOSE.requiresConfirmation())
        assertTrue(OrderStatusAction.CANCEL.requiresConfirmation())
        assertTrue(!OrderStatusAction.START_DIAGNOSIS.requiresConfirmation())
    }
}
