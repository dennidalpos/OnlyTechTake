package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.templates.TemplateRenderer
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.MessageStatus
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TemplateRepository(
    private val templateDao: MessageTemplateDao,
    private val orderDao: RepairOrderDao,
    private val customerDao: CustomerDao,
    private val settingDao: AppSettingDao,
    private val messageLogDao: MessageLogDao,
    private val timelineEventDao: TimelineEventDao,
    private val auditLogDao: AuditLogDao,
    private val renderer: TemplateRenderer = TemplateRenderer(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeTemplates(): Flow<List<MessageTemplateEntity>> = templateDao.observeAll()

    fun observeMessageMode(): Flow<MessageMode> =
        settingDao.observeAll().map { settings ->
            settings.firstOrNull { it.key == MESSAGE_MODE_SETTING_KEY }
                ?.value
                ?.let(::parseMessageMode)
                ?: MessageMode.MANUAL_WHATSAPP
        }

    suspend fun setMessageMode(actor: AuthenticatedUser, mode: MessageMode): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
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
        defaultTemplates.forEach { template ->
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
    }

    suspend fun saveTemplate(
        actor: AuthenticatedUser,
        code: String,
        title: String,
        body: String,
        active: Boolean,
    ): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
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

    suspend fun prepareMessage(actor: AuthenticatedUser, orderId: String, templateCode: String): Result<PreparedMessage> =
        runCatching {
            val order = requireNotNull(orderDao.findById(orderId)) { "Commessa non trovata" }
            require(actor.role == UserRole.ADMIN || order.assignedToUserId == actor.id) { "Operatore non assegnato" }
            val customer = requireNotNull(customerDao.findById(order.customerId)) { "Cliente non trovato" }
            val template = requireNotNull(templateDao.findActiveByCode(templateCode)) { "Template non trovato" }
            val phone = customer.phone?.filter { it.isDigit() || it == '+' }.orEmpty()
            require(phone.isNotBlank()) { "Telefono cliente mancante" }

            val body = renderer.render(
                template.body,
                mapOf(
                    "numero_commessa" to order.orderNumber,
                    "titolo_commessa" to order.title,
                    "stato_commessa" to order.status.label(),
                    "cliente" to listOfNotNull(customer.companyName, customer.firstName, customer.lastName)
                        .joinToString(" ")
                        .ifBlank { "Cliente" },
                ),
            )
            PreparedMessage(order.id, customer.id, phone, body)
        }

    suspend fun recordPrepared(actor: AuthenticatedUser, prepared: PreparedMessage, body: String): Result<Unit> =
        writeMessage(actor, prepared, body, MessageStatus.PREPARED, null)

    suspend fun confirmSent(actor: AuthenticatedUser, prepared: PreparedMessage, body: String): Result<Unit> =
        writeMessage(actor, prepared, body, MessageStatus.SENT_CONFIRMED, clockMillis())

    private suspend fun writeMessage(
        actor: AuthenticatedUser,
        prepared: PreparedMessage,
        body: String,
        status: MessageStatus,
        sentAt: Long?,
    ): Result<Unit> =
        runCatching {
            val now = clockMillis()
            val event = TimelineEventEntity(
                id = UUID.randomUUID().toString(),
                orderId = prepared.orderId,
                type = if (status == MessageStatus.SENT_CONFIRMED) {
                    TimelineEventType.WHATSAPP_SENT_CONFIRMED
                } else {
                    TimelineEventType.WHATSAPP_PREPARED
                },
                title = if (status == MessageStatus.SENT_CONFIRMED) "WhatsApp confermato" else "WhatsApp preparato",
                description = body,
                oldStatus = null,
                newStatus = null,
                visibleToCustomer = false,
                requiresCustomerFeedback = false,
                expectedFeedbackType = null,
                customerFeedback = null,
                customerFeedbackNote = null,
                customerFeedbackAt = null,
                createdByUserId = actor.id,
                createdAt = now,
            )
            timelineEventDao.upsert(event)
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

    private fun parseMessageMode(value: String?): MessageMode =
        when (value) {
            "MANUAL" -> MessageMode.MANUAL_WHATSAPP
            else -> MessageMode.MANUAL_WHATSAPP
        }

    private companion object {
        const val MESSAGE_MODE_SETTING_KEY = "message_mode"
        val supportedVariables = setOf(
            "numero_commessa",
            "titolo_commessa",
            "stato_commessa",
            "cliente",
        )

        val defaultTemplates = listOf(
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_presa_in_carico",
                title = "WhatsApp presa in carico",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, abbiamo preso in carico la commessa {{numero_commessa}}: {{titolo_commessa}}.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_aggiornamento",
                title = "WhatsApp aggiornamento",
                channel = MessageChannel.WHATSAPP,
                body = "Aggiornamento commessa {{numero_commessa}}: stato {{stato_commessa}}.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_diagnosi",
                title = "WhatsApp diagnosi",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, stiamo completando la diagnosi della commessa {{numero_commessa}}: {{titolo_commessa}}.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_risultato",
                title = "WhatsApp risultato",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, abbiamo registrato il risultato della commessa {{numero_commessa}}: {{titolo_commessa}}.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_riparazione",
                title = "WhatsApp riparazione",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, la riparazione della commessa {{numero_commessa}} e in corso.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_lavorazione",
                title = "WhatsApp lavorazione",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, la commessa {{numero_commessa}} e in lavorazione.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_completamento",
                title = "WhatsApp completamento",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, la lavorazione della commessa {{numero_commessa}} e completata.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_pronto_ritiro",
                title = "WhatsApp pronto ritiro",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, la commessa {{numero_commessa}} e pronta per il ritiro.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "privacy_breve",
                title = "Privacy breve",
                channel = MessageChannel.WHATSAPP,
                body = "I dati sono trattati solo per gestione riparazione, comunicazioni e obblighi fiscali.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "condizioni_presa_in_carico",
                title = "Condizioni presa in carico",
                channel = MessageChannel.WHATSAPP,
                body = "Il cliente autorizza la presa in carico del bene e accetta le condizioni operative comunicate.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
        )
    }
}

private fun OrderStatus.label(): String =
    when (this) {
        OrderStatus.OPEN -> "Aperta"
        OrderStatus.IN_PROGRESS -> "In lavorazione"
        OrderStatus.DIAGNOSIS -> "In diagnosi"
        OrderStatus.RESULT -> "Risultato"
        OrderStatus.REPAIRING -> "In riparazione"
        OrderStatus.READY_FOR_PICKUP -> "Pronta al ritiro"
        OrderStatus.CLOSED -> "Chiusa"
    }
