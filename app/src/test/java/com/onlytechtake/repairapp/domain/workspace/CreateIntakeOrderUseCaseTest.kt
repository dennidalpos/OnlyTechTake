package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import android.graphics.Bitmap
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateIntakeOrderUseCaseTest {
    private val actor =
        AuthenticatedUser(
            id = "operator-1",
            displayName = "Operatore",
            username = "operatore",
            role = UserRole.OPERATOR,
            permissions =
                setOf(
                    UserPermission.CREATE_ORDERS,
                    UserPermission.EDIT_OWN_ORDERS,
                    UserPermission.MANAGE_CUSTOMERS,
                ),
        )

    @Test
    fun executeCreatesNewCustomerForAuthorizedOperator() =
        runTest {
            val newCustomer =
                CustomerInput(
                    firstName = "Mario",
                    lastName = "Rossi",
                    phone = "+39123456789",
                )
            val customer = customer()
            val order = order(customerId = customer.id)
            val stagedMedia = listOf(stagedMedia())
            val customerRepository = mockk<CustomerRepository>()
            val orderRepository = mockk<OrderWorkflowRepository>()
            val orderMediaRepository = mockk<OrderMediaRepository>()
            val signatureRepository = mockk<SignatureRepository>()
            coEvery { orderRepository.countAllOrders() } returns 0
            coEvery { customerRepository.save(actor, newCustomer) } returns Result.success(customer)
            coEvery {
                orderRepository.createOrder(
                    actor,
                    match {
                        it.customerId == customer.id &&
                            it.title == "Schermo rotto" &&
                            it.categoryId == "category-1"
                    },
                )
            } returns Result.success(order)
            coEvery {
                signatureRepository.saveSignature(actor, order.id, "Mario Rossi", true, true, any())
            } returns Result.success(signature(order.id))
            coEvery { orderMediaRepository.attachStagedMedia(actor, order.id, stagedMedia) } returns Result.success(emptyList())

            val useCase =
                CreateIntakeOrderUseCase(
                    context = mockk<Context>(relaxed = true),
                    customerRepository = customerRepository,
                    orderRepository = orderRepository,
                    orderMediaRepository = orderMediaRepository,
                    signatureRepository = signatureRepository,
                )

            val result =
                useCase.execute(
                    actor = actor,
                    input =
                        IntakeOrderInput(
                            newCustomer = newCustomer,
                            problemSummary = "Schermo rotto",
                            categoryId = "category-1",
                        ),
                    stagedMedia = stagedMedia,
                    signerName = "Mario Rossi",
                    privacyAccepted = true,
                    termsAccepted = true,
                    signatureBitmap = mockk<Bitmap>(),
                )

            assertTrue(result.isSuccess)
            assertEquals(IntakeOrderCreationResult.Complete(order), result.getOrThrow())
            coVerify(exactly = 1) { customerRepository.save(actor, newCustomer) }
            coVerify(exactly = 1) { orderRepository.createOrder(actor, any()) }
            coVerify(exactly = 1) { signatureRepository.saveSignature(actor, order.id, "Mario Rossi", true, true, any()) }
            coVerify(exactly = 1) { orderMediaRepository.attachStagedMedia(actor, order.id, stagedMedia) }
        }

    private fun customer() =
        CustomerEntity(
            id = "customer-1",
            type = CustomerType.PRIVATE,
            firstName = "Mario",
            lastName = "Rossi",
            companyName = null,
            vatNumber = null,
            taxCode = null,
            phone = "+39123456789",
            email = null,
            address = null,
            notes = null,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun order(customerId: String) =
        RepairOrderEntity(
            id = "order-1",
            orderNumber = "2026-0001",
            customerId = customerId,
            title = "Schermo rotto",
            categoryId = "category-1",
            status = OrderStatus.OPEN,
            priority = OrderPriority.NORMAL,
            assignedToUserId = actor.id,
            createdByUserId = actor.id,
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun signature(orderId: String) =
        SignatureEntity(
            id = "signature-1",
            orderId = orderId,
            signerName = "Mario Rossi",
            privacyAccepted = true,
            termsAccepted = true,
            privacyTextSnapshot = "Privacy",
            privacyTemplateVersion = 1,
            termsTextSnapshot = "Terms",
            termsTemplateVersion = 1,
            localPath = "signature.png",
            fileName = "signature.png",
            mimeType = "image/png",
            sizeBytes = 1L,
            width = 1,
            height = 1,
            createdByUserId = actor.id,
            signedAt = 1L,
            createdAt = 1L,
        )

    private fun stagedMedia() =
        StagedOrderMedia(
            localPath = "photo.jpg",
            fileName = "photo.jpg",
            mimeType = "image/jpeg",
            mediaType = "PHOTO",
            sizeBytes = 1L,
            width = 1,
            height = 1,
        )
}
