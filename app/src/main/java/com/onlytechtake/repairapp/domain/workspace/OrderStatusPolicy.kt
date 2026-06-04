package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus

object OrderStatusPolicy {
    val closedStatuses: Set<OrderStatus> = setOf(
        OrderStatus.CHIUSO,
        OrderStatus.CONSEGNATO,
        OrderStatus.ANNULLATO,
    )

    fun allowedTargets(from: OrderStatus): Set<OrderStatus> =
        when (from) {
            OrderStatus.PRESA_IN_CARICO -> setOf(
                OrderStatus.IN_DIAGNOSI,
                OrderStatus.ANNULLATO,
            )
            OrderStatus.IN_DIAGNOSI -> setOf(
                OrderStatus.PREVENTIVO_INVIATO,
                OrderStatus.IN_LAVORAZIONE,
                OrderStatus.NON_COMPLETABILE,
            )
            OrderStatus.PREVENTIVO_INVIATO,
            OrderStatus.IN_ATTESA_CLIENTE -> setOf(
                OrderStatus.PREVENTIVO_ACCETTATO,
                OrderStatus.PREVENTIVO_RIFIUTATO,
                OrderStatus.IN_ATTESA_CLIENTE,
            )
            OrderStatus.PREVENTIVO_ACCETTATO -> setOf(
                OrderStatus.IN_LAVORAZIONE,
                OrderStatus.IN_ATTESA_RICAMBIO,
            )
            OrderStatus.PREVENTIVO_RIFIUTATO,
            OrderStatus.NON_COMPLETABILE -> setOf(
                OrderStatus.CHIUSO,
                OrderStatus.ANNULLATO,
            )
            OrderStatus.IN_LAVORAZIONE -> setOf(
                OrderStatus.IN_ATTESA_RICAMBIO,
                OrderStatus.COMPLETATO,
                OrderStatus.NON_COMPLETABILE,
            )
            OrderStatus.IN_ATTESA_RICAMBIO -> setOf(
                OrderStatus.IN_LAVORAZIONE,
                OrderStatus.COMPLETATO,
                OrderStatus.NON_COMPLETABILE,
            )
            OrderStatus.COMPLETATO -> setOf(
                OrderStatus.PRONTO_AL_RITIRO,
                OrderStatus.CHIUSO,
            )
            OrderStatus.PRONTO_AL_RITIRO -> setOf(
                OrderStatus.CONSEGNATO,
                OrderStatus.CHIUSO,
            )
            OrderStatus.CONSEGNATO -> setOf(OrderStatus.CHIUSO)
            OrderStatus.CHIUSO,
            OrderStatus.ANNULLATO -> emptySet()
        }

    fun requireAllowed(from: OrderStatus, to: OrderStatus) {
        require(to in allowedTargets(from)) {
            "Transizione stato non valida: ${from.name} -> ${to.name}"
        }
    }
}
