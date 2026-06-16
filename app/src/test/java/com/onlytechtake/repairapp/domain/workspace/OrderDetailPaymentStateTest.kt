package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderDetailPaymentStateTest {
    @Test
    fun isPaidUsesLatestPaymentEvent() {
        val detail = orderDetail(
            event(TimelineEventType.PAYMENT_RECEIVED, createdAt = 100L),
            event(TimelineEventType.PAYMENT_REMOVED, createdAt = 200L),
        )

        assertFalse(detail.isPaid)
    }

    @Test
    fun isPaidIgnoresNonPaymentEventsAfterPayment() {
        val detail = orderDetail(
            event(TimelineEventType.PAYMENT_RECEIVED, createdAt = 100L),
            event(TimelineEventType.STATUS_CHANGED, createdAt = 300L),
        )

        assertTrue(detail.isPaid)
    }

    @Test
    fun isPaidIsFalseWithoutPaymentEvents() {
        val detail = orderDetail(event(TimelineEventType.STATUS_CHANGED, createdAt = 100L))

        assertFalse(detail.isPaid)
    }

    private fun orderDetail(vararg timeline: TimelineEventEntity): OrderDetail =
        OrderDetail(
            order = RepairOrderEntity(
                id = "order-1",
                orderNumber = "2026-0001",
                customerId = "customer-1",
                title = "Display rotto",
                categoryId = "phone",
                status = OrderStatus.OPEN,
                priority = OrderPriority.NORMAL,
                assignedToUserId = "operator-1",
                createdByUserId = "operator-1",
                description = null,
                internalNotes = null,
                estimateAmountCents = null,
                finalAmountCents = null,
                createdAt = 0L,
                updatedAt = 0L,
                closedAt = null,
                closureReason = null,
            ),
            customer = null,
            category = null,
            assignedUser = null,
            timeline = timeline.toList(),
            messages = emptyList(),
            media = emptyList(),
            signatures = emptyList(),
        )

    private fun event(type: TimelineEventType, createdAt: Long): TimelineEventEntity =
        TimelineEventEntity(
            id = "$type-$createdAt",
            orderId = "order-1",
            type = type,
            title = type.name,
            description = null,
            oldStatus = null,
            newStatus = null,
            visibleToCustomer = false,
            requiresCustomerFeedback = false,
            expectedFeedbackType = null,
            customerFeedback = null,
            customerFeedbackNote = null,
            customerFeedbackAt = null,
            createdByUserId = "operator-1",
            createdAt = createdAt,
        )
}
