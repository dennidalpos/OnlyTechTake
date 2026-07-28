package com.onlytechtake.repairapp.domain.usecases

import com.onlytechtake.repairapp.core.error.DomainException
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.CustomerFeedbackType
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.FeedbackInput
import com.onlytechtake.repairapp.domain.models.FeedbackValidationError
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.models.validationErrors
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import kotlinx.coroutines.flow.firstOrNull

class RecordCustomerFeedbackUseCase(
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(
        actor: AuthenticatedUser,
        orderId: String,
        input: FeedbackInput,
    ): Result<Unit> =
        runCatching {
            val errors = input.validationErrors()
            if (errors.isNotEmpty()) {
                throw DomainException.BusinessRuleViolation(
                    errors.joinToString("\n") { error ->
                        when (error) {
                            FeedbackValidationError.REJECT_REASON_REQUIRED -> "Motivo del rifiuto richiesto"
                        }
                    },
                )
            }

            val orderDetail =
                orderRepository.observeOrderDetail(orderId).firstOrNull()
                    ?: throw DomainException.OrderNotFound(orderId)

            require(orderDetail.order.status == OrderStatus.RESULT) {
                "Il feedback del cliente richiede una commessa in attesa di risposta"
            }

            if (input.feedback == CustomerFeedbackType.ACCEPT || input.feedback == CustomerFeedbackType.REJECT) {
                require(orderDetail.order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                    "Accettazione o rifiuto sono disponibili solo per diagnosi riparabili"
                }
            }

            orderRepository.recordFeedback(actor, orderId, input).getOrThrow()
        }
}
