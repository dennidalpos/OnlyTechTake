package com.onlytechtake.repairapp.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
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
class RepairAppDatabaseInstrumentedTest {
    private lateinit var database: RepairAppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RepairAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun opensDatabaseAndPersistsCoreOrderGraph() = runBlocking {
        val now = 1_717_171_717_000L
        val user = UserEntity(
            id = "user-1",
            displayName = "Admin",
            username = "admin",
            passwordHash = "hash",
            passwordSalt = "salt",
            role = UserRole.ADMIN,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
        val customer = CustomerEntity(
            id = "customer-1",
            type = CustomerType.PRIVATE,
            firstName = "Mario",
            lastName = "Rossi",
            companyName = null,
            vatNumber = null,
            taxCode = null,
            phone = "3330000000",
            email = null,
            address = null,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
        val order = RepairOrderEntity(
            id = "order-1",
            orderNumber = "RIP-2026-000001",
            customerId = customer.id,
            title = "Riparazione test",
            categoryId = "category-1",
            status = OrderStatus.OPEN,
            priority = OrderPriority.NORMAL,
            assignedToUserId = user.id,
            createdByUserId = user.id,
            description = "Smoke database",
            internalNotes = null,
            estimateAmountCents = null,
            finalAmountCents = null,
            createdAt = now,
            updatedAt = now,
            closedAt = null,
            closureReason = null,
            deletedAt = null,
            deletedByUserId = null,
            deleteReason = null,
        )

        database.userDao().upsert(user)
        database.orderCategoryDao().upsert(testCategory("category-1"))
        database.customerDao().upsert(customer)
        database.repairOrderDao().upsert(order)

        assertNotNull(database.userDao().findById(user.id))
        assertNotNull(database.customerDao().findById(customer.id))
        assertEquals(order.orderNumber, database.repairOrderDao().findById(order.id)?.orderNumber)
    }

    @Test
    fun customerSearchFindsCustomersByMainFieldsAndOrderNumber() = runBlocking {
        val now = 1_717_171_717_000L
        val user = UserEntity(
            id = "user-1",
            displayName = "Admin",
            username = "admin",
            passwordHash = "hash",
            passwordSalt = "salt",
            role = UserRole.ADMIN,
            active = true,
            createdAt = now,
            updatedAt = now,
        )
        val privateCustomer = CustomerEntity(
            id = "customer-private",
            type = CustomerType.PRIVATE,
            firstName = "Mario",
            lastName = "Rossi",
            companyName = null,
            vatNumber = null,
            taxCode = "RSSMRA80A01H501U",
            phone = "3330000000",
            email = "mario@example.com",
            address = null,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
        val businessCustomer = CustomerEntity(
            id = "customer-business",
            type = CustomerType.BUSINESS,
            firstName = null,
            lastName = null,
            companyName = "Only Tech Srl",
            vatNumber = "12345678901",
            taxCode = null,
            phone = "0220000000",
            email = "office@onlytech.example",
            address = null,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
        val order = RepairOrderEntity(
            id = "order-1",
            orderNumber = "RIP-2026-000777",
            customerId = privateCustomer.id,
            title = "Riparazione search",
            categoryId = "category-1",
            status = OrderStatus.OPEN,
            priority = OrderPriority.NORMAL,
            assignedToUserId = user.id,
            createdByUserId = user.id,
            description = null,
            internalNotes = null,
            estimateAmountCents = null,
            finalAmountCents = null,
            createdAt = now,
            updatedAt = now,
            closedAt = null,
            closureReason = null,
            deletedAt = null,
            deletedByUserId = null,
            deleteReason = null,
        )

        database.userDao().upsert(user)
        database.orderCategoryDao().upsert(testCategory("category-1"))
        database.customerDao().upsert(privateCustomer)
        database.customerDao().upsert(businessCustomer)
        database.repairOrderDao().upsert(order)

        assertSearchContains("Mario", privateCustomer.id)
        assertSearchContains("Rossi", privateCustomer.id)
        assertSearchContains("333", privateCustomer.id)
        assertSearchContains("mario@example.com", privateCustomer.id)
        assertSearchContains("Only Tech", businessCustomer.id)
        assertSearchContains("12345678901", businessCustomer.id)
        assertSearchContains("RSSMRA", privateCustomer.id)
        assertSearchContains("000777", privateCustomer.id)
    }

    private suspend fun assertSearchContains(query: String, customerId: String) {
        val ids = database.customerDao().search(query).first().map { it.id }
        assertTrue("Expected search '$query' to contain '$customerId', got $ids", customerId in ids)
    }

    private fun testCategory(id: String) = OrderCategoryEntity(
        id = id,
        name = "Smartphone",
        description = null,
        active = true,
        sortOrder = 0,
    )
}
