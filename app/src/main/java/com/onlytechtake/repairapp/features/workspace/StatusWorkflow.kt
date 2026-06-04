package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderStatusPolicy

enum class OrderStatusAction(
    val label: String,
    val targetStatus: OrderStatus?,
) {
    START_DIAGNOSIS("Avvia diagnosi", OrderStatus.DIAGNOSIS),
    WAIT_CUSTOMER("Attesa cliente", OrderStatus.WAITING_CUSTOMER),
    START_WORK("Avvia lavorazione", OrderStatus.WORKING),
    WAIT_PARTS("Attesa ricambi", OrderStatus.WAITING_PARTS),
    READY_FOR_PICKUP("Pronta al ritiro", OrderStatus.READY_FOR_PICKUP),
    CLOSE("Chiudi", OrderStatus.CLOSED),
}

fun availableOrderStatusActions(status: OrderStatus): List<OrderStatusAction> =
    OrderStatusAction.entries.filter { action ->
        action.targetStatus in OrderStatusPolicy.allowedTargets(status)
    }

fun OrderStatusAction.requiresConfirmation(): Boolean =
    targetStatus == OrderStatus.CLOSED

fun statusTemplateCode(status: OrderStatus): String =
    when (status) {
        OrderStatus.NEW -> "whatsapp_presa_in_carico"
        OrderStatus.DIAGNOSIS -> "whatsapp_diagnosi"
        OrderStatus.WAITING_CUSTOMER -> "whatsapp_preventivo"
        OrderStatus.WORKING,
        OrderStatus.WAITING_PARTS -> "whatsapp_lavorazione"
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.CLOSED -> "whatsapp_pronto_ritiro"
    }

fun OrderStatus.label(): String =
    when (this) {
        OrderStatus.NEW -> "Nuova"
        OrderStatus.DIAGNOSIS -> "In diagnosi"
        OrderStatus.WAITING_CUSTOMER -> "In attesa cliente"
        OrderStatus.WORKING -> "In lavorazione"
        OrderStatus.WAITING_PARTS -> "In attesa ricambi"
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
