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

    fun observeTemplates(): Flow<List<MessageTemplateEntity>> = templateDao.observeAll()

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
            settings
                .firstOrNull { it.key == MESSAGE_MODE_SETTING_KEY }
                ?.value
                ?.let(MessageMode::valueOf)
                ?: MessageMode.MANUAL_WHATSAPP
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

    suspend fun resetToDefaults(actor: AuthenticatedUser): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_TEMPLATES)
            val now = clockMillis()
            defaultTemplates.forEach { template ->
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
            auditLogDao.upsert(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = AuditAction.TEMPLATE_UPDATED,
                    actorUserId = actor.id,
                    orderId = null,
                    customerId = null,
                    detailsJson = """{"action":"reset_to_defaults","templates":${defaultTemplates.size},"anchors":${TemplateAnchor.entries.size}}""",
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
            val template =
                requireNotNull(templateDao.findActiveByCode(templateCode)) { "Template non trovato" }
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
                                .ifBlank { "Cliente" },
                        "esito_diagnosi" to order.diagnosisOutcome?.label(context).orEmpty(),
                        "importo_preventivo" to order.estimateAmountCents.formatEuroCentsForExport(),
                        "prossima_azione" to nextActionLabel(order),
                        "motivo_chiusura" to
                            when {
                                order.closureReason != null -> order.closureReason.label(context)
                                order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE ->
                                    context.getString(com.onlytechtake.repairapp.R.string.template_diagnosis_not_repairable_reason)

                                else -> ""
                            },
                        "categoria_percorso" to categoryPath,
                        "ancora_label" to (anchor?.label ?: ""),
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

    private companion object {
        const val MESSAGE_MODE_SETTING_KEY = "message_mode"
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

        val defaultTemplates =
            listOf(
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
                    step = "Ingresso",
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
                    step = "Aggiornamento",
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
                    step = "Diagnosi",
                ),
                MessageTemplateEntity(
                    id = "",
                    code = "whatsapp_risultato",
                    title = "WhatsApp risultato",
                    channel = MessageChannel.WHATSAPP,
                    body = "Ciao {{cliente}}, abbiamo completato la diagnosi della commessa {{numero_commessa}}.\nEsito: {{esito_diagnosi}}.\nStato: {{stato_commessa}}.\nProssima azione: {{prossima_azione}}.",
                    active = true,
                    version = 1,
                    updatedByUserId = null,
                    updatedAt = 0L,
                    step = "Risultato",
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
                    step = "Preventivo",
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
                    step = "Non riparabile",
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
                    step = "Riparazione",
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
                    step = "Lavorazione",
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
                    step = "Chiusura",
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
                    step = "Pronto ritiro",
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
                    step = "PDF Stato",
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
                    step = "Legale",
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
                    step = "Legale",
                ),
            )
    }
}
