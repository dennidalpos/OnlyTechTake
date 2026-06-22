package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.core.templates.TemplateAnchor
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

fun OrderStatusAction.requiresConfirmation(): Boolean = targetStatus == OrderStatus.CLOSED

fun statusTemplateAnchor(status: OrderStatus): TemplateAnchor =
    when (status) {
        OrderStatus.OPEN -> TemplateAnchor.WHATSAPP_INTAKE
        OrderStatus.IN_PROGRESS -> TemplateAnchor.WHATSAPP_UPDATE
        OrderStatus.DIAGNOSIS -> TemplateAnchor.WHATSAPP_DIAGNOSIS
        OrderStatus.RESULT -> TemplateAnchor.WHATSAPP_UPDATE
        OrderStatus.REPAIRING -> TemplateAnchor.WHATSAPP_REPAIRING
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.CLOSED,
        -> TemplateAnchor.WHATSAPP_READY
    }

fun statusTemplateAnchor(order: RepairOrderEntity): TemplateAnchor =
    when {
        order.status == OrderStatus.RESULT && order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE ->
            TemplateAnchor.WHATSAPP_QUOTE

        order.status == OrderStatus.RESULT && order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE ->
            TemplateAnchor.WHATSAPP_NOT_REPAIRABLE

        order.status == OrderStatus.CLOSED && order.closureReason == ClosureReason.NOT_REPAIRABLE ->
            TemplateAnchor.WHATSAPP_NOT_REPAIRABLE

        else -> statusTemplateAnchor(order.status)
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

fun shouldShowDiagnosisEstimate(outcome: DiagnosisOutcome?): Boolean = outcome == DiagnosisOutcome.REPAIRABLE

fun canConfirmDiagnosisResult(
    summary: String,
    outcome: DiagnosisOutcome?,
    estimateAmountCents: Long?,
): Boolean =
    summary.isNotBlank() &&
        outcome != null &&
        (outcome != DiagnosisOutcome.REPAIRABLE || (estimateAmountCents != null && estimateAmountCents > 0L))
