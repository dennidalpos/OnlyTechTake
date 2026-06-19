package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Intent
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentType
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthState
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.auth.LoginResult
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testRoot: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testRoot = File("build/tmp/order-detail-vm-${UUID.randomUUID()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testRoot.deleteRecursively()
    }

    @Test
    fun pdfSentConfirmationMarksDocumentSharedAndRecordsTimeline() =
        runTest {
            val actor =
                AuthenticatedUser(
                    id = "operator-1",
                    displayName = "Operatore",
                    username = "operatore",
                    role = UserRole.OPERATOR,
                )
            val pdf =
                File(testRoot, "stato.pdf").absoluteFile.apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val documentRepository =
                FakeOrderDocumentRepository(
                    document(
                        id = "document-1",
                        localPath = pdf.absolutePath,
                        fileName = pdf.name,
                    ),
                )
            val orderRepository = mockk<OrderWorkflowRepository>()
            every { orderRepository.observeOrderDetail("order-1") } returns flowOf(null)
            coEvery {
                orderRepository.recordPdfShared(
                    actor,
                    "order-1",
                    "stato.pdf"
                )
            } returns Result.success(Unit)

            val shareFactory = mockk<ShareDocumentIntentFactory>()
            every {
                shareFactory.createPdfShareIntent(
                    pdf,
                    "Documento stato.pdf"
                )
            } returns mockk<Intent>(relaxed = true)

            val viewModel =
                OrderDetailViewModel(
                    orderId = "order-1",
                    authRepository = FakeAuthRepository(actor),
                    orderRepository = orderRepository,
                    orderMediaRepository = mockk<OrderMediaRepository>(relaxed = true),
                    orderDocumentRepository = documentRepository,
                    templateRepository = mockk<TemplateRepository>(relaxed = true),
                    generateOrderStatusPdfUseCase = mockk<GenerateOrderStatusPdfUseCase>(relaxed = true),
                    shareDocumentIntentFactory = shareFactory,
                    onSetFormDirty = {},
                )

            viewModel.onShareDocumentClicked("document-1")
            advanceUntilIdle()

            assertTrue(viewModel.showPdfSentConfirmation.value)
            assertEquals("document-1", viewModel.lastGeneratedDocumentId.value)
            assertNotNull(viewModel.pendingShareIntent.value)

            viewModel.onPdfSentConfirmed()
            advanceUntilIdle()

            assertFalse(viewModel.showPdfSentConfirmation.value)
            assertNull(viewModel.lastGeneratedDocumentId.value)
            assertEquals("document-1", documentRepository.markedDocumentId)
            assertNotNull(documentRepository.findById("document-1")?.sharedAt)
            coVerify(exactly = 1) { orderRepository.recordPdfShared(actor, "order-1", "stato.pdf") }
        }

    private fun document(
        id: String,
        localPath: String,
        fileName: String,
    ): OrderDocumentEntity =
        OrderDocumentEntity(
            id = id,
            orderId = "order-1",
            type = OrderDocumentType.STATUS_PROGRESS_PDF,
            localPath = localPath,
            fileName = fileName,
            mimeType = "application/pdf",
            sizeBytes = 3L,
            includesCustomerSignature = true,
            createdByUserId = "operator-1",
            createdAt = 1_000L,
            sharedAt = null,
        )

    private class FakeOrderDocumentRepository(
        initialDocument: OrderDocumentEntity,
    ) : OrderDocumentRepository {
        private val documents = mutableMapOf(initialDocument.id to initialDocument)
        private val documentsFlow = MutableStateFlow(listOf(initialDocument))
        var markedDocumentId: String? = null
            private set

        override suspend fun save(document: OrderDocumentEntity) {
            documents[document.id] = document
            documentsFlow.value = documents.values.toList()
        }

        override suspend fun markShared(
            documentId: String,
            sharedAt: Long,
        ) {
            markedDocumentId = documentId
            documents[documentId] = documents.getValue(documentId).copy(sharedAt = sharedAt)
            documentsFlow.value = documents.values.toList()
        }

        override suspend fun findById(id: String): OrderDocumentEntity? = documents[id]

        override fun observeByOrder(orderId: String): Flow<List<OrderDocumentEntity>> =
            documentsFlow

        override suspend fun delete(documentId: String): Result<Unit> =
            runCatching {
                documents.remove(documentId) ?: error("Documento non trovato")
                documentsFlow.value = documents.values.toList()
            }
    }

    private class FakeAuthRepository(
        user: AuthenticatedUser,
    ) : AuthRepository {
        override val currentUser: StateFlow<AuthenticatedUser?> = MutableStateFlow(user)

        override fun observeAuthState(): Flow<AuthState> = emptyFlow()

        override suspend fun createFirstAdmin(
            displayName: String,
            username: String,
            password: CharArray,
        ): AuthenticatedUser = error("createFirstAdmin is not expected")

        override suspend fun login(
            username: String,
            password: CharArray,
        ): LoginResult = error("login is not expected")

        override suspend fun loginDemo(): LoginResult = error("loginDemo is not expected")

        override suspend fun changePassword(
            username: String,
            currentPassword: CharArray,
            newPassword: CharArray,
        ): Result<Unit> = error("changePassword is not expected")

        override suspend fun getRecoveryKit(): Result<String> =
            error("getRecoveryKit is not expected")

        override suspend fun resetAdminWithRecoveryCode(
            code: String,
            newPassword: CharArray,
        ): Result<Unit> = error("resetAdminWithRecoveryCode is not expected")

        override fun touchSession() = Unit

        override suspend fun logout() = Unit
    }
}
