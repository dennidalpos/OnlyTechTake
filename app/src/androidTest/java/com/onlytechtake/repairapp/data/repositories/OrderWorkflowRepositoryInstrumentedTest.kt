package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.*
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderWorkflowRepositoryInstrumentedTest {
    private lateinit var database: RepairAppDatabase
    private lateinit var context: Context
    private lateinit var repository: OrderWorkflowRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, RepairAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        repository = OrderWorkflowRepository(
            context = context,
            repairOrderDao = database.repairOrderDao(),
            customerDao = database.customerDao(),
            categoryDao = database.orderCategoryDao(),
            userDao = database.userDao(),
            timelineEventDao = database.timelineEventDao(),
            orderMediaDao = database.orderMediaDao(),
            signatureDao = database.signatureDao(),
            messageLogDao = database.messageLogDao(),
            auditLogDao = database.auditLogDao(),
            database = database,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun changeStatusUpdatesOrderAndWritesTimeline() = runBlocking {
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
    fun changeStatusToClosedSetsClosedAtAndReason() = runBlocking {
        val actor = mockAdmin()
        seedCoreData()
        val order = createTestOrder()
        
        val result = repository.changeStatus(
            actor, 
            order.id, 
            OrderStatus.CLOSED, 
            note = "Test close", 
            closureReason = ClosureReason.COMPLETED
        )
        assertTrue(result.isSuccess)
        
        val updated = database.repairOrderDao().findById(order.id)
        assertEquals(OrderStatus.CLOSED, updated?.status)
        assertEquals(ClosureReason.COMPLETED, updated?.closureReason)
        assertNotNull(updated?.closedAt)
        assertEquals("Test close", updated?.closeNote)
    }

    private fun mockAdmin() = AuthenticatedUser(
        id = "admin-1",
        displayName = "Admin",
        username = "admin",
        role = UserRole.ADMIN,
        permissions = UserPermission.entries.toSet()
    )

    private suspend fun seedCoreData() {
        val now = System.currentTimeMillis()
        database.userDao().upsert(UserEntity(
            id = "admin-1", 
            displayName = "Admin", 
            username = "admin", 
            passwordHash = "h", 
            passwordSalt = "s", 
            role = UserRole.ADMIN, 
            permissions = UserPermission.entries.toSet(),
            active = true, 
            forcePasswordChange = false,
            createdAt = now, 
            updatedAt = now
        ))
        database.orderCategoryDao().upsert(OrderCategoryEntity("cat-1", null, 0, "Cat", null, true))
        database.customerDao().upsert(CustomerEntity("cust-1", CustomerType.PRIVATE, "M", "R", null, null, null, "1", null, null, null, now, now))
    }

    private suspend fun createTestOrder(): RepairOrderEntity {
        val now = System.currentTimeMillis()
        val order = RepairOrderEntity(
            id = "order-1", 
            orderNumber = "RIP-1", 
            customerId = "cust-1", 
            title = "T", 
            categoryId = "cat-1", 
            status = OrderStatus.OPEN, 
            priority = OrderPriority.NORMAL, 
            assignedToUserId = "admin-1", 
            createdByUserId = "admin-1", 
            description = "D", 
            internalNotes = null, 
            diagnosisOutcome = null, 
            estimateAmountCents = null, 
            finalAmountCents = null, 
            createdAt = now, 
            updatedAt = now, 
            closedAt = null, 
            closureReason = null, 
            customerActionStatus = CustomerActionStatus.NONE,
            closeNote = null,
            closedByUserId = null,
            categoryPathSnapshot = null,
            lastCustomerContactAt = null,
        )
        database.repairOrderDao().upsert(order)
        return order
    }
}
