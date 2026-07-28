package com.onlytechtake.repairapp.features.workspace

import android.content.Context
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.domain.logic.OrderStatusAction
import com.onlytechtake.repairapp.domain.logic.OrderStatusPolicy
import com.onlytechtake.repairapp.domain.logic.availableOrderStatusActions
import com.onlytechtake.repairapp.domain.logic.requiresConfirmation
import com.onlytechtake.repairapp.domain.logic.statusTemplateAnchor
import com.onlytechtake.repairapp.domain.models.ClosureReason
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.OrderPriority
import com.onlytechtake.repairapp.domain.models.OrderStatus
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
                ).toDomain(),
            ),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_NOT_REPAIRABLE,
            statusTemplateAnchor(
                order(
                    OrderStatus.RESULT,
                    diagnosisOutcome = DiagnosisOutcome.NOT_REPAIRABLE,
                ).toDomain(),
            ),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_NOT_REPAIRABLE,
            statusTemplateAnchor(
                order(
                    OrderStatus.CLOSED,
                    closureReason = ClosureReason.NOT_REPAIRABLE,
                ).toDomain(),
            ),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_INTAKE,
            statusTemplateAnchor(order(OrderStatus.OPEN).toDomain()),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_UPDATE,
            statusTemplateAnchor(order(OrderStatus.IN_PROGRESS).toDomain()),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_DIAGNOSIS,
            statusTemplateAnchor(order(OrderStatus.DIAGNOSIS).toDomain()),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_READY,
            statusTemplateAnchor(order(OrderStatus.READY_FOR_PICKUP).toDomain()),
        )
        assertEquals(
            TemplateAnchor.WHATSAPP_COMPLETED,
            statusTemplateAnchor(
                order(
                    OrderStatus.CLOSED,
                    closureReason = ClosureReason.COMPLETED,
                ).toDomain(),
            ),
        )
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
