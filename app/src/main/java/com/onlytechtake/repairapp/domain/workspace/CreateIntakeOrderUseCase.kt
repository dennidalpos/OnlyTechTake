package com.onlytechtake.repairapp.domain.workspace

import android.graphics.Bitmap
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderCreationResult.Complete

class CreateIntakeOrderUseCase(
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderWorkflowRepository,
    private val orderMediaRepository: OrderMediaRepository,
    private val signatureRepository: SignatureRepository,
    private val runAtomically: suspend (suspend () -> IntakeOrderCreationResult) -> IntakeOrderCreationResult = { block -> block() },
) {
    suspend fun execute(
        actor: AuthenticatedUser,
        input: IntakeOrderInput,
        stagedMedia: List<StagedOrderMedia>,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        signatureBitmap: Bitmap,
    ): Result<IntakeOrderCreationResult> =
        runCatching {
            val normalizedInput = input.copy(
                stagedMediaCount = stagedMedia.size,
                hasPrivacySignature = signerName.isNotBlank() && privacyAccepted && termsAccepted,
            )
            val errors = normalizedInput.validationErrors()
            require(errors.isEmpty()) { errors.joinToString("\n") }

            runAtomically {
                val customerId = normalizedInput.existingCustomerId?.takeIf { it.isNotBlank() }
                    ?: customerRepository.save(requireNotNull(normalizedInput.newCustomer) { "Cliente obbligatorio" })
                        .getOrThrow()
                        .id

                val order = orderRepository.createOrder(
                    actor = actor,
                    input = RepairOrderInput(
                        customerId = customerId,
                        title = normalizedInput.problemSummary,
                        categoryId = requireNotNull(normalizedInput.categoryId) { "Categoria obbligatoria" },
                        description = normalizedInput.problemDetails.trim(),
                        assignedToUserId = normalizedInput.assignedToUserId,
                    ),
                ).getOrThrow()

                signatureRepository.saveSignature(
                    actor = actor,
                    orderId = order.id,
                    signerName = signerName,
                    privacyAccepted = privacyAccepted,
                    termsAccepted = termsAccepted,
                    bitmap = signatureBitmap,
                ).getOrThrow()

                orderMediaRepository.attachStagedMedia(actor, order.id, stagedMedia).getOrThrow()

                Complete(order)
            }
        }
}
