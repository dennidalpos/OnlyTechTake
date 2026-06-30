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
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderExportDaoInstrumentedTest {
    private lateinit var database: RepairAppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, RepairAppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportAllOrdersDerivesPaymentStateFromLatestPaymentEvent() =
        runBlocking {
            seedCoreData()
            seedOrder(id = "order-paid", orderNumber = "OTT-2026-000002")
            seedOrder(id = "order-unpaid", orderNumber = "OTT-2026-000001")
            database.timelineEventDao().upsert(
                paymentEvent(
                    "payment-old",
                    "order-paid",
                    TimelineEventType.PAYMENT_REMOVED,
                    100L,
                ),
            )
            database.timelineEventDao().upsert(
                paymentEvent(
                    "payment-new",
                    "order-paid",
                    TimelineEventType.PAYMENT_RECEIVED,
                    200L,
                ),
            )
            database.timelineEventDao().upsert(
                paymentEvent(
                    "payment-removed",
                    "order-unpaid",
                    TimelineEventType.PAYMENT_REMOVED,
                    300L,
                ),
            )

            val rowsByNumber =
                database.orderExportDao().exportAllOrders().associateBy { it.orderNumber }

            assertEquals(setOf("OTT-2026-000001", "OTT-2026-000002"), rowsByNumber.keys)
            assertTrue(rowsByNumber.getValue("OTT-2026-000002").isPaid)
            assertFalse(rowsByNumber.getValue("OTT-2026-000001").isPaid)
        }

    @Test
    fun exportAllOrdersIncludesLatestSignerName() =
        runBlocking {
            seedCoreData()
            seedOrder(id = "order-signed", orderNumber = "OTT-2026-SIGNED")

            val now = 1_717_171_717_000L
            database.signatureDao().upsert(
                SignatureEntity(
                    id = "sig-1",
                    orderId = "order-signed",
                    signerName = "Old Signer",
                    privacyAccepted = true,
                    termsAccepted = true,
                    privacyTextSnapshot = "...",
                    privacyTemplateVersion = 1,
                    termsTextSnapshot = "...",
                    termsTemplateVersion = 1,
                    localPath = "/path/1",
                    fileName = "sig1.png",
                    mimeType = "image/png",
                    sizeBytes = 100,
                    width = 100,
                    height = 100,
                    createdByUserId = "admin-1",
                    signedAt = now,
                    createdAt = now,
                ),
            )
            database.signatureDao().upsert(
                SignatureEntity(
                    id = "sig-2",
                    orderId = "order-signed",
                    signerName = "Latest Signer",
                    privacyAccepted = true,
                    termsAccepted = true,
                    privacyTextSnapshot = "...",
                    privacyTemplateVersion = 1,
                    termsTextSnapshot = "...",
                    termsTemplateVersion = 1,
                    localPath = "/path/2",
                    fileName = "sig2.png",
                    mimeType = "image/png",
                    sizeBytes = 100,
                    width = 100,
                    height = 100,
                    createdByUserId = "admin-1",
                    signedAt = now + 1000,
                    createdAt = now + 1000,
                ),
            )

            val rows = database.orderExportDao().exportAllOrders()
            val signedRow = rows.find { it.orderNumber == "OTT-2026-SIGNED" }

            assertEquals("Latest Signer", signedRow?.signerName)
        }

    private suspend fun seedCoreData() {
        val now = 1_717_171_717_000L
        database.userDao().upsert(
            UserEntity(
                id = "admin-1",
                displayName = "Admin",
                username = "admin",
                passwordHash = "hash",
                passwordSalt = "salt",
                role = UserRole.ADMIN,
                active = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database
            .orderCategoryDao()
            .upsert(OrderCategoryEntity("cat-1", null, 0, "Smartphone", null, active = true))
        database.customerDao().upsert(
            CustomerEntity(
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
            ),
        )
    }

    private suspend fun seedOrder(
        id: String,
        orderNumber: String,
    ) {
        val now = 1_717_171_717_000L
        database.repairOrderDao().upsert(
            RepairOrderEntity(
                id = id,
                orderNumber = orderNumber,
                customerId = "customer-1",
                title = "Riparazione test",
                categoryId = "cat-1",
                status = OrderStatus.OPEN,
                priority = OrderPriority.NORMAL,
                assignedToUserId = "admin-1",
                createdByUserId = "admin-1",
                description = "Smoke export",
                internalNotes = null,
                estimateAmountCents = null,
                finalAmountCents = null,
                createdAt = now,
                updatedAt = now,
                closedAt = null,
                closureReason = null,
            ),
        )
    }

    private fun paymentEvent(
        id: String,
        orderId: String,
        type: TimelineEventType,
        createdAt: Long,
    ) = TimelineEventEntity(
        id = id,
        orderId = orderId,
        type = type,
        title = type.name,
        description = null,
        oldStatus = null,
        newStatus = null,
        visibleToCustomer = false,
        requiresCustomerFeedback = false,
        expectedFeedbackType = null,
        customerFeedback = null,
        customerFeedbackNote = null,
        customerFeedbackAt = null,
        createdByUserId = "admin-1",
        createdAt = createdAt,
    )
}
