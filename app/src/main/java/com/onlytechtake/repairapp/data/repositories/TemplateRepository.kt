package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.templates.TemplateRenderer
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.MessageStatus
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import android.content.Context
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    fun observeTemplates(): Flow<List<MessageTemplateEntity>> = templateDao.observeAll()

    fun observeMessageMode(): Flow<MessageMode> =
        settingDao.observeAll().map { settings ->
            settings.firstOrNull { it.key == MESSAGE_MODE_SETTING_KEY }
                ?.value
                ?.let(MessageMode::valueOf)
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
            require((actor.role == UserRole.ADMIN) || (order.assignedToUserId == actor.id)) { "Operatore non assegnato" }
            val customer = requireNotNull(customerDao.findById(order.customerId)) { "Cliente non trovato" }
            val template = requireNotNull(templateDao.findActiveByCode(templateCode)) { "Template non trovato" }
            val categoryPath = categoryPath(order.categoryId)
            val phone = customer.phone?.filter { it.isDigit() || (it == '+') }.orEmpty()
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
                    "importo_preventivo" to order.estimateAmountCents?.let(::formatEuroCents).orEmpty(),
                    "prossima_azione" to order.nextActionLabel(),
                    "motivo_chiusura" to when {
                        order.closureReason != null -> order.closureReason.label()
                        order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE -> "Diagnosi tecnica: non riparabile"
                        else -> ""
                    },
                    "categoria_percorso" to categoryPath,
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
            val event = if (status == MessageStatus.SENT_CONFIRMED) {
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

    private companion object {
        const val MESSAGE_MODE_SETTING_KEY = "message_mode"
        val supportedVariables = setOf(
            "numero_commessa",
            "titolo_commessa",
            "stato_commessa",
            "cliente",
            "importo_preventivo",
            "prossima_azione",
            "motivo_chiusura",
            "categoria_percorso",
        )

        val defaultTemplates = listOf(
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_presa_in_carico",
                title = "WhatsApp presa in carico",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, abbiamo preso in carico la commessa {{numero_commessa}}: {{titolo_commessa}}.\nCategoria: {{categoria_percorso}}.\nTi aggiorneremo appena avremo novita'.",
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
                body = "Aggiornamento commessa {{numero_commessa}}: stato attuale {{stato_commessa}}.\nProssima azione: {{prossima_azione}}.",
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
                body = "Ciao {{cliente}}, la commessa {{numero_commessa}} e' in diagnosi.\nOggetto: {{titolo_commessa}}.\nTi comunicheremo esito e preventivo prima di procedere.",
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
                body = "Ciao {{cliente}}, abbiamo completato la diagnosi della commessa {{numero_commessa}}.\nStato: {{stato_commessa}}.\nProssima azione: {{prossima_azione}}.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_preventivo_intervento",
                title = "WhatsApp preventivo intervento",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, per la commessa {{numero_commessa}} abbiamo completato la diagnosi.\nPreventivo intervento: {{importo_preventivo}}.\nRispondi confermando o rifiutando il preventivo: procederemo solo dopo la tua conferma.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_non_riparabile",
                title = "WhatsApp non riparabile",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, dopo la diagnosi della commessa {{numero_commessa}} l'oggetto risulta non riparabile.\nPuoi passare in negozio per il ritiro o contattarci per chiarimenti.",
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
                body = "Ciao {{cliente}}, la riparazione della commessa {{numero_commessa}} e' in corso.\nTi aggiorneremo quando sara' pronta.",
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
                body = "Ciao {{cliente}}, la commessa {{numero_commessa}} e' in lavorazione.\nProssima azione: {{prossima_azione}}.",
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
                body = "Ciao {{cliente}}, la lavorazione della commessa {{numero_commessa}} e' completata.",
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
                body = "Ciao {{cliente}}, la commessa {{numero_commessa}} e' pronta per il ritiro.",
                active = true,
                version = 1,
                updatedByUserId = null,
                updatedAt = 0L,
            ),
            MessageTemplateEntity(
                id = "",
                code = "whatsapp_stato_avanzamento_pdf",
                title = "WhatsApp stato avanzamento PDF",
                channel = MessageChannel.WHATSAPP,
                body = "Ciao {{cliente}}, ti inviamo in allegato lo stato avanzamento della commessa {{numero_commessa}}: {{titolo_commessa}}.\n\nStato attuale: {{stato_commessa}}\nProssima azione: {{prossima_azione}}\n\nOnlyTechTake",
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
                body = "Informativa privacy sintetica ai sensi del Regolamento UE 2016/679 (GDPR).\nTitolare: OnlyTechTake, contattabile presso il punto vendita o i recapiti indicati nella ricevuta.\nFinalita': presa in carico e gestione della commessa, diagnosi, preventivo, riparazione, consegna, assistenza post-intervento, comunicazioni operative e adempimenti contabili/fiscali.\nBasi giuridiche: esecuzione di misure precontrattuali o contrattuali, obblighi di legge e legittimo interesse alla gestione organizzativa e alla tutela dei diritti. Per comunicazioni WhatsApp non indispensabili o ulteriori rispetto alla gestione della commessa puo' essere richiesto consenso specifico.\nDati trattati: dati identificativi e di contatto, dati fiscali se necessari, informazioni sull'oggetto consegnato, descrizione del problema, foto/allegati, firma, note operative e storico comunicazioni.\nConservazione: per il tempo necessario alla gestione della commessa e successivamente per obblighi di legge o tutela dei diritti.\nDestinatari: personale autorizzato, fornitori tecnici strettamente necessari, consulenti e soggetti previsti dalla legge. L'uso di WhatsApp comporta trattamento secondo le condizioni della piattaforma usata dal cliente.\nDiritti: accesso, rettifica, cancellazione nei casi previsti, limitazione, opposizione, portabilita' ove applicabile e reclamo al Garante per la protezione dei dati personali.",
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
                body = "Condizioni di presa in carico.\nIl cliente dichiara di consegnare l'oggetto indicato in commessa e autorizza OnlyTechTake a custodirlo, verificarlo e svolgere diagnosi tecnica preliminare.\nLa diagnosi puo' richiedere apertura, test, pulizia tecnica o collegamento ad alimentatori/strumenti compatibili. L'intervento di riparazione viene eseguito solo dopo accettazione del preventivo quando previsto.\nIl cliente si impegna a fornire informazioni corrette su proprieta', difetti noti, blocchi, accessori e condizioni dell'oggetto. OnlyTechTake non risponde di dati non salvati, password non comunicate, difetti preesistenti o peggioramenti inevitabili emersi durante diagnosi su oggetti gia' danneggiati.\nGli importi comunicati come preventivo devono essere accettati dal cliente prima dell'avvio intervento. In caso di rifiuto, impossibilita' di riparazione o mancato ritiro, la commessa puo' essere chiusa secondo le condizioni comunicate dall'operatore.\nCon la firma il cliente conferma presa visione di privacy e condizioni operative.",
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

private fun ClosureReason.label(): String =
    when (this) {
        ClosureReason.COMPLETED -> "Completata"
        ClosureReason.CUSTOMER_DECLINED -> "Rifiutata dal cliente"
        ClosureReason.NOT_REPAIRABLE -> "Non riparabile"
        ClosureReason.CANCELLED -> "Annullata"
        ClosureReason.OTHER -> "Altro"
    }

private fun RepairOrderEntity.nextActionLabel(): String =
    when (status) {
        OrderStatus.RESULT -> when (diagnosisOutcome) {
            DiagnosisOutcome.REPAIRABLE -> "Attendere accettazione o rifiuto preventivo"
            DiagnosisOutcome.NOT_REPAIRABLE -> "Avvisare il cliente e preparare il ritiro"
            null -> "Registrare esito diagnosi"
        }
        OrderStatus.OPEN -> "Prendere in carico"
        OrderStatus.IN_PROGRESS -> "Avviare diagnosi"
        OrderStatus.DIAGNOSIS -> "Registrare risultato diagnosi"
        OrderStatus.REPAIRING -> "Completare riparazione"
        OrderStatus.READY_FOR_PICKUP -> "Consegnare e chiudere"
        OrderStatus.CLOSED -> "Nessuna azione"
    }

private fun formatEuroCents(cents: Long): String =
    "EUR ${cents / 100},${(cents % 100).toString().padStart(2, '0')}"
