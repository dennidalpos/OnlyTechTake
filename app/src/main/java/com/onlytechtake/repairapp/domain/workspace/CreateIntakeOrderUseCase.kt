package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import android.graphics.Bitmap
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderCreationResult.Complete

class CreateIntakeOrderUseCase(
    private val context: Context,
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderWorkflowRepository,
    private val orderMediaRepository: OrderMediaRepository,
    private val signatureRepository: SignatureRepository,
    private val runAtomically: suspend (suspend () -> IntakeOrderCreationResult) -> IntakeOrderCreationResult = { block -> block() },
) {
    suspend fun countAllOrders(): Int = orderRepository.countAllOrders()

    fun getDemoLimitErrorMessage(): String =
        context.resources.getQuantityString(
            R.plurals.demo_error_order_limit,
            BuildConfig.MAX_ORDERS,
            BuildConfig.MAX_ORDERS,
        )

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
            val normalizedInput =
                input.copy(
                    stagedMediaCount = stagedMedia.size,
                    hasPrivacySignature = privacyAccepted && termsAccepted,
                )
            val errors = normalizedInput.validationErrors()
            require(errors.isEmpty()) { errors.joinToString("\n") }

            if (BuildConfig.IS_DEMO_VERSION && BuildConfig.MAX_ORDERS > 0) {
                val totalOrders = orderRepository.countAllOrders()
                if (totalOrders >= BuildConfig.MAX_ORDERS) {
                    throw IllegalStateException(
                        context.resources.getQuantityString(
                            R.plurals.demo_error_order_limit,
                            BuildConfig.MAX_ORDERS,
                            BuildConfig.MAX_ORDERS,
                        ),
                    )
                }
            }

            var savedSignature: SignatureEntity? = null
            var attachedMedia: List<OrderMediaEntity> = emptyList()

            try {
                runAtomically {
                    val customerId =
                        normalizedInput.existingCustomerId?.takeIf { it.isNotBlank() }
                            ?: customerRepository
                                .save(
                                    actor,
                                    requireNotNull(normalizedInput.newCustomer) { "Cliente obbligatorio" },
                                ).getOrThrow()
                                .id

                    val order =
                        orderRepository
                            .createOrder(
                                actor = actor,
                                input =
                                    RepairOrderInput(
                                        customerId = customerId,
                                        title = normalizedInput.problemSummary,
                                        categoryId = requireNotNull(normalizedInput.categoryId) { "Categoria obbligatoria" },
                                        description = normalizedInput.problemDetails.trim(),
                                        assignedToUserId = normalizedInput.assignedToUserId,
                                    ),
                            ).getOrThrow()

                    signatureRepository
                        .saveSignature(
                            actor = actor,
                            orderId = order.id,
                            signerName = signerName,
                            privacyAccepted = privacyAccepted,
                            termsAccepted = termsAccepted,
                            bitmap = signatureBitmap,
                        ).getOrThrow()
                        .also { savedSignature = it }

                    attachedMedia =
                        orderMediaRepository
                            .attachStagedMedia(actor, order.id, stagedMedia)
                            .getOrThrow()

                    Complete(order)
                }
            } catch (error: Throwable) {
                savedSignature?.let(signatureRepository::deleteSignatureFile)
                orderMediaRepository.deleteMediaFiles(attachedMedia)
                throw error
            }
        }
}
