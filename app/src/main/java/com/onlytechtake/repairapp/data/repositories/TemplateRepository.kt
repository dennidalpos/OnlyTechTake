package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.core.templates.TemplateRenderer
import com.onlytechtake.repairapp.core.utils.formatEuroCentsForExport
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.MessageStatus
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import com.onlytechtake.repairapp.domain.workspace.nextActionLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TemplateRepository(
    private val context: Context,
    private val templateDao: MessageTemplateDao,
    private val orderDao: RepairOrderDao,
    private val customerDao: CustomerDao,
    private val categoryDao: OrderCategoryDao,
    private val settingDao: AppSettingDao,
    private val messageLogDao: MessageLogDao,
    private val timelineEventDao: TimelineEventDao,
    private val auditLogDao: AuditLogDao,
    private val renderer: TemplateRenderer = TemplateRenderer(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(context, timelineEventDao = timelineEventDao)

    fun observeTemplates(): Flow<List<MessageTemplateEntity>> =
        templateDao.observeAll().map { list ->
            val defaults = getDefaultTemplates()
            list.map { template ->
                if ((template.code in systemTemplateCodes) && (template.version == 1)) {
                    val default = defaults.find { it.code == template.code }
                    if (default != null) {
                        template.copy(
                            title = default.title,
                            body = default.body,
                            step = default.step,
                        )
                    } else {
                        template
                    }
                } else {
                    template
                }
            }
        }

    fun observeAnchorMappings(): Flow<Map<TemplateAnchor, String>> =
        settingDao.observeAll().map { settings ->
            TemplateAnchor.entries.associateWith { anchor ->
                settings.find { it.key == anchor.settingKey }?.value ?: anchor.defaultCode
            }
        }

    suspend fun setTemplateForAnchor(
        actor: AuthenticatedUser,
        anchor: TemplateAnchor,
        templateCode: String,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_TEMPLATES)
            require(templateDao.findByCode(templateCode) != null) { "Template non trovato" }
            settingDao.upsert(
                AppSettingEntity(
                    key = anchor.settingKey,
                    value = templateCode,
                    updatedByUserId = actor.id,
                    updatedAt = clockMillis(),
                ),
            )
        }

    fun observeMessageMode(): Flow<MessageMode> =
        settingDao.observeAll().map { settings ->
            val value = settings.firstOrNull { it.key == MESSAGE_MODE_SETTING_KEY }?.value
            if (value != null) {
                try {
                    MessageMode.valueOf(value)
                } catch (_: Exception) {
                    MessageMode.MANUAL_WHATSAPP
                }
            } else {
                MessageMode.MANUAL_WHATSAPP
            }
        }

    suspend fun setMessageMode(
        actor: AuthenticatedUser,
        mode: MessageMode,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_TEMPLATES)
            settingDao.upsert(
                AppSettingEntity(
                    key = MESSAGE_MODE_SETTING_KEY,
                    value = mode.name,
                    updatedByUserId = actor.id,
                    updatedAt = clockMillis(),
                ),
            )
        }

    suspend fun ensureDefaults(actorUserId: String?) {
        val now = clockMillis()
        // 1. Ensure all system templates exist in the database
        getDefaultTemplates().forEach { template ->
            if (templateDao.findByCode(template.code) == null) {
                templateDao.upsert(
                    template.copy(
                        id = UUID.randomUUID().toString(),
                        updatedByUserId = actorUserId,
                        updatedAt = now,
                    ),
                )
            }
        }
        // 2. Ensure all activators (anchors) have a default mapping explicitly set in settings
        TemplateAnchor.entries.forEach { anchor ->
            if (settingDao.findByKey(anchor.settingKey) == null) {
                settingDao.upsert(
                    AppSettingEntity(
                        key = anchor.settingKey,
                        value = anchor.defaultCode,
                        updatedByUserId = actorUserId,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    suspend fun resetToDefaults(actor: AuthenticatedUser): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_TEMPLATES)
            val now = clockMillis()
            val defaults = getDefaultTemplates()
            defaults.forEach { template ->
                val existing = templateDao.findByCode(template.code)
                templateDao.upsert(
                    template.copy(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        active = true,
                        version = (existing?.version ?: 0) + 1,
                        updatedByUserId = actor.id,
                        updatedAt = now,
                    ),
                )
            }
            settingDao.deleteByKeys(TemplateAnchor.entries.map { it.settingKey })
            ensureDefaults(actor.id)
            auditLogDao.upsert(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = AuditAction.TEMPLATE_UPDATED,
                    actorUserId = actor.id,
                    orderId = null,
                    customerId = null,
                    detailsJson = """{"action":"reset_to_defaults","templates":${defaults.size},"anchors":${TemplateAnchor.entries.size}}""",
                    createdAt = now,
                ),
            )
        }

    suspend fun saveTemplate(
        actor: AuthenticatedUser,
        code: String,
        title: String,
        body: String,
        active: Boolean,
        step: String? = null,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_TEMPLATES)
            require(code.isNotBlank()) { "Codice template obbligatorio" }
            require(title.isNotBlank()) { "Titolo template obbligatorio" }
            require(body.isNotBlank()) { "Corpo template obbligatorio" }
            renderer.requireSupportedVariables(body, supportedVariables)

            val existing = templateDao.findByCode(code.trim())
            val now = clockMillis()
            templateDao.upsert(
                MessageTemplateEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    code = code.trim(),
                    title = title.trim(),
                    channel = MessageChannel.WHATSAPP,
                    body = body,
                    active = active,
                    version = (existing?.version ?: 0) + 1,
                    updatedByUserId = actor.id,
                    updatedAt = now,
                    step = step?.trim(),
                ),
            )
            auditLogDao.upsert(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = if (code.startsWith("privacy")) AuditAction.PRIVACY_TEXT_UPDATED else AuditAction.TEMPLATE_UPDATED,
                    actorUserId = actor.id,
                    orderId = null,
                    customerId = null,
                    detailsJson = """{"code":"${code.trim()}"}""",
                    createdAt = now,
                ),
            )
        }

    suspend fun prepareMessage(
        actor: AuthenticatedUser,
        orderId: String,
        anchor: TemplateAnchor,
    ): Result<PreparedMessage> {
        val templateCode = resolveAnchor(anchor)
        return prepareMessage(actor, orderId, templateCode, anchor)
    }

    private suspend fun resolveAnchor(anchor: TemplateAnchor): String = settingDao.findByKey(anchor.settingKey)?.value ?: anchor.defaultCode

    suspend fun prepareMessage(
        actor: AuthenticatedUser,
        orderId: String,
        templateCode: String,
        anchor: TemplateAnchor? = null,
    ): Result<PreparedMessage> =
        runCatching {
            val order = requireNotNull(orderDao.findById(orderId)) { "Commessa non trovata" }
            if (order.assignedToUserId != actor.id) {
                PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
            } else {
                PermissionService.requirePermission(actor, UserPermission.EDIT_OWN_ORDERS)
            }
            val customer =
                requireNotNull(customerDao.findById(order.customerId)) { "Cliente non trovato" }
            val dbTemplate =
                requireNotNull(templateDao.findActiveByCode(templateCode)) { "Template non trovato" }
            val template =
                if (dbTemplate.code in systemTemplateCodes && dbTemplate.version == 1) {
                    getDefaultTemplates().find { it.code == dbTemplate.code } ?: dbTemplate
                } else {
                    dbTemplate
                }
            val categoryPath = categoryPath(order.categoryId)
            val phone = customer.phone?.filter { it.isDigit() || (it == '+') }.orEmpty()
            require(phone.isNotBlank()) { "Telefono cliente mancante" }

            val body =
                renderer.render(
                    template.body,
                    mapOf(
                        "numero_commessa" to order.orderNumber,
                        "titolo_commessa" to order.title,
                        "stato_commessa" to order.status.label(context),
                        "cliente" to
                            listOfNotNull(
                                customer.companyName,
                                customer.firstName,
                                customer.lastName,
                            ).joinToString(" ")
                                .ifBlank { context.getString(com.onlytechtake.repairapp.R.string.signature_default_signer) },
                        "esito_diagnosi" to order.diagnosisOutcome?.label(context).orEmpty(),
                        "importo_preventivo" to order.estimateAmountCents.formatEuroCentsForExport(),
                        "prossima_azione" to nextActionLabel(context, order),
                        "motivo_chiusura" to
                            when {
                                order.closureReason != null -> order.closureReason.label(context)
                                order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE ->
                                    context.getString(com.onlytechtake.repairapp.R.string.template_diagnosis_not_repairable_reason)

                                else -> ""
                            },
                        "categoria_percorso" to categoryPath,
                        "ancora_label" to (anchor?.let { context.getString(it.labelRes) } ?: ""),
                        "ancora_id" to (anchor?.settingKey ?: ""),
                        "template_codice" to template.code,
                    ),
                )
            PreparedMessage(order.id, customer.id, phone, body, template.title)
        }

    suspend fun recordPrepared(
        actor: AuthenticatedUser,
        prepared: PreparedMessage,
        body: String,
    ): Result<Unit> = writeMessage(actor, prepared, body, MessageStatus.PREPARED, null)

    suspend fun confirmSent(
        actor: AuthenticatedUser,
        prepared: PreparedMessage,
        body: String,
    ): Result<Unit> = writeMessage(actor, prepared, body, MessageStatus.SENT_CONFIRMED, clockMillis())

    private suspend fun writeMessage(
        actor: AuthenticatedUser,
        prepared: PreparedMessage,
        body: String,
        status: MessageStatus,
        sentAt: Long?,
    ): Result<Unit> =
        runCatching {
            val now = clockMillis()
            val event =
                if (status == MessageStatus.SENT_CONFIRMED) {
                    eventWriter.whatsappSent(prepared.orderId, actor.id, body, now)
                } else {
                    eventWriter.whatsappPrepared(prepared.orderId, actor.id, body, now)
                }
            messageLogDao.upsert(
                MessageLogEntity(
                    id = UUID.randomUUID().toString(),
                    orderId = prepared.orderId,
                    timelineEventId = event.id,
                    customerId = prepared.customerId,
                    channel = MessageChannel.WHATSAPP,
                    mode = MessageMode.MANUAL_WHATSAPP,
                    body = body,
                    status = status,
                    expectedReply = CustomerFeedbackType.READ,
                    actualReply = null,
                    actualReplyNote = null,
                    createdByUserId = actor.id,
                    createdAt = now,
                    sentAt = sentAt,
                ),
            )
        }

    private suspend fun categoryPath(categoryId: String): String {
        val chain = mutableListOf<OrderCategoryEntity>()
        val visited = mutableSetOf<String>()
        var current = categoryDao.findById(categoryId)
        while (current != null && visited.add(current.id)) {
            chain += current
            current = current.parentId?.let { categoryDao.findById(it) }
        }
        return chain.asReversed().joinToString(" > ") { it.name }
    }

    private fun getDefaultTemplates() =
        listOf(
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_presa_in_carico",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_intake_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_intake_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_intake),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_aggiornamento",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_update_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_update_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_update),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_diagnosi",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_diagnosis_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_diagnosis_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_diagnosis),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_risultato",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_result_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_result_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_outcome),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_preventivo_intervento",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_quote_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_quote_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_quote),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_non_riparabile",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_not_repairable_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_not_repairable_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_outcome),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_riparazione",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_repairing_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_repairing_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_repair),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_lavorazione",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_lavorazione_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_lavorazione_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_update),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_completamento",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_completamento_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_completamento_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_closed),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_pronto_ritiro",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_ready_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_ready_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_pickup),
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_stato_avanzamento_pdf",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_wa_pdf_status_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_wa_pdf_status_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_docs),
            ),
            MessageTemplateEntity(
                id = "",
                code = "privacy_breve",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_privacy_breve_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_privacy_breve_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_legal),
            ),
            MessageTemplateEntity(
                id = "",
                code = "condizioni_presa_in_carico",
                title = context.getString(com.onlytechtake.repairapp.R.string.template_condizioni_title),
                channel = MessageChannel.WHATSAPP,
                body = context.getString(com.onlytechtake.repairapp.R.string.template_condizioni_body),
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
                step = context.getString(com.onlytechtake.repairapp.R.string.anchor_step_legal),
            ),
        )

    private companion object {
        const val MESSAGE_MODE_SETTING_KEY = "message_mode"

        val systemTemplateCodes =
            setOf(
                "privacy_breve",
                "condizioni_presa_in_carico",
                "whatsapp_presa_in_carico",
                "whatsapp_aggiornamento",
                "whatsapp_diagnosi",
                "whatsapp_risultato",
                "whatsapp_preventivo_intervento",
                "whatsapp_non_riparabile",
                "whatsapp_riparazione",
                "whatsapp_lavorazione",
                "whatsapp_completamento",
                "whatsapp_pronto_ritiro",
                "whatsapp_stato_avanzamento_pdf",
            )

        val supportedVariables =
            setOf(
                "numero_commessa",
                "titolo_commessa",
                "stato_commessa",
                "cliente",
                "esito_diagnosi",
                "importo_preventivo",
                "prossima_azione",
                "motivo_chiusura",
                "categoria_percorso",
                "ancora_label",
                "ancora_id",
                "template_codice",
            )
    }
}
