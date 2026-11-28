package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.orders.OrderFileCleanupService
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.*
import com.onlytechtake.repairapp.domain.models.ClosureReason
import com.onlytechtake.repairapp.domain.models.CustomerActionStatus
import com.onlytechtake.repairapp.domain.models.CustomerFeedbackType
import com.onlytechtake.repairapp.domain.models.CustomerType
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.FeedbackInput
import com.onlytechtake.repairapp.domain.models.OrderPriority
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.models.TimelineEventType
import com.onlytechtake.repairapp.domain.models.UserPermission
import com.onlytechtake.repairapp.domain.models.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderRepositoryInstrumentedTest {
    private lateinit var database: RepairAppDatabase
    private lateinit var context: Context
    private lateinit var repository: DefaultOrderRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database =
            Room
                .inMemoryDatabaseBuilder(context, RepairAppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        repository =
            DefaultOrderRepository(
                context = context,
                repairOrderDao = database.repairOrderDao(),
                customerDao = database.customerDao(),
                categoryDao = database.orderCategoryDao(),
                timelineEventDao = database.timelineEventDao(),
                signatureDao = database.signatureDao(),
                messageLogDao = database.messageLogDao(),
                auditLogDao = database.auditLogDao(),
                database = database,
                fileCleanupService =
                    OrderFileCleanupService(
                        context,
                        com.onlytechtake.repairapp.core.media
                            .MediaStorageProvider(context),
                    ),
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun changeStatusUpdatesOrderAndWritesTimeline() =
        runBlocking {
            val actor = mockAdmin()
            seedCoreData()
            val order = createTestOrder()

            val result = repository.changeStatus(actor, order.id, OrderStatus.IN_PROGRESS)
            assertTrue(result.isSuccess)

            val updated = database.repairOrderDao().findById(order.id)
            assertEquals(OrderStatus.IN_PROGRESS, updated?.status)

            val events = database.timelineEventDao().observeForOrder(order.id).first()
            assertTrue(events.any { it.type == TimelineEventType.STATUS_CHANGED && it.newStatus == OrderStatus.IN_PROGRESS.name })
        }

    @Test
    fun changeStatusToClosedSetsClosedAtAndReason() =
        runBlocking {
            val actor = mockAdmin()
            seedCoreData()
            val order = createTestOrder()

            val result =
                repository.changeStatus(
                    actor,
                    order.id,
                    OrderStatus.CLOSED,
                    note = "Test close",
                    closureReason = ClosureReason.COMPLETED,
                )
            assertTrue(result.isSuccess)

            val updated = database.repairOrderDao().findById(order.id)
            assertEquals(OrderStatus.CLOSED, updated?.status)
            assertEquals(ClosureReason.COMPLETED, updated?.closureReason)
            assertNotNull(updated?.closedAt)
            assertEquals("Test close", updated?.closeNote)
        }

    @Test
    fun changeStatusToClosedRequiresCloseOrdersPermission() =
        runBlocking {
            val actor = mockOperator(permissions = setOf(UserPermission.EDIT_OWN_ORDERS))
            seedCoreData(
                userId = actor.id,
                role = UserRole.OPERATOR,
                permissions = actor.permissions,
            )
            val order = createTestOrder(assignedToUserId = actor.id)

            val result =
                repository.changeStatus(
                    actor,
                    order.id,
                    OrderStatus.CLOSED,
                    note = "Cliente rinuncia",
                    closureReason = ClosureReason.CUSTOMER_DECLINED,
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SecurityException)
            assertEquals(OrderStatus.OPEN, database.repairOrderDao().findById(order.id)?.status)
        }

    @Test
    fun changeStatusToClosedSucceedsWithCloseOrdersPermission() =
        runBlocking {
            val actor =
                mockOperator(
                    permissions =
                        setOf(
                            UserPermission.EDIT_OWN_ORDERS,
                            UserPermission.CLOSE_ORDERS,
                        ),
                )
            seedCoreData(
                userId = actor.id,
                role = UserRole.OPERATOR,
                permissions = actor.permissions,
            )
            val order = createTestOrder(assignedToUserId = actor.id)

            val result =
                repository.changeStatus(
                    actor,
                    order.id,
                    OrderStatus.CLOSED,
                    note = "Completata",
                    closureReason = ClosureReason.COMPLETED,
                )

            assertTrue(result.isSuccess)
            val updated = database.repairOrderDao().findById(order.id)
            assertEquals(OrderStatus.CLOSED, updated?.status)
            assertEquals(ClosureReason.COMPLETED, updated?.closureReason)
        }

    @Test
    fun recordFeedbackRejectRequiresCloseOrdersPermission() =
        runBlocking {
            val actor = mockOperator(permissions = setOf(UserPermission.EDIT_OWN_ORDERS))
            seedCoreData(
                userId = actor.id,
                role = UserRole.OPERATOR,
                permissions = actor.permissions,
            )
            val order =
                createTestOrder(
                    assignedToUserId = actor.id,
                    status = OrderStatus.RESULT,
                    diagnosisOutcome = DiagnosisOutcome.REPAIRABLE,
                    estimateAmountCents = 12_300L,
                    customerActionStatus = CustomerActionStatus.WAITING_ACCEPTANCE,
                )

            val result =
                repository.recordFeedback(
                    actor,
                    order.id,
                    FeedbackInput(CustomerFeedbackType.REJECT, note = "Troppo caro"),
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SecurityException)
            assertEquals(OrderStatus.RESULT, database.repairOrderDao().findById(order.id)?.status)
        }

    @Test
    fun recordFeedbackRejectSucceedsWithCloseOrdersPermission() =
        runBlocking {
            val actor =
                mockOperator(
                    permissions =
                        setOf(
                            UserPermission.EDIT_OWN_ORDERS,
                            UserPermission.CLOSE_ORDERS,
                        ),
                )
            seedCoreData(
                userId = actor.id,
                role = UserRole.OPERATOR,
                permissions = actor.permissions,
            )
            val order =
                createTestOrder(
                    assignedToUserId = actor.id,
                    status = OrderStatus.RESULT,
                    diagnosisOutcome = DiagnosisOutcome.REPAIRABLE,
                    estimateAmountCents = 12_300L,
                    customerActionStatus = CustomerActionStatus.WAITING_ACCEPTANCE,
                )

            val result =
                repository.recordFeedback(
                    actor,
                    order.id,
                    FeedbackInput(CustomerFeedbackType.REJECT, note = "Troppo caro"),
                )

            assertTrue(result.isSuccess)
            val updated = database.repairOrderDao().findById(order.id)
            assertEquals(OrderStatus.CLOSED, updated?.status)
            assertEquals(ClosureReason.CUSTOMER_DECLINED, updated?.closureReason)
            assertEquals(actor.id, updated?.closedByUserId)
        }

    private fun mockAdmin() =
        AuthenticatedUser(
            id = "admin-1",
            displayName = "Admin",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = UserPermission.entries.toSet(),
        )

    private fun mockOperator(permissions: Set<UserPermission>) =
        AuthenticatedUser(
            id = "operator-1",
            displayName = "Operatore",
            username = "operatore",
            role = UserRole.OPERATOR,
            permissions = permissions,
        )

    private suspend fun seedCoreData(
        userId: String = "admin-1",
        role: UserRole = UserRole.ADMIN,
        permissions: Set<UserPermission> = UserPermission.entries.toSet(),
    ) {
        val now = System.currentTimeMillis()
        database.userDao().upsert(
            UserEntity(
                id = userId,
                displayName = if (role == UserRole.ADMIN) "Admin" else "Operatore",
                username = if (role == UserRole.ADMIN) "admin" else "operatore",
                passwordHash = "h",
                passwordSalt = "s",
                role = role,
                permissions = permissions,
                active = true,
                forcePasswordChange = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.orderCategoryDao().upsert(OrderCategoryEntity("cat-1", null, 0, "Cat", null, true))
        database.customerDao().upsert(
            CustomerEntity(
                "cust-1",
                CustomerType.PRIVATE,
                "M",
                "R",
                null,
                null,
                null,
                "1",
                null,
                null,
                null,
                now,
                now,
            ),
        )
    }

    private suspend fun createTestOrder(
        assignedToUserId: String = "admin-1",
        status: OrderStatus = OrderStatus.OPEN,
        diagnosisOutcome: DiagnosisOutcome? = null,
        estimateAmountCents: Long? = null,
        customerActionStatus: CustomerActionStatus = CustomerActionStatus.NONE,
    ): RepairOrderEntity {
        val now = System.currentTimeMillis()
        val order =
            RepairOrderEntity(
                id = "order-1",
                orderNumber = "OTT-1",
                customerId = "cust-1",
                title = "T",
                categoryId = "cat-1",
                status = status,
                priority = OrderPriority.NORMAL,
                assignedToUserId = assignedToUserId,
                createdByUserId = assignedToUserId,
                description = "D",
                internalNotes = null,
                diagnosisOutcome = diagnosisOutcome,
                estimateAmountCents = estimateAmountCents,
                finalAmountCents = null,
                createdAt = now,
                updatedAt = now,
                closedAt = null,
                closureReason = null,
                customerActionStatus = customerActionStatus,
                closeNote = null,
                closedByUserId = null,
                categoryPathSnapshot = null,
                lastCustomerContactAt = null,
            )
        database.repairOrderDao().upsert(order)
        return order
    }
}
