package com.onlytechtake.repairapp.features.workspace

import androidx.annotation.StringRes
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.domain.models.Order
import com.onlytechtake.repairapp.domain.models.OrderFilter
import com.onlytechtake.repairapp.domain.models.OrderStatus

data class OperationalQueue(
    @get:StringRes val titleRes: Int,
    val statuses: Set<OrderStatus>,
    val orders: List<Order>,
)

val queueDefinitions: List<Pair<Int, Set<OrderStatus>>> =
    listOf(
        R.string.queue_open to setOf(OrderStatus.OPEN),
        R.string.queue_processing to setOf(OrderStatus.IN_PROGRESS),
        R.string.queue_diagnosis to setOf(OrderStatus.DIAGNOSIS),
        R.string.queue_result to setOf(OrderStatus.RESULT),
        R.string.queue_repair to setOf(OrderStatus.REPAIRING),
        R.string.queue_ready to setOf(OrderStatus.READY_FOR_PICKUP),
        R.string.queue_closed to setOf(OrderStatus.CLOSED),
    )

fun buildOperationalQueues(orders: List<Order>): List<OperationalQueue> {
    val groupedOrders = orders.groupBy { it.status }
    return queueDefinitions.map { (titleRes, statuses) ->
        OperationalQueue(
            titleRes = titleRes,
            statuses = statuses,
            orders =
                statuses
                    .asSequence()
                    .flatMap { groupedOrders[it] ?: emptyList() }
                    .sortedByDescending { it.updatedAt }
                    .toList(),
        )
    }
}

fun queueFilter(queue: OperationalQueue): OrderFilter = OrderFilter(statuses = queue.statuses)
