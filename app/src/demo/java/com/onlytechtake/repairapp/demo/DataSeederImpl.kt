package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.security.PasswordHasher
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
            DemoSeeder.seedDemoData(context)
        }

    override val defaultPassword: String = DemoSeeder.DEFAULT_PASSWORD
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
                Triple(
                    "privacy_breve",
                    R.string.template_privacy_breve_title,
                    R.string.anchor_step_legal,
                ),
                Triple(
                    "condizioni_presa_in_carico",
                    R.string.template_condizioni_title,
                    R.string.anchor_step_legal,
                ),
                Triple(
                    "whatsapp_presa_in_carico",
                    R.string.template_wa_intake_title,
                    R.string.anchor_step_intake,
                ),
                Triple(
                    "whatsapp_aggiornamento",
                    R.string.template_wa_update_title,
                    R.string.anchor_step_update,
                ),
                Triple(
                    "whatsapp_diagnosi",
                    R.string.template_wa_diagnosis_title,
                    R.string.anchor_step_diagnosis,
                ),
                Triple(
                    "whatsapp_preventivo_intervento",
                    R.string.template_wa_quote_title,
                    R.string.anchor_step_quote,
                ),
                Triple(
                    "whatsapp_non_riparabile",
                    R.string.template_wa_not_repairable_title,
                    R.string.anchor_step_outcome,
                ),
                Triple(
                    "whatsapp_riparazione",
                    R.string.template_wa_repairing_title,
                    R.string.anchor_step_repair,
                ),
                Triple(
                    "whatsapp_pronto_ritiro",
                    R.string.template_wa_ready_title,
                    R.string.anchor_step_pickup,
                ),
                Triple(
                    "whatsapp_stato_avanzamento_pdf",
                    R.string.template_wa_pdf_status_title,
                    R.string.anchor_step_docs,
                ),
                Triple(
                    "whatsapp_completamento",
                    R.string.template_wa_completamento_title,
                    R.string.anchor_step_closed,
                ),
            )

        templates.forEach { template ->
            val code = template.first
            val titleRes = template.second
            val stepRes = template.third
            if (db.messageTemplateDao().findByCode(code) == null) {
                db.messageTemplateDao().upsert(
                    MessageTemplateEntity(
                        id = UUID.randomUUID().toString(),
                        code = code,
                        title = context.getString(titleRes),
                        channel = MessageChannel.WHATSAPP,
                        body = "Demo body for $code",
                        active = true,
                        version = 1,
                        updatedByUserId = "admin",
                        updatedAt = now,
                        step = context.getString(stepRes),
                    ),
                )
            }
        }
    }
}
