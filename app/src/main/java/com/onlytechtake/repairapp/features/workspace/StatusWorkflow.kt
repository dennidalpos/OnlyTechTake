package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderStatusPolicy

enum class OrderStatusAction(
    val label: String,
    val targetStatus: OrderStatus?,
) {
    START_DIAGNOSIS("Avvia diagnosi", OrderStatus.IN_DIAGNOSI),
    SEND_ESTIMATE("Preventivo inviato", OrderStatus.PREVENTIVO_INVIATO),
    WAIT_CUSTOMER("Attesa cliente", OrderStatus.IN_ATTESA_CLIENTE),
    ACCEPT_ESTIMATE("Preventivo accettato", OrderStatus.PREVENTIVO_ACCETTATO),
    REJECT_ESTIMATE("Preventivo rifiutato", OrderStatus.PREVENTIVO_RIFIUTATO),
    START_WORK("Avvia lavorazione", OrderStatus.IN_LAVORAZIONE),
    WAIT_PARTS("Attesa ricambio", OrderStatus.IN_ATTESA_RICAMBIO),
    COMPLETE("Completa", OrderStatus.COMPLETATO),
    NOT_COMPLETABLE("Non completabile", OrderStatus.NON_COMPLETABILE),
    READY_FOR_PICKUP("Pronta al ritiro", OrderStatus.PRONTO_AL_RITIRO),
    DELIVER("Consegnata", OrderStatus.CONSEGNATO),
    CLOSE("Chiudi", OrderStatus.CHIUSO),
    CANCEL("Annulla", OrderStatus.ANNULLATO),
}

fun availableOrderStatusActions(status: OrderStatus): List<OrderStatusAction> =
    OrderStatusAction.entries.filter { action ->
        action.targetStatus in OrderStatusPolicy.allowedTargets(status)
    }

fun OrderStatusAction.requiresConfirmation(): Boolean =
    this in setOf(
        OrderStatusAction.CLOSE,
        OrderStatusAction.CANCEL,
    )

fun statusTemplateCode(status: OrderStatus): String =
    when (status) {
        OrderStatus.PRESA_IN_CARICO -> "whatsapp_presa_in_carico"
        OrderStatus.IN_DIAGNOSI -> "whatsapp_diagnosi"
        OrderStatus.PREVENTIVO_INVIATO,
        OrderStatus.IN_ATTESA_CLIENTE -> "whatsapp_preventivo"
        OrderStatus.PREVENTIVO_ACCETTATO,
        OrderStatus.PREVENTIVO_RIFIUTATO -> "whatsapp_accettazione"
        OrderStatus.IN_LAVORAZIONE,
        OrderStatus.IN_ATTESA_RICAMBIO -> "whatsapp_lavorazione"
        OrderStatus.COMPLETATO -> "whatsapp_completamento"
        OrderStatus.NON_COMPLETABILE,
        OrderStatus.PRONTO_AL_RITIRO,
        OrderStatus.CONSEGNATO,
        OrderStatus.CHIUSO,
        OrderStatus.ANNULLATO -> "whatsapp_pronto_ritiro"
    }

fun OrderStatus.label(): String =
    when (this) {
        OrderStatus.PRESA_IN_CARICO -> "Presa in carico"
        OrderStatus.IN_DIAGNOSI -> "In diagnosi"
        OrderStatus.PREVENTIVO_INVIATO -> "Preventivo inviato"
        OrderStatus.IN_ATTESA_CLIENTE -> "In attesa cliente"
        OrderStatus.PREVENTIVO_ACCETTATO -> "Preventivo accettato"
        OrderStatus.PREVENTIVO_RIFIUTATO -> "Preventivo rifiutato"
        OrderStatus.IN_LAVORAZIONE -> "In lavorazione"
        OrderStatus.IN_ATTESA_RICAMBIO -> "In attesa ricambio"
        OrderStatus.COMPLETATO -> "Completato"
        OrderStatus.NON_COMPLETABILE -> "Non completabile"
        OrderStatus.PRONTO_AL_RITIRO -> "Pronto al ritiro"
        OrderStatus.CONSEGNATO -> "Consegnato"
        OrderStatus.CHIUSO -> "Chiuso"
        OrderStatus.ANNULLATO -> "Annullato"
    }
