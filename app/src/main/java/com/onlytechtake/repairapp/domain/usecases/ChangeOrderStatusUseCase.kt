package com.onlytechtake.repairapp.domain.usecases

import com.onlytechtake.repairapp.core.error.DomainException
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.logic.OrderClosurePermissionPolicy
import com.onlytechtake.repairapp.domain.models.ClosureReason
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import kotlinx.coroutines.flow.firstOrNull

class ChangeOrderStatusUseCase(
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(
        actor: AuthenticatedUser,
        orderId: String,
        status: OrderStatus,
        note: String = "",
        closureReason: ClosureReason? = null,
    ): Result<Unit> =
        runCatching {
            val orderDetail =
                orderRepository.observeOrderDetail(orderId).firstOrNull()
                    ?: throw DomainException.OrderNotFound(orderId)

            val currentStatus = orderDetail.order.status
            if (currentStatus == status) return@runCatching

            // Business Rules
            OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(actor, status)

            if ((currentStatus == OrderStatus.RESULT) && (status == OrderStatus.REPAIRING)) {
                if (orderDetail.order.diagnosisOutcome != DiagnosisOutcome.REPAIRABLE) {
                    throw DomainException.BusinessRuleViolation("Solo una diagnosi riparabile può passare in lavorazione")
                }
            }
            if ((currentStatus == OrderStatus.RESULT) && (status == OrderStatus.READY_FOR_PICKUP)) {
                if (orderDetail.order.diagnosisOutcome != DiagnosisOutcome.NOT_REPAIRABLE) {
                    throw DomainException.BusinessRuleViolation("Il passaggio diretto alla consegna richiede una diagnosi non riparabile")
                }
            }

            orderRepository.changeStatus(actor, orderId, status, note, closureReason).getOrThrow()
        }
}
