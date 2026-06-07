package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderStatusPolicy

enum class OrderStatusAction(
    val label: String,
    val targetStatus: OrderStatus?,
    val closureReason: ClosureReason? = null,
) {
    START_PROGRESS("Prendi in carico", OrderStatus.IN_PROGRESS),
    START_DIAGNOSIS("Avvia diagnosi", OrderStatus.DIAGNOSIS),
    RECORD_RESULT("Registra risultato", OrderStatus.RESULT),
    START_REPAIR("Avvia riparazione", OrderStatus.REPAIRING),
    READY_FOR_PICKUP("Pronta al ritiro", OrderStatus.READY_FOR_PICKUP),
    CLOSE_ORDER("Chiudi commessa", OrderStatus.CLOSED),
}

fun availableOrderStatusActions(status: OrderStatus): List<OrderStatusAction> =
    OrderStatusAction.entries.filter { action ->
        action.targetStatus in OrderStatusPolicy.allowedTargets(status)
    }

fun OrderStatusAction.requiresConfirmation(): Boolean =
    targetStatus == OrderStatus.CLOSED

fun statusTemplateCode(status: OrderStatus): String =
    when (status) {
        OrderStatus.OPEN -> "whatsapp_presa_in_carico"
        OrderStatus.IN_PROGRESS -> "whatsapp_lavorazione"
        OrderStatus.DIAGNOSIS -> "whatsapp_diagnosi"
        OrderStatus.RESULT -> "whatsapp_risultato"
        OrderStatus.REPAIRING -> "whatsapp_riparazione"
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.CLOSED -> "whatsapp_pronto_ritiro"
    }

fun OrderStatus.label(): String =
    when (this) {
        OrderStatus.OPEN -> "Aperta"
        OrderStatus.IN_PROGRESS -> "In lavorazione"
        OrderStatus.DIAGNOSIS -> "In diagnosi"
        OrderStatus.RESULT -> "Risultato"
        OrderStatus.REPAIRING -> "In riparazione"
        OrderStatus.READY_FOR_PICKUP -> "Pronta al ritiro"
        OrderStatus.CLOSED -> "Chiusa"
    }

fun ClosureReason.label(): String =
    when (this) {
        ClosureReason.COMPLETED -> "Completata"
        ClosureReason.CUSTOMER_DECLINED -> "Rifiutata dal cliente"
        ClosureReason.NOT_REPAIRABLE -> "Non riparabile"
        ClosureReason.CANCELLED -> "Annullata"
        ClosureReason.OTHER -> "Altro"
    }
