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
    "Da diagnosticare" to setOf(OrderStatus.PRESA_IN_CARICO, OrderStatus.IN_DIAGNOSI),
    "Attesa cliente" to setOf(OrderStatus.PREVENTIVO_INVIATO, OrderStatus.IN_ATTESA_CLIENTE),
    "Da lavorare" to setOf(OrderStatus.PREVENTIVO_ACCETTATO, OrderStatus.IN_LAVORAZIONE, OrderStatus.IN_ATTESA_RICAMBIO),
    "Pronte / chiusura" to setOf(OrderStatus.COMPLETATO, OrderStatus.PRONTO_AL_RITIRO, OrderStatus.CONSEGNATO),
)
