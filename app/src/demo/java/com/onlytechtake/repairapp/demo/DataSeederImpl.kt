package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.core.utils.LocaleHelper
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Implementation of [DataSeeder] for the Demo flavor.
 * Seeds the database with minimal demo data if requested.
 */
class DataSeederImpl : DataSeeder {
    override suspend fun seed(context: Context) =
        withContext(Dispatchers.IO) {
            val englishContext = getLocalizedContext(context)
            DemoSeeder.seedDemoData(englishContext)
        }

    override val defaultPassword: String = DemoSeeder.DEFAULT_PASSWORD

    private fun getLocalizedContext(
        context: Context,
    ): Context = LocaleHelper.applyLocale(context)
}

object DemoSeeder {
    const val DEFAULT_PASSWORD = "DemoPassword123"

    suspend fun seedDemoData(context: Context) {
        val authPreferences = AuthPreferences(context)
        val db = RepairAppDatabase.getInstance(context)
        val now = System.currentTimeMillis()
        val passwordHasher = PasswordHasher()
        val pwdHash = passwordHasher.hash(DEFAULT_PASSWORD.toCharArray())

        db.withTransaction {
            // Seed Admin
            db.userDao().upsert(
                UserEntity(
                    id = "admin",
                    displayName = DemoMode.getAdminDisplayName(context),
                    username = "admin",
                    passwordHash = pwdHash.hash,
                    passwordSalt = pwdHash.salt,
                    role = UserRole.ADMIN,
                    permissions = UserPermission.entries.toSet(),
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            // Seed Demo Content
            db.customerDao().upsert(DemoMode.getDemoCustomer(context))
            db.orderCategoryDao().upsertAll(DemoMode.getDemoCategories(context))

            // Seed Default Templates
            seedDefaultTemplates(db, context, now)

            // Reset Anchor Mappings to defaults
            db.appSettingDao().deleteByKeys(
                TemplateAnchor.entries.map { it.settingKey },
            )
        }

        authPreferences.markConfigured()
    }

    private suspend fun seedDefaultTemplates(
        db: RepairAppDatabase,
        context: Context,
        now: Long,
    ) {
        val templates =
            listOf(
                SeedTemplate(
                    "privacy_short",
                    R.string.template_privacy_short_title,
                    R.string.template_privacy_short_body,
                    R.string.anchor_step_legal,
                ),
                SeedTemplate(
                    "terms_intake",
                    R.string.template_terms_title,
                    R.string.template_terms_body,
                    R.string.anchor_step_legal,
                ),
                SeedTemplate(
                    "whatsapp_intake",
                    R.string.template_wa_intake_title,
                    R.string.template_wa_intake_body,
                    R.string.anchor_step_intake,
                ),
                SeedTemplate(
                    "whatsapp_update",
                    R.string.template_wa_update_title,
                    R.string.template_wa_update_body,
                    R.string.anchor_step_update,
                ),
                SeedTemplate(
                    "whatsapp_diagnosis",
                    R.string.template_wa_diagnosis_title,
                    R.string.template_wa_diagnosis_body,
                    R.string.anchor_step_diagnosis,
                ),
                SeedTemplate(
                    "whatsapp_quote",
                    R.string.template_wa_quote_title,
                    R.string.template_wa_quote_body,
                    R.string.anchor_step_quote,
                ),
                SeedTemplate(
                    "whatsapp_not_repairable",
                    R.string.template_wa_not_repairable_title,
                    R.string.template_wa_not_repairable_body,
                    R.string.anchor_step_outcome,
                ),
                SeedTemplate(
                    "whatsapp_repairing",
                    R.string.template_wa_repairing_title,
                    R.string.template_wa_repairing_body,
                    R.string.anchor_step_repair,
                ),
                SeedTemplate(
                    "whatsapp_ready",
                    R.string.template_wa_ready_title,
                    R.string.template_wa_ready_body,
                    R.string.anchor_step_pickup,
                ),
                SeedTemplate(
                    "whatsapp_pdf_status",
                    R.string.template_wa_pdf_status_title,
                    R.string.template_wa_pdf_status_body,
                    R.string.anchor_step_docs,
                ),
                SeedTemplate(
                    "whatsapp_completed",
                    R.string.template_wa_completed_title,
                    R.string.template_wa_completed_body,
                    R.string.anchor_step_closed,
                ),
            )

        templates.forEach { template ->
            if (db.messageTemplateDao().findByCode(template.code) == null) {
                db.messageTemplateDao().upsert(
                    MessageTemplateEntity(
                        id = UUID.randomUUID().toString(),
                        code = template.code,
                        title = context.getString(template.titleRes),
                        channel = MessageChannel.WHATSAPP,
                        body = context.getString(template.bodyRes),
                        active = true,
                        version = 1,
                        updatedByUserId = "admin",
                        updatedAt = now,
                        step = context.getString(template.stepRes),
                    ),
                )
            }
        }
    }

    private data class SeedTemplate(
        val code: String,
        val titleRes: Int,
        val bodyRes: Int,
        val stepRes: Int,
    )
}
