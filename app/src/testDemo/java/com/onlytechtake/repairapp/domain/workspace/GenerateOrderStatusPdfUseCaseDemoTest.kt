package com.onlytechtake.repairapp.domain.usecases

import android.content.Context
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfPayload
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfWriter
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.data.orders.OrderDocumentFileStorage
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.domain.models.*
import com.onlytechtake.repairapp.domain.repositories.CatalogRepository
import com.onlytechtake.repairapp.domain.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.repositories.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class GenerateOrderStatusPdfUseCaseDemoTest {
    private lateinit var testRoot: File

    @Before
    fun setUp() {
        testRoot = File("build/tmp/order-status-pdf-demo-${UUID.randomUUID()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        testRoot.deleteRecursively()
    }

    @Test
    fun demoFlavorAddsWatermarkToGeneratedStatusPdfPayload() =
        runTest {
            assertTrue("Expected IS_DEMO_VERSION to be true", BuildConfig.IS_DEMO_VERSION)

            val context = mockk<Context>()
            every { context.filesDir } returns testRoot
            every { context.getString(any()) } answers {
                if (firstArg<Int>() == R.string.demo_version_banner) "VERSIONE DEMO" else "Aperta"
            }

            val workflowRepository = mockk<OrderRepository>()
            every { workflowRepository.observeOrderDetail("order-1") } returns flowOf(orderDetail())

            val catalogRepository = mockk<CatalogRepository>()
            coEvery { catalogRepository.getCategoryPath("cat-1") } returns "Notebook"

            val userRepository = mockk<UserRepository>()
            coEvery { userRepository.findById("operator-1") } returns user().toDomain()

            val ownerPreferences = mockk<OwnerPreferences>()
            every { ownerPreferences.ownerInfo } returns flowOf(OwnerInfo())

            val documentRepository = FakeOrderDocumentRepository()
            val pdfGenerator = CapturingPdfGenerator()
            val useCase =
                GenerateOrderStatusPdfUseCase(
                    context = context,
                    workflowRepository = workflowRepository,
                    catalogRepository = catalogRepository,
                    documentRepository = documentRepository,
                    userRepository = userRepository,
                    fileStorage = OrderDocumentFileStorage(MediaStorageProvider(context)),
                    ownerPreferences = ownerPreferences,
                    pdfGenerator = pdfGenerator,
                    clockMillis = { 1_000L },
                )

            val document =
                useCase(
                    orderId = "order-1",
                    userId = "operator-1",
                    includeCustomerSignature = false,
                ).getOrThrow()

            assertEquals("VERSIONE DEMO", pdfGenerator.capturedPayload?.watermarkText)
            assertEquals(document, documentRepository.saved.single())
            assertTrue(File(document.localPath).exists())
        }

    private fun orderDetail() =
        OrderDetail(
            order =
                RepairOrderEntity(
                    id = "order-1",
                    orderNumber = "2026-0001",
                    customerId = "customer-1",
                    title = "Notebook lento",
                    categoryId = "cat-1",
                    status = OrderStatus.OPEN,
                    priority = OrderPriority.NORMAL,
                    assignedToUserId = "operator-1",
                    createdByUserId = "operator-1",
                    description = "Avvio lento",
                    createdAt = 1L,
                    updatedAt = 1L,
                ).toDomain(),
            customer =
                CustomerEntity(
                    id = "customer-1",
                    type = CustomerType.PRIVATE,
                    firstName = "Mario",
                    lastName = "Rossi",
                    companyName = null,
                    vatNumber = null,
                    taxCode = null,
                    phone = "333",
                    email = null,
                    address = null,
                    notes = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                ).toDomain(),
            category = null,
            assignedUser = null,
            timeline = emptyList(),
            media = emptyList(),
            signatures = emptyList(),
        )

    private fun user() =
        UserEntity(
            id = "operator-1",
            displayName = "Operatore",
            username = "operatore",
            passwordHash = "hash",
            passwordSalt = "salt",
            role = UserRole.OPERATOR,
            permissions = setOf(UserPermission.EDIT_OWN_ORDERS),
            active = true,
            forcePasswordChange = false,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private class CapturingPdfGenerator : OrderStatusPdfWriter {
        var capturedPayload: OrderStatusPdfPayload? = null
            private set

        override fun generate(
            context: Context,
            outputFile: File,
            payload: OrderStatusPdfPayload,
        ): File {
            capturedPayload = payload
            outputFile.parentFile?.mkdirs()
            outputFile.writeBytes(byteArrayOf(1, 2, 3))
            return outputFile
        }
    }

    private class FakeOrderDocumentRepository : OrderDocumentRepository {
        val saved = mutableListOf<OrderDocument>()
        private val documentsFlow = MutableStateFlow<List<OrderDocument>>(emptyList())

        override suspend fun save(document: OrderDocument) {
            saved += document
            documentsFlow.value = saved.toList()
        }

        override suspend fun markShared(
            documentId: String,
            sharedAt: Long,
        ) = Unit

        override suspend fun findById(id: String): OrderDocument? = saved.firstOrNull { it.id == id }

        override fun observeByOrder(orderId: String): Flow<List<OrderDocument>> = documentsFlow

        override suspend fun delete(documentId: String): Result<Unit> = Result.success(Unit)
    }
}
