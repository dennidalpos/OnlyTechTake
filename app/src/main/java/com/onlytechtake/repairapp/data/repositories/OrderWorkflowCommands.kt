package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.R
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
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
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
    private val database: RepairAppDatabase,
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
            database.withTransaction {
                require(input.customerId.isNotBlank()) { context.getString(R.string.error_customer_required) }
                require(input.title.isNotBlank()) { context.getString(R.string.error_problem_required) }
                require(input.categoryId.isNotBlank()) { context.getString(R.string.error_category_required) }
                requireNotNull(customerDao.findById(input.customerId)) { context.getString(R.string.customer_no_results_title) }
                requireNotNull(categoryDao.findById(input.categoryId)) { context.getString(R.string.catalog_error_not_found) }

                val now = clockMillis()
                val prefix = orderNumberGenerator.yearPrefix(now)
                val categoryPathSnapshot = buildCategoryPathSnapshot(input.categoryId)
                val order =
                    RepairOrderEntity(
                        id = UUID.randomUUID().toString(),
                        orderNumber =
                            orderNumberGenerator.next(
                                prefix,
                                repairOrderDao.findLatestOrderNumberForYear(prefix),
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
            database.withTransaction {
                val order = requireWritableOrder(actor, orderId)
                if (order.status == status) return@withTransaction
                OrderStatusPolicy.requireAllowed(context, order.status, status, closureReason)
                OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(actor, status)
                if ((order.status == OrderStatus.RESULT) && (status == OrderStatus.REPAIRING)) {
                    require(order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                        context.getString(R.string.error_repairable_diagnosis_required)
                    }
                }
                if ((order.status == OrderStatus.RESULT) && (status == OrderStatus.READY_FOR_PICKUP)) {
                    require(order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE) {
                        context.getString(R.string.error_not_repairable_diagnosis_required)
                    }
                }

                val now = clockMillis()
                val isClosing = status in OrderStatusPolicy.closedStatuses
                repairOrderDao.upsert(
                    order.copy(
                        status = status,
                        updatedAt = now,
                        closedAt = if (isClosing) now else null,
                        closureReason = if (isClosing) closureReason else null,
                        closedByUserId = if (isClosing) actor.id else null,
                        closeNote =
                            if (isClosing) {
                                note
                                    .capitalizeFirst()
                                    .ifBlank { null }
                            } else {
                                null
                            },
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
            database.withTransaction {
                val errors = input.validationErrors(context)
                require(errors.isEmpty()) { errors.joinToString("\n") }
                val order = requireWritableOrder(actor, orderId)
                require((order.status == OrderStatus.DIAGNOSIS) || (order.status == OrderStatus.RESULT)) {
                    context.getString(R.string.error_diagnosis_only_during)
                }
                if (order.status != OrderStatus.RESULT) {
                    OrderStatusPolicy.requireAllowed(context, order.status, OrderStatus.RESULT)
                }

                val now = clockMillis()
                val estimateAmountCents = input.normalizedEstimateAmountCents()
                val details =
                    listOfNotNull(
                        context.getString(
                            R.string.timeline_result_summary,
                            input.summary.capitalizeFirst(),
                        ),
                        context.getString(
                            R.string.timeline_diagnosis_outcome,
                            input.diagnosisOutcome.label(context),
                        ),
                        estimateAmountCents?.let {
                            context.getString(
                                R.string.timeline_repair_amount,
                                it.formatEuroCents(),
                            )
                        },
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
                    now,
                )
            }
        }

    suspend fun reassign(
        actor: AuthenticatedUser,
        orderId: String,
        assignedToUserId: String?,
    ): Result<Unit> =
        runCatching {
            database.withTransaction {
                val order = requireWritableOrder(actor, orderId)
                if (order.assignedToUserId != actor.id) {
                    PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
                }

                val now = clockMillis()
                repairOrderDao.upsert(
                    order.copy(
                        assignedToUserId = assignedToUserId,
                        updatedAt = now,
                    ),
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
            database.withTransaction {
                require(note.isNotBlank()) { context.getString(R.string.error_note_empty) }
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
            database.withTransaction {
                val errors = input.validationErrors(context)
                require(errors.isEmpty()) { errors.joinToString("\n") }
                val order = requireWritableOrder(actor, orderId)
                require(order.status == OrderStatus.RESULT) {
                    context.getString(R.string.error_feedback_requires_result)
                }
                when (input.feedback) {
                    CustomerFeedbackType.ACCEPT,
                    CustomerFeedbackType.REJECT,
                        -> {
                        require(order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                            context.getString(R.string.error_feedback_only_repairable)
                        }
                    }

                    else -> {}
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
                    OrderStatusPolicy.requireAllowed(
                        context,
                        order.status,
                        targetStatus,
                        closureReason,
                    )
                    OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(actor, targetStatus)
                }
                val isClosing = targetStatus == OrderStatus.CLOSED
                repairOrderDao.upsert(
                    order.copy(
                        status = targetStatus,
                        customerActionStatus = customerActionStatus,
                        closeNote = if (isClosing) input.note.capitalizeFirst()
                            .ifBlank { null } else null,
                        closedByUserId = if (isClosing) actor.id else null,
                        updatedAt = now,
                        closedAt = if (isClosing) now else null,
                        closureReason = if (isClosing) closureReason else null,
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
                        now,
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
            database.withTransaction {
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
            database.withTransaction {
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
            database.withTransaction {
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
        fileCleanupService: OrderFileCleanupService,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
            database.withTransaction {
                val order =
                    requireNotNull(repairOrderDao.findById(orderId)) { context.getString(R.string.error_order_not_found) }
                val now = clockMillis()

                // 1. Delete actual files
                fileCleanupService.deleteOrderFiles(orderId)

                // 2. Delete database records
                database.timelineEventDao().deleteByOrderId(orderId)
                database.orderMediaDao().deleteByOrderId(orderId)
                database.signatureDao().deleteByOrderId(orderId)
                messageLogDao.deleteByOrderId(orderId)
                database.orderDocumentDao().deleteByOrderId(orderId)
                repairOrderDao.deleteById(orderId)

                // 3. Audit log
                database.auditLogDao().upsert(
                    AuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        action = AuditAction.ORDER_DELETED,
                        actorUserId = actor.id,
                        orderId = null, // Set to null because order is already deleted
                        customerId = order.customerId,
                        detailsJson = """{"orderNumber":"${order.orderNumber}","title":"${
                            order.title.replace(
                                "\"",
                                "\\\"",
                            )
                        }"}""",
                        createdAt = now,
                    ),
                )
            }
        }

    private suspend fun requireWritableOrder(
        actor: AuthenticatedUser,
        orderId: String,
    ): RepairOrderEntity {
        val order =
            requireNotNull(repairOrderDao.findById(orderId)) { context.getString(R.string.error_order_not_found) }
        if (order.assignedToUserId != actor.id) {
            PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
        } else {
            PermissionService.requirePermission(actor, UserPermission.EDIT_OWN_ORDERS)
        }
        return order
    }

    private suspend fun buildCategoryPathSnapshot(categoryId: String): String? {
        val path = mutableListOf<String>()
        var currentId: String? = categoryId
        val visited = mutableSetOf<String>()

        while ((currentId != null) && (currentId !in visited)) {
            visited.add(currentId)
            val category = categoryDao.findById(currentId) ?: break
            path.add(0, category.name)
            currentId = category.parentId
        }

        return if (path.isEmpty()) null else path.joinToString(context.getString(R.string.catalog_path_separator))
    }
}
