package com.onlytechtake.repairapp.features.workspace

import android.content.Context
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.OrderStatusPolicy
import com.onlytechtake.repairapp.domain.workspace.availableOrderStatusActions
import com.onlytechtake.repairapp.domain.workspace.requiresConfirmation
import com.onlytechtake.repairapp.domain.workspace.statusTemplateAnchor
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusWorkflowTest {
    private val context =
        mockk<Context>(relaxed = true) {
            every { getString(any()) } returns "error"
        }

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
            OrderStatusPolicy.requireAllowed(
                context,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.CLOSED,
            )
        }
    }

    @Test
    fun statusMessageTemplateUsesSpecialCases() {
        assertEquals(
            TemplateAnchor.WHATSAPP_QUOTE,
            statusTemplateAnchor(
                order(
                    OrderStatus.RESULT,
                    diagnosisOutcome = DiagnosisOutcome.REPAIRABLE,
                ),
            ),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_NOT_REPAIRABLE,
            statusTemplateAnchor(
                order(
                    OrderStatus.RESULT,
                    diagnosisOutcome = DiagnosisOutcome.NOT_REPAIRABLE,
                ),
            ),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_NOT_REPAIRABLE,
            statusTemplateAnchor(
                order(
                    OrderStatus.CLOSED,
                    closureReason = ClosureReason.NOT_REPAIRABLE,
                ),
            ),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_INTAKE,
            statusTemplateAnchor(order(OrderStatus.IN_PROGRESS)),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_DIAGNOSIS,
            statusTemplateAnchor(order(OrderStatus.DIAGNOSIS)),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_READY,
            statusTemplateAnchor(order(OrderStatus.READY_FOR_PICKUP)),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_COMPLETED,
            statusTemplateAnchor(
                order(
                    OrderStatus.CLOSED,
                    closureReason = ClosureReason.COMPLETED,
                ),
            ),
        )
    }

    @Test
    fun orderStatusStepNumberMapsWorkflowStates() {
        assertEquals(1, orderStatusStepNumber(OrderStatus.OPEN))
        assertEquals(2, orderStatusStepNumber(OrderStatus.IN_PROGRESS))
        assertEquals(3, orderStatusStepNumber(OrderStatus.DIAGNOSIS))
        assertEquals(4, orderStatusStepNumber(OrderStatus.RESULT))
        assertEquals(5, orderStatusStepNumber(OrderStatus.REPAIRING))
        assertEquals(6, orderStatusStepNumber(OrderStatus.READY_FOR_PICKUP))
        assertEquals(7, orderStatusStepNumber(OrderStatus.CLOSED))
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
    )
}
