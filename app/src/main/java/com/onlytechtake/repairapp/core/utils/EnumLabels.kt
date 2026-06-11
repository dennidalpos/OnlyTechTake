package com.onlytechtake.repairapp.core.utils

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerActionStatus
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus

fun OrderStatus.label(context: Context): String =
    when (this) {
        OrderStatus.OPEN -> context.getString(R.string.status_open)
        OrderStatus.IN_PROGRESS -> context.getString(R.string.status_in_progress)
        OrderStatus.DIAGNOSIS -> context.getString(R.string.status_diagnosis)
        OrderStatus.RESULT -> context.getString(R.string.status_result)
        OrderStatus.REPAIRING -> context.getString(R.string.status_repairing)
        OrderStatus.READY_FOR_PICKUP -> context.getString(R.string.status_ready_for_pickup)
        OrderStatus.CLOSED -> context.getString(R.string.status_closed)
    }

fun ClosureReason.label(context: Context): String =
    when (this) {
        ClosureReason.COMPLETED -> context.getString(R.string.reason_completed)
        ClosureReason.CUSTOMER_DECLINED -> context.getString(R.string.reason_customer_declined)
        ClosureReason.NOT_REPAIRABLE -> context.getString(R.string.reason_not_repairable)
        ClosureReason.CANCELLED -> context.getString(R.string.reason_cancelled)
        ClosureReason.OTHER -> context.getString(R.string.reason_other)
    }

fun CustomerActionStatus.label(context: Context): String =
    when (this) {
        CustomerActionStatus.NONE -> context.getString(R.string.action_none)
        CustomerActionStatus.TO_NOTIFY -> context.getString(R.string.action_to_notify)
        CustomerActionStatus.WAITING_ACCEPTANCE -> context.getString(R.string.action_waiting_acceptance)
        CustomerActionStatus.ACCEPTED -> context.getString(R.string.action_accepted)
        CustomerActionStatus.REJECTED -> context.getString(R.string.action_rejected)
        CustomerActionStatus.ASKED_INFO -> context.getString(R.string.action_asked_info)
        CustomerActionStatus.CALL_REQUESTED -> context.getString(R.string.action_call_requested)
        CustomerActionStatus.NOTIFIED -> context.getString(R.string.action_notified)
    }

fun DiagnosisOutcome.label(context: Context): String =
    when (this) {
        DiagnosisOutcome.REPAIRABLE -> context.getString(R.string.order_diagnosis_repairable)
        DiagnosisOutcome.NOT_REPAIRABLE -> context.getString(R.string.order_diagnosis_not_repairable)
    }

fun CustomerType.label(context: Context): String =
    when (this) {
        CustomerType.PRIVATE -> context.getString(R.string.customer_type_private)
        CustomerType.BUSINESS -> context.getString(R.string.customer_type_business)
    }
