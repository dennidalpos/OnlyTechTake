package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.domain.workspace.OrderFilter

data class OperationalQueue(
    val title: String,
    val statuses: Set<OrderStatus>,
    val orders: List<RepairOrderEntity>,
)

fun buildOperationalQueues(orders: List<RepairOrderEntity>): List<OperationalQueue> =
    queueDefinitions.map { (title, statuses) ->
        OperationalQueue(
            title = title,
            statuses = statuses,
            orders = orders.filter { it.status in statuses },
        )
    }

fun queueFilter(queue: OperationalQueue): OrderFilter =
    OrderFilter(statuses = queue.statuses)

private val queueDefinitions = listOf(
    "Da diagnosticare" to setOf(OrderStatus.NEW, OrderStatus.DIAGNOSIS),
    "Attesa cliente" to setOf(OrderStatus.WAITING_CUSTOMER),
    "Da lavorare" to setOf(OrderStatus.WORKING, OrderStatus.WAITING_PARTS),
    "Pronte / chiusura" to setOf(OrderStatus.READY_FOR_PICKUP),
)
