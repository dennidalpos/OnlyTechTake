package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationalQueuesTest {
    @Test
    fun queuesGroupOrdersByOperationalState() {
        val queues =
            buildOperationalQueues(
                listOf(
                    order("1", OrderStatus.OPEN),
                    order("2", OrderStatus.IN_PROGRESS),
                    order("3", OrderStatus.RESULT),
                    order("5", OrderStatus.REPAIRING),
                    order("4", OrderStatus.READY_FOR_PICKUP),
                ),
            )

        assertEquals(listOf(1, 1, 2, 1), queues.map { it.orders.size })
        assertTrue(queueFilter(queues[1]).statuses.contains(OrderStatus.DIAGNOSIS))
    }

    private fun order(
        id: String,
        status: OrderStatus,
    ) = RepairOrderEntity(
        id = id,
        orderNumber = "2026-$id",
        customerId = "customer",
        title = "Commessa $id",
        categoryId = "category",
        status = status,
        priority = OrderPriority.NORMAL,
        assignedToUserId = "operator",
        createdByUserId = "admin",
        description = null,
        internalNotes = null,
        estimateAmountCents = null,
        finalAmountCents = null,
        createdAt = 0L,
        updatedAt = 0L,
        closedAt = null,
        closureReason = null,
    )
}
