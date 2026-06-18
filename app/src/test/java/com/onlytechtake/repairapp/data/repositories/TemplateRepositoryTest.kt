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
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateRepositoryTest {
    private val context = mockk<Context>(relaxed = true)
    private val templateDao = mockk<MessageTemplateDao>(relaxed = true)
    private val settingDao = mockk<AppSettingDao>(relaxed = true)
    private val auditLogDao = mockk<AuditLogDao>(relaxed = true)
    private val repository =
        TemplateRepository(
            context = context,
            templateDao = templateDao,
            orderDao = mockk<RepairOrderDao>(relaxed = true),
            customerDao = mockk<CustomerDao>(relaxed = true),
            categoryDao = mockk<OrderCategoryDao>(relaxed = true),
            settingDao = settingDao,
            messageLogDao = mockk<MessageLogDao>(relaxed = true),
            timelineEventDao = mockk<TimelineEventDao>(relaxed = true),
            auditLogDao = auditLogDao,
            clockMillis = { 42L },
        )

    @Test
    fun resetToDefaultsRestoresSystemTemplatesAndAnchorMappings() =
        runBlocking {
            val savedTemplates = mutableListOf<MessageTemplateEntity>()
            val deletedKeys = slot<List<String>>()
            val audit = slot<AuditLogEntity>()
            coEvery { templateDao.findByCode(any()) } returns null
            coEvery { templateDao.findByCode("whatsapp_presa_in_carico") } returns existingInactiveTemplate()
            coEvery { templateDao.upsert(capture(savedTemplates)) } returns Unit
            coEvery { settingDao.deleteByKeys(capture(deletedKeys)) } returns TemplateAnchor.entries.size
            coEvery { auditLogDao.upsert(capture(audit)) } returns Unit

            val result = repository.resetToDefaults(admin())

            assertTrue(result.isSuccess)
            val restoredIntake = savedTemplates.first { it.code == "whatsapp_presa_in_carico" }
            assertEquals("existing-id", restoredIntake.id)
            assertEquals(true, restoredIntake.active)
            assertEquals(8, restoredIntake.version)
            assertEquals("admin", restoredIntake.updatedByUserId)
            assertEquals(42L, restoredIntake.updatedAt)
            assertEquals(TemplateAnchor.entries.map { it.settingKey }.toSet(), deletedKeys.captured.toSet())
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
            coEvery { templateDao.findByCode("custom_code") } returns existingInactiveTemplate().copy(code = "custom_code")
            coEvery { settingDao.upsert(capture(setting)) } returns Unit

            val result = repository.setTemplateForAnchor(admin(), TemplateAnchor.WHATSAPP_QUOTE, "custom_code")

            assertTrue(result.isSuccess)
            assertEquals(TemplateAnchor.WHATSAPP_QUOTE.settingKey, setting.captured.key)
            assertEquals("custom_code", setting.captured.value)
            assertEquals("admin", setting.captured.updatedByUserId)
            assertEquals(42L, setting.captured.updatedAt)
        }

    private fun existingInactiveTemplate() =
        MessageTemplateEntity(
            id = "existing-id",
            code = "whatsapp_presa_in_carico",
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
