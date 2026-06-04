package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderStatus

object OrderStatusPolicy {
    val closedStatuses: Set<OrderStatus> = setOf(OrderStatus.CLOSED)

    fun allowedTargets(from: OrderStatus): Set<OrderStatus> =
        when (from) {
            OrderStatus.NEW -> setOf(
                OrderStatus.DIAGNOSIS,
                OrderStatus.CLOSED,
            )
            OrderStatus.DIAGNOSIS -> setOf(
                OrderStatus.WAITING_CUSTOMER,
                OrderStatus.WORKING,
                OrderStatus.WAITING_PARTS,
                OrderStatus.CLOSED,
            )
            OrderStatus.WAITING_CUSTOMER -> setOf(
                OrderStatus.WORKING,
                OrderStatus.CLOSED,
            )
            OrderStatus.WORKING -> setOf(
                OrderStatus.WAITING_CUSTOMER,
                OrderStatus.WAITING_PARTS,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.CLOSED,
            )
            OrderStatus.WAITING_PARTS -> setOf(
                OrderStatus.WORKING,
                OrderStatus.WAITING_CUSTOMER,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.CLOSED,
            )
            OrderStatus.READY_FOR_PICKUP -> setOf(
                OrderStatus.CLOSED,
            )
            OrderStatus.CLOSED -> emptySet()
        }

    fun requiresClosureReason(to: OrderStatus): Boolean = to == OrderStatus.CLOSED

    fun requireAllowed(from: OrderStatus, to: OrderStatus, closureReason: ClosureReason? = null) {
        require(to in allowedTargets(from)) {
            "Transizione stato non valida: ${from.name} -> ${to.name}"
        }
        require(!requiresClosureReason(to) || closureReason != null) {
            "Motivo chiusura obbligatorio"
        }
    }
}
