package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderStatus

object OrderStatusPolicy {
    val closedStatuses: Set<OrderStatus> = setOf(OrderStatus.CLOSED)

    fun allowedTargets(from: OrderStatus): Set<OrderStatus> =
        when (from) {
            OrderStatus.OPEN -> {
                setOf(
                    OrderStatus.IN_PROGRESS,
                    OrderStatus.CLOSED,
                )
            }

            OrderStatus.IN_PROGRESS -> {
                setOf(
                    OrderStatus.DIAGNOSIS,
                    OrderStatus.CLOSED,
                )
            }

            OrderStatus.DIAGNOSIS -> {
                setOf(
                    OrderStatus.RESULT,
                    OrderStatus.CLOSED,
                )
            }

            OrderStatus.RESULT -> {
                setOf(
                    OrderStatus.REPAIRING,
                    OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.CLOSED,
                )
            }

            OrderStatus.REPAIRING -> {
                setOf(
                    OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.CLOSED,
                )
            }

            OrderStatus.READY_FOR_PICKUP -> {
                setOf(
                    OrderStatus.CLOSED,
                )
            }

            OrderStatus.CLOSED -> {
                emptySet()
            }
        }

    private fun requiresClosureReason(to: OrderStatus): Boolean = to == OrderStatus.CLOSED

    fun requireAllowed(
        context: Context,
        from: OrderStatus,
        to: OrderStatus,
        closureReason: ClosureReason? = null,
    ) {
        require(to in allowedTargets(from)) {
            "Invalid status transition: ${from.name} -> ${to.name}"
        }
        require(!requiresClosureReason(to) || (closureReason != null)) {
            context.getString(R.string.error_closure_reason_required)
        }
    }
}
