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
        val queues = buildOperationalQueues(
            listOf(
                order("1", OrderStatus.PRESA_IN_CARICO),
                order("2", OrderStatus.IN_ATTESA_CLIENTE),
                order("3", OrderStatus.IN_LAVORAZIONE),
                order("4", OrderStatus.PRONTO_AL_RITIRO),
            ),
        )

        assertEquals(listOf(1, 1, 1, 1), queues.map { it.orders.size })
        assertTrue(queueFilter(queues.first()).statuses.contains(OrderStatus.IN_DIAGNOSI))
    }

    private fun order(id: String, status: OrderStatus) = RepairOrderEntity(
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
        deletedAt = null,
        deletedByUserId = null,
        deleteReason = null,
    )
}
