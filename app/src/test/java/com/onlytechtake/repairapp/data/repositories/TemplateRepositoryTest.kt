package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.MessageChannel
import com.onlytechtake.repairapp.domain.models.OwnerInfo
import com.onlytechtake.repairapp.domain.models.UserPermission
import com.onlytechtake.repairapp.domain.models.UserRole
import com.onlytechtake.repairapp.domain.repositories.TemplateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateRepositoryTest {
    private val context =
        mockk<Context>(relaxed = true) {
            every { getString(any<Int>()) } returns "localized"
            every { getString(any<Int>(), *anyVararg()) } returns "localized"
        }
    private val templateDao = mockk<MessageTemplateDao>(relaxed = true)
    private val settingDao = mockk<AppSettingDao>(relaxed = true)
    private val auditLogDao = mockk<AuditLogDao>(relaxed = true)
    private val ownerPreferences =
        mockk<OwnerPreferences>(relaxed = true) {
            every { ownerInfo } returns flowOf(OwnerInfo(name = "Test Owner"))
        }
    private val repository: TemplateRepository =
        DefaultTemplateRepository(
            context = context,
            templateDao = templateDao,
            orderDao = mockk<RepairOrderDao>(relaxed = true),
            customerDao = mockk<CustomerDao>(relaxed = true),
            categoryDao = mockk<OrderCategoryDao>(relaxed = true),
            settingDao = settingDao,
            messageLogDao = mockk<MessageLogDao>(relaxed = true),
            timelineEventDao = mockk<TimelineEventDao>(relaxed = true),
            auditLogDao = auditLogDao,
            ownerPreferences = ownerPreferences,
            clockMillis = { 42L },
        )

    @Test
    fun `prepareMessage renders owner variables correctly`() =
        runBlocking {
            val orderDao = mockk<RepairOrderDao>(relaxed = true)
            val customerDao = mockk<CustomerDao>(relaxed = true)
            val categoryDao = mockk<OrderCategoryDao>(relaxed = true)
            val repository: TemplateRepository =
                DefaultTemplateRepository(
                    context = context,
                    templateDao = templateDao,
                    orderDao = orderDao,
                    customerDao = customerDao,
                    categoryDao = categoryDao,
                    settingDao = settingDao,
                    messageLogDao = mockk(relaxed = true),
                    timelineEventDao = mockk(relaxed = true),
                    auditLogDao = auditLogDao,
                    ownerPreferences =
                        mockk {
                            every { ownerInfo } returns
                                flowOf(
                                    OwnerInfo(
                                        name = "Owner Name",
                                        vatNumber = "PIVA123",
                                    ),
                                )
                        },
                )

            val orderId = "order-1"
            val customerId = "cust-1"
            coEvery { orderDao.findById(orderId) } returns
                com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity(
                    id = orderId,
                    orderNumber = "2026-001",
                    customerId = customerId,
                    title = "Phone Repair",
                    categoryId = "cat-1",
                    status = com.onlytechtake.repairapp.domain.models.OrderStatus.OPEN,
                    priority = com.onlytechtake.repairapp.domain.models.OrderPriority.NORMAL,
                    createdByUserId = "u1",
                    assignedToUserId = null,
                    description = "",
                    createdAt = 0L,
                    updatedAt = 0L,
                    internalNotes = null,
                    diagnosisOutcome = null,
                    estimateAmountCents = null,
                    finalAmountCents = null,
                    closedAt = null,
                    closureReason = null,
                    customerActionStatus = null,
                    closeNote = null,
                    closedByUserId = null,
                    categoryPathSnapshot = null,
                    lastCustomerContactAt = null,
                )
            coEvery { customerDao.findById(customerId) } returns
                com.onlytechtake.repairapp.data.local.entities.CustomerEntity(
                    id = customerId,
                    firstName = "John",
                    lastName = "Doe",
                    type = com.onlytechtake.repairapp.domain.models.CustomerType.PRIVATE,
                    phone = "123456",
                    companyName = null,
                    vatNumber = null,
                    taxCode = null,
                    email = null,
                    address = null,
                    notes = null,
                    createdAt = 0L,
                    updatedAt = 0L,
                )
            coEvery { templateDao.findActiveByCode("test_code") } returns
                MessageTemplateEntity(
                    id = "t1",
                    code = "test_code",
                    title = "Test",
                    body = "Owner: {{proprietario_nome}}, PIVA: {{proprietario_piva}}",
                    channel = MessageChannel.WHATSAPP,
                    active = true,
                    version = 1,
                    updatedByUserId = "u1",
                    updatedAt = 0L,
                )

            val result = repository.prepareMessage(admin(), orderId, "test_code")

            assertTrue(result.isSuccess)
            assertEquals("Owner: Owner Name, PIVA: PIVA123", result.getOrNull()?.body)
        }

    @Test
    fun resetToDefaultsRestoresSystemTemplatesAndAnchorMappings() =
        runBlocking {
            val savedTemplates = mutableListOf<MessageTemplateEntity>()
            val deletedKeys = slot<List<String>>()
            val audit = slot<AuditLogEntity>()
            coEvery { templateDao.findByCode(any()) } returns null
            coEvery { templateDao.findByCode("whatsapp_intake") } returns existingInactiveTemplate()
            coEvery { templateDao.upsert(capture(savedTemplates)) } returns Unit
            coEvery { settingDao.deleteByKeys(capture(deletedKeys)) } returns TemplateAnchor.entries.size
            coEvery { auditLogDao.upsert(capture(audit)) } returns Unit

            val result = repository.resetToDefaults(admin())

            assertTrue(result.isSuccess)
            val restoredIntake = savedTemplates.first { it.code == "whatsapp_intake" }
            assertEquals("existing-id", restoredIntake.id)
            assertEquals(true, restoredIntake.active)
            assertEquals(8, restoredIntake.version)
            assertEquals("admin", restoredIntake.updatedByUserId)
            assertEquals(42L, restoredIntake.updatedAt)
            assertEquals(
                TemplateAnchor.entries.map { it.settingKey }.toSet(),
                deletedKeys.captured.toSet(),
            )
            assertTrue(
                audit.captured.detailsJson
                    .orEmpty()
                    .contains("reset_to_defaults"),
            )
        }

    @Test
    fun resetToDefaultsFailsWithoutManageTemplatesPermission() =
        runBlocking {
            val result = repository.resetToDefaults(restrictedOperator())

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { settingDao.deleteByKeys(any()) }
            coVerify(exactly = 0) { templateDao.upsert(any()) }
        }

    @Test
    fun setTemplateForAnchorPersistsMappingForExistingTemplate() =
        runBlocking {
            val setting = slot<AppSettingEntity>()
            coEvery { templateDao.findByCode("custom_code") } returns
                existingInactiveTemplate().copy(
                    code = "custom_code",
                )
            coEvery { settingDao.upsert(capture(setting)) } returns Unit

            val result =
                repository.setTemplateForAnchor(
                    admin(),
                    TemplateAnchor.WHATSAPP_QUOTE,
                    "custom_code",
                )

            assertTrue(result.isSuccess)
            assertEquals(TemplateAnchor.WHATSAPP_QUOTE.settingKey, setting.captured.key)
            assertEquals("custom_code", setting.captured.value)
            assertEquals("admin", setting.captured.updatedByUserId)
            assertEquals(42L, setting.captured.updatedAt)
        }

    private fun existingInactiveTemplate() =
        MessageTemplateEntity(
            id = "existing-id",
            code = "whatsapp_intake",
            title = "Custom",
            channel = MessageChannel.WHATSAPP,
            body = "Custom body",
            active = false,
            version = 7,
            updatedByUserId = "previous",
            updatedAt = 1L,
            step = "Custom",
        )

    private fun admin() =
        AuthenticatedUser(
            id = "admin",
            username = "admin",
            displayName = "Admin",
            role = UserRole.ADMIN,
            permissions = UserPermission.entries.toSet(),
        )

    private fun restrictedOperator() =
        AuthenticatedUser(
            id = "operator",
            username = "operator",
            displayName = "Operator",
            role = UserRole.OPERATOR,
            permissions = emptySet(),
        )
}
