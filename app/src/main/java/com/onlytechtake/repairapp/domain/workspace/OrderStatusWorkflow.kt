package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import androidx.annotation.StringRes
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity

enum class OrderStatusAction(
    @param:StringRes val labelRes: Int,
    val targetStatus: OrderStatus?,
    val closureReason: ClosureReason? = null,
) {
    START_PROGRESS(R.string.action_start_progress, OrderStatus.IN_PROGRESS),
    START_DIAGNOSIS(R.string.action_start_diagnosis, OrderStatus.DIAGNOSIS),
    RECORD_RESULT(R.string.action_record_result, OrderStatus.RESULT),
    START_REPAIR(R.string.action_start_repair, OrderStatus.REPAIRING),
    READY_FOR_PICKUP(R.string.action_ready_for_pickup, OrderStatus.READY_FOR_PICKUP),
    CLOSE_ORDER(R.string.action_close_order, OrderStatus.CLOSED),
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
    when (order.status) {
        OrderStatus.RESULT -> {
            if (order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                TemplateAnchor.WHATSAPP_QUOTE
            } else {
                TemplateAnchor.WHATSAPP_NOT_REPAIRABLE
            }
        }

        OrderStatus.CLOSED -> {
            if (order.closureReason == ClosureReason.NOT_REPAIRABLE) {
                TemplateAnchor.WHATSAPP_NOT_REPAIRABLE
            } else {
                statusTemplateAnchor(order.status)
            }
        }

        else -> {
            statusTemplateAnchor(order.status)
        }
    }

fun nextActionLabel(
    context: Context,
    status: OrderStatus,
): String =
    if (status == OrderStatus.RESULT) {
        context.getString(R.string.action_record_quote_outcome)
    } else {
        availableOrderStatusActions(status).firstOrNull()?.let {
            context.getString(it.labelRes)
        } ?: context.getString(R.string.action_none_label)
    }

fun nextActionLabel(
    context: Context,
    order: RepairOrderEntity,
): String =
    when (order.status) {
        OrderStatus.RESULT -> {
            if (order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                context.getString(R.string.action_wait_quote)
            } else {
                context.getString(R.string.action_notify_not_repairable)
            }
        }

        else -> {
            nextActionLabel(context, order.status)
        }
    }

fun shouldShowDiagnosisEstimate(outcome: DiagnosisOutcome?): Boolean = outcome == DiagnosisOutcome.REPAIRABLE

fun canConfirmDiagnosisResult(
    summary: String,
    outcome: DiagnosisOutcome?,
    estimateAmountCents: Long?,
): Boolean =
    (summary.isNotBlank()) &&
        (outcome != null) &&
        ((outcome != DiagnosisOutcome.REPAIRABLE) || ((estimateAmountCents != null) && (estimateAmountCents > 0L)))
