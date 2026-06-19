package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.core.orders.OrderNumberGenerator
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.utils.capitalizeFirst
import com.onlytechtake.repairapp.core.utils.formatEuroCents
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerActionStatus
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.OrderClosurePermissionPolicy
import com.onlytechtake.repairapp.domain.workspace.OrderStatusPolicy
import com.onlytechtake.repairapp.domain.workspace.RepairOrderInput
import com.onlytechtake.repairapp.domain.workspace.normalizedEstimateAmountCents
import com.onlytechtake.repairapp.domain.workspace.validationErrors
import java.util.UUID

internal class OrderWorkflowCommands(
    private val context: Context,
    private val repairOrderDao: RepairOrderDao,
    private val customerDao: CustomerDao,
    private val categoryDao: OrderCategoryDao,
    private val messageLogDao: MessageLogDao,
    private val database: RepairAppDatabase?,
    private val orderNumberGenerator: OrderNumberGenerator,
    private val clockMillis: () -> Long,
    private val eventWriter: OrderWorkflowEventWriter,
) {
    suspend fun createOrder(
        actor: AuthenticatedUser,
        input: RepairOrderInput,
    ): Result<RepairOrderEntity> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.CREATE_ORDERS)
            inTransaction {
                require(input.customerId.isNotBlank()) { "Cliente obbligatorio" }
                require(input.title.isNotBlank()) { "Titolo obbligatorio" }
                require(input.categoryId.isNotBlank()) { "Categoria obbligatoria" }
                requireNotNull(customerDao.findById(input.customerId)) { "Cliente non trovato" }
                requireNotNull(categoryDao.findById(input.categoryId)) { "Categoria non trovata" }

                val now = clockMillis()
                val prefix = orderNumberGenerator.yearPrefix(now)
                val categoryPathSnapshot = buildCategoryPathSnapshot(input.categoryId)
                val order =
                    RepairOrderEntity(
                        id = UUID.randomUUID().toString(),
                        orderNumber = orderNumberGenerator.next(
                            prefix,
                            repairOrderDao.findLatestOrderNumberForYear(prefix)
                        ),
                        customerId = input.customerId,
                        title = input.title.capitalizeFirst(),
                        categoryId = input.categoryId,
                        categoryPathSnapshot = categoryPathSnapshot,
                        status = OrderStatus.OPEN,
                        priority = input.priority,
                        assignedToUserId = input.assignedToUserId ?: actor.id,
                        createdByUserId = actor.id,
                        description = input.description.capitalizeFirst().ifBlank { null },
                        internalNotes = null,
                        diagnosisOutcome = null,
                        estimateAmountCents = null,
                        finalAmountCents = null,
                        createdAt = now,
                        updatedAt = now,
                        closedAt = null,
                        closureReason = null,
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
                OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(actor, status)
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
                        closeNote = if (status in OrderStatusPolicy.closedStatuses) note.capitalizeFirst()
                            .ifBlank { null } else order.closeNote,
                    ),
                )
                eventWriter.statusChanged(
                    orderId = orderId,
                    actorUserId = actor.id,
                    oldStatus = order.status,
                    newStatus = status,
                    note = note.capitalizeFirst().ifBlank { null },
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
                val details =
                    listOfNotNull(
                        "Sintesi risultato: ${input.summary.capitalizeFirst()}",
                        "Esito diagnosi: ${input.diagnosisOutcome.label(context)}",
                        estimateAmountCents?.let { "Importo riparazione: ${it.formatEuroCents()}" },
                    ).joinToString("\n")

                val customerActionStatus =
                    when (input.diagnosisOutcome) {
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
                eventWriter.customerActionStatusChanged(
                    orderId,
                    actor.id,
                    customerActionStatus,
                    now
                )
            }
        }

    suspend fun reassign(
        actor: AuthenticatedUser,
        orderId: String,
        assignedToUserId: String?,
    ): Result<Unit> =
        runCatching {
            inTransaction {
                val order = requireWritableOrder(actor, orderId)
                if (order.assignedToUserId != actor.id) {
                    PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
                }

                val now = clockMillis()
                repairOrderDao.upsert(
                    order.copy(
                        assignedToUserId = assignedToUserId,
                        updatedAt = now
                    )
                )
                eventWriter.orderReassigned(orderId, actor.id, assignedToUserId, now)
            }
        }

    suspend fun addInternalNote(
        actor: AuthenticatedUser,
        orderId: String,
        note: String,
    ): Result<Unit> =
        runCatching {
            inTransaction {
                require(note.isNotBlank()) { "Nota vuota" }
                val order = requireWritableOrder(actor, orderId)
                val now = clockMillis()
                val normalizedNote = note.capitalizeFirst()
                val mergedNotes =
                    listOfNotNull(order.internalNotes, normalizedNote).joinToString("\n\n")
                repairOrderDao.upsert(order.copy(internalNotes = mergedNotes, updatedAt = now))
                eventWriter.internalNoteAdded(orderId, actor.id, normalizedNote, now)
            }
        }

    suspend fun recordFeedback(
        actor: AuthenticatedUser,
        orderId: String,
        input: FeedbackInput,
    ): Result<Unit> =
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
                        ->
                        require(order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                            "Accettazione o rifiuto sono disponibili solo per una diagnosi riparabile"
                        }

                    else -> Unit
                }
                val now = clockMillis()
                val targetStatus =
                    when (input.feedback) {
                        CustomerFeedbackType.ACCEPT -> OrderStatus.REPAIRING
                        CustomerFeedbackType.REJECT -> OrderStatus.CLOSED
                        else -> order.status
                    }
                val customerActionStatus =
                    when (input.feedback) {
                        CustomerFeedbackType.ACCEPT -> CustomerActionStatus.ACCEPTED
                        CustomerFeedbackType.REJECT -> CustomerActionStatus.REJECTED
                        CustomerFeedbackType.READ -> CustomerActionStatus.NOTIFIED
                        CustomerFeedbackType.ASK_INFO -> CustomerActionStatus.ASKED_INFO
                        CustomerFeedbackType.CALL_ME -> CustomerActionStatus.CALL_REQUESTED
                    }
                val closureReason =
                    input.closureReason ?: when (targetStatus) {
                        OrderStatus.CLOSED -> ClosureReason.CUSTOMER_DECLINED
                        else -> null
                    }
                if (targetStatus != order.status) {
                    OrderStatusPolicy.requireAllowed(order.status, targetStatus, closureReason)
                    OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(actor, targetStatus)
                }
                repairOrderDao.upsert(
                    order.copy(
                        status = targetStatus,
                        customerActionStatus = customerActionStatus,
                        closeNote = if (targetStatus == OrderStatus.CLOSED) input.note.capitalizeFirst() else order.closeNote,
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
                    note = input.note.capitalizeFirst().ifBlank { input.feedback.name },
                    targetStatus = targetStatus,
                    now = now,
                )
                if (customerActionStatus != order.customerActionStatus) {
                    eventWriter.customerActionStatusChanged(
                        orderId,
                        actor.id,
                        customerActionStatus,
                        now
                    )
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

    suspend fun recordPdfShared(
        actor: AuthenticatedUser,
        orderId: String,
        fileName: String,
    ): Result<Unit> =
        runCatching {
            inTransaction {
                requireWritableOrder(actor, orderId)
                val now = clockMillis()
                eventWriter.pdfShared(orderId, actor.id, fileName, now)
            }
        }

    suspend fun recordMediaRemoved(
        actor: AuthenticatedUser,
        orderId: String,
        fileName: String,
    ): Result<Unit> =
        runCatching {
            inTransaction {
                requireWritableOrder(actor, orderId)
                val now = clockMillis()
                eventWriter.mediaRemoved(orderId, actor.id, fileName, now)
            }
        }

    suspend fun togglePaymentStatus(
        actor: AuthenticatedUser,
        orderId: String,
        isPaid: Boolean,
    ): Result<Unit> =
        runCatching {
            inTransaction {
                requireWritableOrder(actor, orderId)
                val now = clockMillis()
                if (isPaid) {
                    eventWriter.paymentReceived(orderId, actor.id, now)
                } else {
                    eventWriter.paymentRemoved(orderId, actor.id, now)
                }
            }
        }

    suspend fun deleteOrder(
        actor: AuthenticatedUser,
        orderId: String,
        timelineEventDao: com.onlytechtake.repairapp.data.local.dao.TimelineEventDao,
        orderMediaDao: com.onlytechtake.repairapp.data.local.dao.OrderMediaDao,
        signatureDao: com.onlytechtake.repairapp.data.local.dao.SignatureDao,
        orderDocumentDao: com.onlytechtake.repairapp.data.local.dao.OrderDocumentDao,
        auditLogDao: com.onlytechtake.repairapp.data.local.dao.AuditLogDao,
        fileCleanupService: OrderFileCleanupService,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
            inTransaction {
                val order =
                    requireNotNull(repairOrderDao.findById(orderId)) { "Commessa non trovata" }
                val now = clockMillis()

                // 1. Delete actual files
                fileCleanupService.deleteOrderFiles(orderId)

                // 2. Delete database records
                timelineEventDao.deleteByOrderId(orderId)
                orderMediaDao.deleteByOrderId(orderId)
                signatureDao.deleteByOrderId(orderId)
                messageLogDao.deleteByOrderId(orderId)
                orderDocumentDao.deleteByOrderId(orderId)
                repairOrderDao.deleteById(orderId)

                // 3. Audit log
                auditLogDao.upsert(
                    com.onlytechtake.repairapp.data.local.entities.AuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        action = com.onlytechtake.repairapp.data.local.entities.AuditAction.ORDER_DELETED,
                        actorUserId = actor.id,
                        orderId = orderId,
                        customerId = order.customerId,
                        detailsJson = """{"orderNumber":"${order.orderNumber}","title":"${order.title}"}""",
                        createdAt = now,
                    ),
                )
            }
        }

    private suspend fun requireWritableOrder(
        actor: AuthenticatedUser,
        orderId: String,
    ): RepairOrderEntity {
        val order = requireNotNull(repairOrderDao.findById(orderId)) { "Commessa non trovata" }
        if (order.assignedToUserId != actor.id) {
            PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
        } else {
            PermissionService.requirePermission(actor, UserPermission.EDIT_OWN_ORDERS)
        }
        return order
    }

    private suspend fun <T> inTransaction(block: suspend () -> T): T =
        database?.withTransaction { block() } ?: block()

    private suspend fun buildCategoryPathSnapshot(categoryId: String): String? {
        val path = mutableListOf<String>()
        var currentId: String? = categoryId
        val visited = mutableSetOf<String>()

        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId)
            val category = categoryDao.findById(currentId) ?: break
            path.add(0, category.name)
            currentId = category.parentId
        }

        return if (path.isEmpty()) null else path.joinToString(" > ")
    }
}
