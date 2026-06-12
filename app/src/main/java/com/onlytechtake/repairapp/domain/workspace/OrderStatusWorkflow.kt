package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity

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

fun statusTemplateCode(order: RepairOrderEntity): String =
    when {
        order.status == OrderStatus.RESULT && order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE ->
            "whatsapp_preventivo_intervento"
        order.status == OrderStatus.RESULT && order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE ->
            "whatsapp_non_riparabile"
        order.status == OrderStatus.CLOSED && order.closureReason == ClosureReason.NOT_REPAIRABLE ->
            "whatsapp_non_riparabile"
        else -> statusTemplateCode(order.status)
    }

fun nextActionLabel(status: OrderStatus): String =
    if (status == OrderStatus.RESULT) {
        "Registra esito preventivo"
    } else {
        availableOrderStatusActions(status).firstOrNull()?.label ?: "Nessuna azione"
    }

fun nextActionLabel(order: RepairOrderEntity): String =
    when {
        order.status == OrderStatus.RESULT && order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE ->
            "Attendere accettazione o rifiuto preventivo"
        order.status == OrderStatus.RESULT && order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE ->
            "Avvisare il cliente e preparare il ritiro"
        else -> nextActionLabel(order.status)
    }

fun shouldShowDiagnosisEstimate(outcome: DiagnosisOutcome?): Boolean =
    outcome == DiagnosisOutcome.REPAIRABLE

fun canConfirmDiagnosisResult(
    summary: String,
    outcome: DiagnosisOutcome?,
    estimateAmountCents: Long?,
): Boolean =
    summary.isNotBlank() &&
        outcome != null &&
        (outcome != DiagnosisOutcome.REPAIRABLE || (estimateAmountCents != null && estimateAmountCents > 0L))
