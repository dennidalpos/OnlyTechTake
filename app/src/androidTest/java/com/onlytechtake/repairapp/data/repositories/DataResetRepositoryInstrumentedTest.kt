package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.*
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DataResetRepositoryInstrumentedTest {
    private lateinit var database: RepairAppDatabase
    private lateinit var context: Context
    private lateinit var repository: DataResetRepository
    private lateinit var authPreferences: AuthPreferences
    private lateinit var sessionStore: InMemorySessionStore
    private lateinit var cleanupService: OrderFileCleanupService
    private lateinit var mediaStorageProvider: MediaStorageProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database =
            Room
                .inMemoryDatabaseBuilder(context, RepairAppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        authPreferences = AuthPreferences(context)
        sessionStore = InMemorySessionStore()
        mediaStorageProvider = MediaStorageProvider(context)
        cleanupService = OrderFileCleanupService(context, mediaStorageProvider)
        repository =
            DataResetRepository(
                context = context,
                database = database,
                auditLogDao = database.auditLogDao(),
                authPreferences = authPreferences,
                sessionStore = sessionStore,
                fileCleanupService = cleanupService,
                mediaStorageProvider = mediaStorageProvider,
            )
    }

    @After
    fun tearDown() {
        database.close()
        // Cleanup actual files created during test
        File(context.filesDir, "app_media").deleteRecursively()
        File(context.filesDir, "other").deleteRecursively()
    }

    @Test
    fun resetAllClearsDatabaseAndFiles() =
        runBlocking {
            val actor = mockAdmin()
            seedData()

            val orderFile = File(context.filesDir, "app_media/documents/order-1/test.txt")
            orderFile.parentFile?.mkdirs()
            orderFile.writeText("test")

            val result = repository.resetAll(actor, "RESET")
            assertTrue("Reset should be successful: ${result.exceptionOrNull()?.message}", result.isSuccess)

            // Users should be gone (resetAll deletes all tables)
            val users = database.userDao().observeAll().first()
            assertEquals(0, users.size)

            assertFalse(orderFile.exists())
        }

    @Test
    fun resetAllFailsWithWrongConfirmation() =
        runBlocking {
            val actor = mockAdmin()
            val result = repository.resetAll(actor, "WRONG")
            assertTrue(result.isFailure)
            assertEquals("Digitare RESET per confermare", result.exceptionOrNull()?.message)
        }

    @Test
    fun resetOrdersClearsOnlyOrderRelatedData() =
        runBlocking {
            val actor = mockAdmin()
            seedData()

            val orderFile = File(context.filesDir, "app_media/documents/order-1/test.txt")
            orderFile.parentFile?.mkdirs()
            orderFile.writeText("test")

            val otherFile = File(context.filesDir, "other/keep.txt")
            otherFile.parentFile?.mkdirs()
            otherFile.writeText("keep")

            val result = repository.resetOrders(actor, "CANCELLA TUTTO")
            assertTrue(result.isSuccess)

            // Orders should be gone
            val orderCount = database.repairOrderDao().countAll()
            assertEquals(0, orderCount)

            // User should still exist
            val userCount =
                database
                    .userDao()
                    .observeAll()
                    .first()
                    .size
            assertEquals(1, userCount)

            assertFalse(orderFile.exists())
            assertTrue(otherFile.exists())
        }

    @Test
    fun resetAllRequiresPermission() =
        runBlocking {
            // Operator without DELETE_DATA
            val actor =
                AuthenticatedUser(
                    id = "op-1",
                    displayName = "Op",
                    username = "op",
                    role = UserRole.OPERATOR,
                    permissions = setOf(UserPermission.CREATE_ORDERS),
                )
            val result = repository.resetAll(actor, "RESET")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SecurityException)
        }

    private fun mockAdmin() =
        AuthenticatedUser(
            id = "admin-1",
            displayName = "Admin",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = UserPermission.entries.toSet(),
        )

    private suspend fun seedData() {
        val now = System.currentTimeMillis()
        database.userDao().upsert(
            UserEntity(
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
                updatedAt = now,
            ),
        )
        database.orderCategoryDao().upsert(OrderCategoryEntity("cat-1", null, 0, "Cat", null, true))
        database.customerDao().upsert(CustomerEntity("cust-1", CustomerType.PRIVATE, "M", "R", null, null, null, "1", null, null, null, now, now))
        database.repairOrderDao().upsert(
            RepairOrderEntity(
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
                diagnosisOutcome = DiagnosisOutcome.REPAIRABLE,
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
            ),
        )
    }
}
