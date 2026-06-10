package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
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

    @Test
    fun statusMessageTemplateUsesSpecialCases() {
        assertEquals(
            "whatsapp_preventivo_intervento",
            statusTemplateCode(order(OrderStatus.RESULT, diagnosisOutcome = DiagnosisOutcome.REPAIRABLE)),
        )
        assertEquals(
            "whatsapp_non_riparabile",
            statusTemplateCode(order(OrderStatus.RESULT, diagnosisOutcome = DiagnosisOutcome.NOT_REPAIRABLE)),
        )
        assertEquals("whatsapp_non_riparabile", statusTemplateCode(order(OrderStatus.CLOSED, closureReason = ClosureReason.NOT_REPAIRABLE)))
        assertEquals("whatsapp_diagnosi", statusTemplateCode(order(OrderStatus.DIAGNOSIS)))
    }

    private fun order(
        status: OrderStatus,
        diagnosisOutcome: DiagnosisOutcome? = null,
        closureReason: ClosureReason? = null,
    ) = RepairOrderEntity(
        id = "order",
        orderNumber = "2026-0001",
        customerId = "customer",
        title = "Oggetto",
        categoryId = "category",
        status = status,
        priority = OrderPriority.NORMAL,
        assignedToUserId = "operator",
        createdByUserId = "operator",
        description = null,
        internalNotes = null,
        diagnosisOutcome = diagnosisOutcome,
        estimateAmountCents = if (diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) 12_300 else null,
        finalAmountCents = null,
        createdAt = 0L,
        updatedAt = 0L,
        closedAt = null,
        closureReason = closureReason,
        deletedAt = null,
        deletedByUserId = null,
        deleteReason = null,
    )
}
