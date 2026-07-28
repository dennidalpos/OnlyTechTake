package com.onlytechtake.repairapp.domain.usecases

import com.onlytechtake.repairapp.core.error.DomainException
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.models.DiagnosisValidationError
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.models.validationErrors
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import kotlinx.coroutines.flow.firstOrNull

class RecordDiagnosisResultUseCase(
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(
        actor: AuthenticatedUser,
        orderId: String,
        input: DiagnosisResultInput,
    ): Result<Unit> =
        runCatching {
            val errors = input.validationErrors()
            if (errors.isNotEmpty()) {
                throw DomainException.BusinessRuleViolation(
                    errors.joinToString("\n") { error ->
                        when (error) {
                            DiagnosisValidationError.SUMMARY_REQUIRED -> "Riepilogo richiesto"
                            DiagnosisValidationError.POSITIVE_ESTIMATE_REQUIRED -> "Importo preventivo richiesto"
                        }
                    },
                )
            }

            val orderDetail =
                orderRepository.observeOrderDetail(orderId).firstOrNull()
                    ?: throw DomainException.OrderNotFound(orderId)

            require(orderDetail.order.status == OrderStatus.DIAGNOSIS || orderDetail.order.status == OrderStatus.RESULT) {
                "La diagnosi può essere registrata solo durante o dopo la fase di diagnosi"
            }

            orderRepository.recordResult(actor, orderId, input).getOrThrow()
        }
}
