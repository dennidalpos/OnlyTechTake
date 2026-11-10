package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.core.templates.TemplateRenderer
import com.onlytechtake.repairapp.core.utils.formatEuroCentsForExport
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.*
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.data.orders.OrderWorkflowEventWriter
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.logic.nextActionLabel
import com.onlytechtake.repairapp.domain.models.*
import com.onlytechtake.repairapp.domain.repositories.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTemplateRepository
    @Inject
    constructor(
        private val context: Context,
        private val templateDao: MessageTemplateDao,
        private val orderDao: RepairOrderDao,
        private val customerDao: CustomerDao,
        private val categoryDao: OrderCategoryDao,
        private val settingDao: AppSettingDao,
        private val messageLogDao: MessageLogDao,
        private val timelineEventDao: TimelineEventDao,
        private val auditLogDao: AuditLogDao,
        private val ownerPreferences: OwnerPreferences,
        private val renderer: TemplateRenderer = TemplateRenderer(),
        private val clockMillis: () -> Long = System::currentTimeMillis,
    ) : TemplateRepository {
        private val eventWriter = OrderWorkflowEventWriter(context, timelineEventDao = timelineEventDao)

        override fun observeTemplates(): Flow<List<Template>> =
            templateDao.observeAll().map { list ->
                val defaults = getDefaultTemplates()
                list.map { templateEntity ->
                    val template =
                        if ((templateEntity.code in systemTemplateCodes) && (templateEntity.version == 1)) {
                            val default = defaults.find { it.code == templateEntity.code }
                            if (default != null) {
                                templateEntity.copy(
                                    title = default.title,
                                    body = default.body,
                                    step = default.step,
                                )
                            } else {
                                templateEntity
                            }
                        } else {
                            templateEntity
                        }
                    template.toDomain()
                }
            }

        override fun observeAnchorMappings(): Flow<Map<TemplateAnchor, String>> =
            settingDao.observeAll().map { settings ->
                TemplateAnchor.entries.associateWith { anchor ->
                    settings.find { it.key == anchor.settingKey }?.value ?: anchor.defaultCode
                }
            }

        override suspend fun setTemplateForAnchor(
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

        override fun observeMessageMode(): Flow<MessageMode> =
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

        override suspend fun setMessageMode(
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

        override suspend fun ensureDefaults(actorUserId: String?) {
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

        override suspend fun resetToDefaults(actor: AuthenticatedUser): Result<Unit> =
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

        override suspend fun saveTemplate(
            actor: AuthenticatedUser,
            code: String,
            title: String,
            body: String,
            active: Boolean,
            step: String?,
        ): Result<Unit> =
            runCatching {
                PermissionService.requirePermission(actor, UserPermission.MANAGE_TEMPLATES)
                require(code.isNotBlank()) { context.getString(R.string.error_template_code_required) }
                require(title.isNotBlank()) { context.getString(R.string.error_template_title_required) }
                require(body.isNotBlank()) { context.getString(R.string.error_template_body_required) }
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

        override suspend fun prepareMessage(
            actor: AuthenticatedUser,
            orderId: String,
            anchor: TemplateAnchor,
        ): Result<PreparedMessage> {
            val templateCode = resolveAnchor(anchor)
            return prepareMessage(actor, orderId, templateCode, anchor)
        }

        private suspend fun resolveAnchor(anchor: TemplateAnchor): String = settingDao.findByKey(anchor.settingKey)?.value ?: anchor.defaultCode

        override suspend fun prepareMessage(
            actor: AuthenticatedUser,
            orderId: String,
            templateCode: String,
            anchor: TemplateAnchor?,
        ): Result<PreparedMessage> =
            runCatching {
                val order =
                    requireNotNull(orderDao.findById(orderId)) { context.getString(R.string.error_order_not_found) }
                if (order.assignedToUserId != actor.id) {
                    PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
                } else {
                    PermissionService.requirePermission(actor, UserPermission.EDIT_OWN_ORDERS)
                }
                val customer =
                    requireNotNull(customerDao.findById(order.customerId)) { context.getString(R.string.error_customer_not_found) }
                val dbTemplate =
                    requireNotNull(templateDao.findActiveByCode(templateCode)) { context.getString(R.string.error_template_not_found) }
                val template =
                    if ((dbTemplate.code in systemTemplateCodes) && (dbTemplate.version == 1)) {
                        getDefaultTemplates().find { it.code == dbTemplate.code } ?: dbTemplate
                    } else {
                        dbTemplate
                    }
                val categoryPath = categoryPath(order.categoryId)
                val phone = customer.phone?.filter { it.isDigit() || (it == '+') }.orEmpty()
                require(phone.isNotBlank()) { context.getString(R.string.error_customer_phone_missing) }

                val owner = ownerPreferences.ownerInfo.firstOrNull()

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
                                    .ifBlank { context.getString(R.string.signature_default_signer) },
                            "esito_diagnosi" to order.diagnosisOutcome?.label(context).orEmpty(),
                            "importo_preventivo" to order.estimateAmountCents.formatEuroCentsForExport(),
                            "prossima_azione" to nextActionLabel(context, order.toDomain()),
                            "motivo_chiusura" to
                                when {
                                    order.closureReason != null -> {
                                        order.closureReason.label(context)
                                    }

                                    order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE -> {
                                        context.getString(R.string.template_diagnosis_not_repairable_reason)
                                    }

                                    else -> {
                                        ""
                                    }
                                },
                            "categoria_percorso" to categoryPath,
                            "ancora_label" to (anchor?.let { context.getString(it.labelRes) } ?: ""),
                            "ancora_id" to (anchor?.settingKey ?: ""),
                            "template_codice" to template.code,
                            "proprietario_nome" to (owner?.name ?: ""),
                            "proprietario_piva" to (owner?.vatNumber ?: ""),
                            "proprietario_indirizzo" to (owner?.address ?: ""),
                            "proprietario_email" to (owner?.email ?: ""),
                            "proprietario_telefono" to (owner?.phone ?: ""),
                        ),
                    )
                PreparedMessage(order.id, customer.id, phone, body, template.title)
            }

        override suspend fun recordPrepared(
            actor: AuthenticatedUser,
            prepared: PreparedMessage,
            body: String,
        ): Result<Unit> = writeMessage(actor, prepared, body, MessageStatus.PREPARED, null)

        override suspend fun confirmSent(
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
            while ((current != null) && (visited.add(current.id))) {
                chain += current
                current = current.parentId?.let { categoryDao.findById(it) }
            }
            return chain.asReversed().joinToString(" > ") { it.name }
        }

        private fun getDefaultTemplates() =
            listOf(
                MessageTemplateEntity(
                    id = "",
                    code = "privacy_short",
                    title = context.getString(R.string.template_privacy_short_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_privacy_short_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_legal),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "terms_intake",
                    title = context.getString(R.string.template_terms_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_terms_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_legal),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_intake",
                    title = context.getString(R.string.template_wa_intake_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_intake_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_intake),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_update",
                    title = context.getString(R.string.template_wa_update_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_update_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_update),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_diagnosis",
                    title = context.getString(R.string.template_wa_diagnosis_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_diagnosis_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_diagnosis),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_quote",
                    title = context.getString(R.string.template_wa_quote_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_quote_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_quote),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_not_repairable",
                    title = context.getString(R.string.template_wa_not_repairable_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_not_repairable_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_outcome),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_repairing",
                    title = context.getString(R.string.template_wa_repairing_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_repairing_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_repair),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_ready",
                    title = context.getString(R.string.template_wa_ready_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_ready_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_pickup),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_pdf_status",
                    title = context.getString(R.string.template_wa_pdf_status_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_pdf_status_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_docs),
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_completed",
                    title = context.getString(R.string.template_wa_completed_title),
                    channel = MessageChannel.WHATSAPP,
                    body = context.getString(R.string.template_wa_completed_body),
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = context.getString(R.string.anchor_step_closed),
                ),
            )

        private companion object {
            const val MESSAGE_MODE_SETTING_KEY = "message_mode"

            val systemTemplateCodes =
                setOf(
                    "privacy_short",
                    "terms_intake",
                    "whatsapp_intake",
                    "whatsapp_update",
                    "whatsapp_diagnosis",
                    "whatsapp_quote",
                    "whatsapp_not_repairable",
                    "whatsapp_repairing",
                    "whatsapp_ready",
                    "whatsapp_pdf_status",
                    "whatsapp_completed",
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
                    "proprietario_nome",
                    "proprietario_piva",
                    "proprietario_indirizzo",
                    "proprietario_email",
                    "proprietario_telefono",
                )
        }
    }
