package com.onlytechtake.repairapp.core.utils

import android.content.Context
import androidx.annotation.StringRes
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerActionStatus
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus

val OrderStatus.labelRes: Int
    @StringRes get() =
        when (this) {
            OrderStatus.OPEN -> R.string.status_open
            OrderStatus.IN_PROGRESS -> R.string.status_in_progress
            OrderStatus.DIAGNOSIS -> R.string.status_diagnosis
            OrderStatus.RESULT -> R.string.status_result
            OrderStatus.REPAIRING -> R.string.status_repairing
            OrderStatus.READY_FOR_PICKUP -> R.string.status_ready_for_pickup
            OrderStatus.CLOSED -> R.string.status_closed
        }

fun OrderStatus.label(context: Context): String = context.getString(labelRes)

val ClosureReason.labelRes: Int
    @StringRes get() =
        when (this) {
            ClosureReason.COMPLETED -> R.string.reason_completed
            ClosureReason.CUSTOMER_DECLINED -> R.string.reason_customer_declined
            ClosureReason.NOT_REPAIRABLE -> R.string.reason_not_repairable
            ClosureReason.CANCELLED -> R.string.reason_cancelled
            ClosureReason.OTHER -> R.string.reason_other
        }

fun ClosureReason.label(context: Context): String = context.getString(labelRes)

val CustomerActionStatus.labelRes: Int
    @StringRes get() =
        when (this) {
            CustomerActionStatus.NONE -> R.string.action_none
            CustomerActionStatus.TO_NOTIFY -> R.string.action_to_notify
            CustomerActionStatus.WAITING_ACCEPTANCE -> R.string.action_waiting_acceptance
            CustomerActionStatus.ACCEPTED -> R.string.action_accepted
            CustomerActionStatus.REJECTED -> R.string.action_rejected
            CustomerActionStatus.ASKED_INFO -> R.string.action_asked_info
            CustomerActionStatus.CALL_REQUESTED -> R.string.action_call_requested
            CustomerActionStatus.NOTIFIED -> R.string.action_notified
        }

fun CustomerActionStatus.label(context: Context): String = context.getString(labelRes)

val DiagnosisOutcome.labelRes: Int
    @StringRes get() =
        when (this) {
            DiagnosisOutcome.REPAIRABLE -> R.string.order_diagnosis_repairable
            DiagnosisOutcome.NOT_REPAIRABLE -> R.string.order_diagnosis_not_repairable
        }

fun DiagnosisOutcome.label(context: Context): String = context.getString(labelRes)

val CustomerType.labelRes: Int
    @StringRes get() =
        when (this) {
            CustomerType.PRIVATE -> R.string.customer_type_private
            CustomerType.BUSINESS -> R.string.customer_type_business
        }

fun CustomerType.label(context: Context): String = context.getString(labelRes)

val OrderPriority.labelRes: Int
    @StringRes get() =
        when (this) {
            OrderPriority.LOW -> R.string.priority_low
            OrderPriority.NORMAL -> R.string.priority_normal
            OrderPriority.HIGH -> R.string.priority_high
            OrderPriority.URGENT -> R.string.priority_urgent
        }

fun OrderPriority.label(context: Context): String = context.getString(labelRes)
