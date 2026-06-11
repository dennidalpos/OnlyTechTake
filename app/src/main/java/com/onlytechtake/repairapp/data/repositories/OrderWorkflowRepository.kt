package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.core.orders.OrderNumberGenerator
import com.onlytechtake.repairapp.core.utils.formatEuroCents
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.core.utils.capitalizeFirst
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.OrderMediaDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.SignatureDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerActionStatus
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import com.onlytechtake.repairapp.domain.workspace.discardedCleanupReasons
import com.onlytechtake.repairapp.domain.workspace.OrderStatusPolicy
import com.onlytechtake.repairapp.domain.workspace.RepairOrderInput
import com.onlytechtake.repairapp.domain.workspace.validationErrors
import com.onlytechtake.repairapp.domain.workspace.normalizedEstimateAmountCents
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class OrderWorkflowRepository(
    private val context: Context,
    private val repairOrderDao: RepairOrderDao,
    private val customerDao: CustomerDao,
    private val categoryDao: OrderCategoryDao,
    private val userDao: UserDao,
    private val timelineEventDao: TimelineEventDao,
    private val orderMediaDao: OrderMediaDao,
    private val signatureDao: SignatureDao,
    private val messageLogDao: MessageLogDao,
    private val auditLogDao: AuditLogDao,
    private val database: RepairAppDatabase? = null,
    private val orderNumberGenerator: OrderNumberGenerator = OrderNumberGenerator(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(context, timelineEventDao, auditLogDao)

    fun observeOrders(actor: AuthenticatedUser, filter: OrderFilter): Flow<List<RepairOrderEntity>> {
        val assignedToUserId = if (filter.showMineOnly) actor.id else null
        val status = filter.status.takeIf { filter.statuses.isEmpty() }
        val base = repairOrderDao.observeFiltered(
            includeDeleted = filter.showArchive,
            onlyDeleted = filter.showArchive,
            assignedToUserId = assignedToUserId,
            status = status,
            customerId = filter.customerId,
        )
        return if (filter.statuses.isEmpty()) base else base.map { orders ->
            orders.filter { it.status in filter.statuses }
        }
    }

    fun observeOrderList(actor: AuthenticatedUser, filter: OrderFilter): Flow<List<OrderListItem>> =
        combine(
            observeOrders(actor, filter),
            customerDao.observeAll(),
            orderMediaDao.observeAggregates(),
            signatureDao.observeAggregates(),
        ) { orders, customers, mediaAggregates, signatureAggregates ->
            val customersById = customers.associateBy { it.id }
            val mediaByOrder = mediaAggregates.associateBy { it.orderId }
            val signaturesByOrder = signatureAggregates.associateBy { it.orderId }
            orders.map { order ->
                val media = mediaByOrder[order.id]
                val signatures = signaturesByOrder[order.id]
                OrderListItem(
                    order = order,
                    customer = customersById[order.customerId],
                    attachments = OrderAttachmentCounts(
                        images = media?.images ?: 0,
                        documents = media?.documents ?: 0,
                        signatures = signatures?.signatures ?: 0,
                        finalPhotos = media?.finalPhotos ?: 0,
                    ),
                )
            }
        }

    fun observeOrderDetail(orderId: String): Flow<OrderDetail?> =
        combine(
            repairOrderDao.observeById(orderId),
            timelineEventDao.observeForOrder(orderId),
            messageLogDao.observeForOrder(orderId),
            orderMediaDao.observeForOrder(orderId),
        ) { order, timeline, messages, media ->
            order?.let { ObservedOrderData(it, timeline, messages, media) }
        }.combine(signatureDao.observeForOrder(orderId)) { data, signatures ->
            data?.let {
                val order = it.order
                OrderDetail(
                    order = order,
                    customer = customerDao.findById(order.customerId),
                    category = categoryDao.findById(order.categoryId),
                    assignedUser = order.assignedToUserId?.let { userId -> userDao.findById(userId) },
                    timeline = it.timeline,
                    messages = it.messages,
                    media = it.media,
                    signatures = signatures,
                )
            }
        }

    private data class ObservedOrderData(
        val order: RepairOrderEntity,
        val timeline: List<TimelineEventEntity>,
        val messages: List<MessageLogEntity>,
        val media: List<OrderMediaEntity>,
    )

    suspend fun createOrder(actor: AuthenticatedUser, input: RepairOrderInput): Result<RepairOrderEntity> =
        runCatching {
            inTransaction {
                require(input.customerId.isNotBlank()) { "Cliente obbligatorio" }
                require(input.title.isNotBlank()) { "Titolo obbligatorio" }
                require(input.categoryId.isNotBlank()) { "Categoria obbligatoria" }
                requireNotNull(customerDao.findById(input.customerId)) { "Cliente non trovato" }
                requireNotNull(categoryDao.findById(input.categoryId)) { "Categoria non trovata" }

                val now = clockMillis()
                val prefix = orderNumberGenerator.yearPrefix(now)
                val order = RepairOrderEntity(
                    id = UUID.randomUUID().toString(),
                    orderNumber = orderNumberGenerator.next(prefix, repairOrderDao.findLatestOrderNumberForYear(prefix)),
                    customerId = input.customerId,
                    title = input.title.trim().capitalizeFirst(),
                    categoryId = input.categoryId,
                    status = OrderStatus.OPEN,
                    priority = input.priority,
                    assignedToUserId = input.assignedToUserId ?: actor.id,
                    createdByUserId = actor.id,
                    description = input.description.trim().capitalizeFirst().ifBlank { null },
                    internalNotes = null,
                    diagnosisOutcome = null,
                    estimateAmountCents = null,
                    finalAmountCents = null,
                    createdAt = now,
                    updatedAt = now,
                    closedAt = null,
                    closureReason = null,
                    deletedAt = null,
                    deletedByUserId = null,
                    deleteReason = null,
                )

                repairOrderDao.upsert(order)
                eventWriter.orderCreated(order.id, actor.id, now)
                order
            }
        }

    suspend fun changeStatus(
        actor: AuthenticatedUser,
        orderId: String,
        status: OrderStatus,
        note: String = "",
        closureReason: ClosureReason? = null,
    ): Result<Unit> =
        runCatching {
            inTransaction {
                val order = requireWritableOrder(actor, orderId)
                if (order.status == status) return@inTransaction
                OrderStatusPolicy.requireAllowed(order.status, status, closureReason)
                if ((order.status == OrderStatus.RESULT) && (status == OrderStatus.REPAIRING)) {
                    require(order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                        "Solo una diagnosi riparabile puo' passare in riparazione"
                    }
                }
                if ((order.status == OrderStatus.RESULT) && (status == OrderStatus.READY_FOR_PICKUP)) {
                    require(order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE) {
                        "Il passaggio diretto al ritiro richiede una diagnosi non riparabile"
                    }
                }

                val now = clockMillis()
                repairOrderDao.upsert(
                    order.copy(
                        status = status,
                        updatedAt = now,
                        closedAt = if (status in OrderStatusPolicy.closedStatuses) now else order.closedAt,
                        closureReason = if (status in OrderStatusPolicy.closedStatuses) closureReason else null,
                        closedByUserId = if (status in OrderStatusPolicy.closedStatuses) actor.id else order.closedByUserId,
                        closeNote = if (status in OrderStatusPolicy.closedStatuses) note.trim().capitalizeFirst().ifBlank { null } else order.closeNote,
                    ),
                )
                eventWriter.statusChanged(
                    orderId = orderId,
                    actorUserId = actor.id,
                    oldStatus = order.status,
                    newStatus = status,
                    note = note.trim().capitalizeFirst().ifBlank { null },
                    closureReason = closureReason,
                    now = now,
                )
            }
        }

    suspend fun recordResult(
        actor: AuthenticatedUser,
        orderId: String,
        input: DiagnosisResultInput,
    ): Result<Unit> =
        runCatching {
            inTransaction {
                val errors = input.validationErrors()
                require(errors.isEmpty()) { errors.joinToString("\n") }
                val order = requireWritableOrder(actor, orderId)
                require((order.status == OrderStatus.DIAGNOSIS) || (order.status == OrderStatus.RESULT)) {
                    "La diagnosi puo' essere registrata solo durante o dopo la diagnosi"
                }
                if (order.status != OrderStatus.RESULT) {
                    OrderStatusPolicy.requireAllowed(order.status, OrderStatus.RESULT)
                }

                val now = clockMillis()
                val estimateAmountCents = input.normalizedEstimateAmountCents()
                val details = listOfNotNull(
                    "Sintesi risultato: ${input.summary.trim().capitalizeFirst()}",
                    "Esito diagnosi: ${input.diagnosisOutcome.label(context)}",
                    estimateAmountCents?.let { "Importo riparazione: ${it.formatEuroCents()}" },
                ).joinToString("\n")

                val customerActionStatus = when (input.diagnosisOutcome) {
                    DiagnosisOutcome.REPAIRABLE -> CustomerActionStatus.WAITING_ACCEPTANCE
                    DiagnosisOutcome.NOT_REPAIRABLE -> CustomerActionStatus.TO_NOTIFY
                }

                repairOrderDao.upsert(
                    order.copy(
                        status = OrderStatus.RESULT,
                        diagnosisOutcome = input.diagnosisOutcome,
                        estimateAmountCents = estimateAmountCents,
                        customerActionStatus = customerActionStatus,
                        updatedAt = now,
                    ),
                )
                eventWriter.statusChanged(
                    orderId = orderId,
                    actorUserId = actor.id,
                    oldStatus = order.status,
                    newStatus = OrderStatus.RESULT,
                    note = details,
                    closureReason = null,
                    now = now,
                )
                eventWriter.customerActionStatusChanged(orderId, actor.id, customerActionStatus, now)
            }
        }

    suspend fun reassign(actor: AuthenticatedUser, orderId: String, assignedToUserId: String?): Result<Unit> =
        runCatching {
            inTransaction {
                val order = requireWritableOrder(actor, orderId)
                if (actor.role != UserRole.ADMIN) {
                    require(order.assignedToUserId == actor.id) { "Solo la propria commessa puo' essere riassegnata" }
                }

                val now = clockMillis()
                repairOrderDao.upsert(order.copy(assignedToUserId = assignedToUserId, updatedAt = now))
                eventWriter.orderReassigned(orderId, actor.id, assignedToUserId, now)
            }
        }

    suspend fun addInternalNote(actor: AuthenticatedUser, orderId: String, note: String): Result<Unit> =
        runCatching {
            inTransaction {
                require(note.isNotBlank()) { "Nota vuota" }
                val order = requireWritableOrder(actor, orderId)
                val now = clockMillis()
                val normalizedNote = note.trim().capitalizeFirst()
                val mergedNotes = listOfNotNull(order.internalNotes, normalizedNote).joinToString("\n\n")
                repairOrderDao.upsert(order.copy(internalNotes = mergedNotes, updatedAt = now))
                eventWriter.internalNoteAdded(orderId, actor.id, normalizedNote, now)
            }
        }

    suspend fun softDelete(actor: AuthenticatedUser, orderId: String, reason: String): Result<Unit> =
        runCatching {
            inTransaction {
                require(actor.role == UserRole.ADMIN) { "Solo admin" }
                val order = requireNotNull(repairOrderDao.findById(orderId)) { "Commessa non trovata" }
                val now = clockMillis()
                repairOrderDao.upsert(
                    order.copy(
                        deletedAt = now,
                        deletedByUserId = actor.id,
                        deleteReason = reason.trim().ifBlank { null },
                        updatedAt = now,
                    ),
                )
                eventWriter.orderDeleted(orderId, actor.id, reason.trim().ifBlank { null }, now)
            }
        }

    suspend fun archiveDiscardedClosedOrders(actor: AuthenticatedUser): Result<Int> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            val orders = repairOrderDao.findClosedByClosureReasons(discardedCleanupReasons)
            if (orders.isEmpty()) return@runCatching 0

            val now = clockMillis()
            inTransaction {
                orders.forEach { order ->
                    repairOrderDao.upsert(
                        order.copy(
                            deletedAt = now,
                            deletedByUserId = actor.id,
                            deleteReason = "Pulizia commesse scartate",
                            updatedAt = now,
                        ),
                    )
                    eventWriter.orderDeleted(order.id, actor.id, "Pulizia commesse scartate", now)
                }
                auditCleanup(actor.id, "archive_discarded", orders.size, now)
            }
            orders.size
        }

    suspend fun deleteArchivedOrders(actor: AuthenticatedUser): Result<Int> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            val orders = repairOrderDao.findArchived()
            if (orders.isEmpty()) return@runCatching 0

            val orderIds = orders.map { it.id }
            val mediaFiles = orderMediaDao.findForOrders(orderIds).map { it.localPath }
            val signatureFiles = signatureDao.findForOrders(orderIds).map { it.localPath }
            val deletedCount = inTransaction {
                val count = repairOrderDao.deleteByIds(orderIds)
                auditCleanup(actor.id, "delete_archived", count, clockMillis())
                count
            }
            (mediaFiles + signatureFiles).forEach(::deleteFileQuietly)
            deletedCount
        }

    suspend fun recordFeedback(actor: AuthenticatedUser, orderId: String, input: FeedbackInput): Result<Unit> =
        runCatching {
            inTransaction {
                val errors = input.validationErrors()
                require(errors.isEmpty()) { errors.joinToString("\n") }
                val order = requireWritableOrder(actor, orderId)
                require(order.status == OrderStatus.RESULT) {
                    "Il feedback preventivo richiede una commessa in attesa cliente"
                }
                when (input.feedback) {
                    CustomerFeedbackType.ACCEPT,
                    CustomerFeedbackType.REJECT,
                    -> require(order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                        "Accettazione o rifiuto sono disponibili solo per una diagnosi riparabile"
                    }
                    else -> Unit
                }
                val now = clockMillis()
                val targetStatus = when (input.feedback) {
                    CustomerFeedbackType.ACCEPT -> OrderStatus.REPAIRING
                    CustomerFeedbackType.REJECT -> OrderStatus.CLOSED
                    else -> order.status
                }
                val customerActionStatus = when (input.feedback) {
                    CustomerFeedbackType.ACCEPT -> CustomerActionStatus.ACCEPTED
                    CustomerFeedbackType.REJECT -> CustomerActionStatus.REJECTED
                    CustomerFeedbackType.READ -> CustomerActionStatus.NOTIFIED
                    CustomerFeedbackType.ASK_INFO -> CustomerActionStatus.ASKED_INFO
                    CustomerFeedbackType.CALL_ME -> CustomerActionStatus.CALL_REQUESTED
                }
                val closureReason = input.closureReason ?: when (targetStatus) {
                    OrderStatus.CLOSED -> ClosureReason.CUSTOMER_DECLINED
                    else -> null
                }
                if (targetStatus != order.status) {
                    OrderStatusPolicy.requireAllowed(order.status, targetStatus, closureReason)
                }
                repairOrderDao.upsert(
                    order.copy(
                        status = targetStatus,
                        customerActionStatus = customerActionStatus,
                        closeNote = if (targetStatus == OrderStatus.CLOSED) input.note.trim().capitalizeFirst() else order.closeNote,
                        closedByUserId = if (targetStatus == OrderStatus.CLOSED) actor.id else order.closedByUserId,
                        updatedAt = now,
                        closedAt = if (targetStatus == OrderStatus.CLOSED) now else order.closedAt,
                        closureReason = if (targetStatus == OrderStatus.CLOSED) closureReason else order.closureReason,
                    ),
                )
                eventWriter.feedbackRecorded(
                    orderId = orderId,
                    actorUserId = actor.id,
                    feedback = input.feedback,
                    note = input.note.trim().capitalizeFirst().ifBlank { input.feedback.name },
                    targetStatus = targetStatus,
                    now = now,
                )
                if (customerActionStatus != order.customerActionStatus) {
                    eventWriter.customerActionStatusChanged(orderId, actor.id, customerActionStatus, now)
                }
                messageLogDao.findLatestForOrder(orderId)?.let { latest ->
                    messageLogDao.upsert(
                        latest.copy(
                            actualReply = input.feedback,
                            actualReplyNote = input.note.trim().ifBlank { input.feedback.name },
                        ),
                    )
                }
            }
        }

    suspend fun recordPdfShared(actor: AuthenticatedUser, orderId: String, fileName: String): Result<Unit> =
        runCatching {
            inTransaction {
                requireWritableOrder(actor, orderId)
                val now = clockMillis()
                eventWriter.pdfShared(orderId, actor.id, fileName, now)
            }
        }

    suspend fun recordMediaRemoved(actor: AuthenticatedUser, orderId: String, fileName: String): Result<Unit> =
        runCatching {
            inTransaction {
                requireWritableOrder(actor, orderId)
                val now = clockMillis()
                eventWriter.mediaRemoved(orderId, actor.id, fileName, now)
            }
        }

    private suspend fun requireWritableOrder(actor: AuthenticatedUser, orderId: String): RepairOrderEntity {
        val order = requireNotNull(repairOrderDao.findById(orderId)) { "Commessa non trovata" }
        require(order.deletedAt == null) { "Commessa archiviata" }
        require((actor.role == UserRole.ADMIN) || (order.assignedToUserId == actor.id)) {
            "Operatore non assegnato"
        }
        return order
    }

    private suspend fun <T> inTransaction(block: suspend () -> T): T =
        database?.withTransaction { block() } ?: block()

    private suspend fun auditCleanup(actorUserId: String, operation: String, count: Int, now: Long) {
        auditLogDao.upsert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = AuditAction.ORDER_DELETED,
                actorUserId = actorUserId,
                orderId = null,
                customerId = null,
                detailsJson = """{"operation":"$operation","count":$count}""",
                createdAt = now,
            ),
        )
    }

    private fun deleteFileQuietly(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        file.parentFile?.deleteIfEmpty()
        file.parentFile?.parentFile?.deleteIfEmpty()
    }

    private fun File.deleteIfEmpty() {
        if (exists() && isDirectory && listFiles().isNullOrEmpty()) {
            delete()
        }
    }

}
