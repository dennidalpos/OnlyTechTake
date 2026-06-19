package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfPayload
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfWriter
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.OrderDocumentFileStorage
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
        runBlocking {
            assertTrue(BuildConfig.IS_DEMO_VERSION)

            val context = mockk<Context>()
            every { context.filesDir } returns testRoot
            every { context.getString(any()) } answers {
                if (firstArg<Int>() == R.string.demo_version_banner) "VERSIONE DEMO" else "Aperta"
            }

            val workflowRepository = mockk<OrderWorkflowRepository>()
            every { workflowRepository.observeOrderDetail("order-1") } returns flowOf(orderDetail())

            val catalogRepository = mockk<CatalogRepository>()
            coEvery { catalogRepository.getCategoryPath("cat-1") } returns "Notebook"

            val userDao = mockk<UserDao>()
            coEvery { userDao.findById("operator-1") } returns user()

            val documentRepository = FakeOrderDocumentRepository()
            val pdfGenerator = CapturingPdfGenerator()
            val useCase =
                GenerateOrderStatusPdfUseCase(
                    context = context,
                    workflowRepository = workflowRepository,
                    catalogRepository = catalogRepository,
                    documentRepository = documentRepository,
                    userDao = userDao,
                    fileStorage = OrderDocumentFileStorage(MediaStorageProvider(context)),
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
                ),
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
                ),
            category = null,
            assignedUser = null,
            timeline = emptyList(),
            messages = emptyList(),
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
        val saved = mutableListOf<OrderDocumentEntity>()
        private val documentsFlow = MutableStateFlow<List<OrderDocumentEntity>>(emptyList())

        override suspend fun save(document: OrderDocumentEntity) {
            saved += document
            documentsFlow.value = saved.toList()
        }

        override suspend fun markShared(
            documentId: String,
            sharedAt: Long,
        ) = Unit

        override suspend fun findById(id: String): OrderDocumentEntity? =
            saved.firstOrNull { it.id == id }

        override fun observeByOrder(orderId: String): Flow<List<OrderDocumentEntity>> =
            documentsFlow

        override suspend fun delete(documentId: String): Result<Unit> = Result.success(Unit)
    }
}
