package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.orders.OrderNumberGenerator
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.utils.capitalizeFirst
import com.onlytechtake.repairapp.core.utils.formatEuroCents
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.*
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.data.orders.OrderFileCleanupService
import com.onlytechtake.repairapp.data.orders.OrderWorkflowEventWriter
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.logic.OrderClosurePermissionPolicy
import com.onlytechtake.repairapp.domain.logic.OrderStatusPolicy
import com.onlytechtake.repairapp.domain.models.*
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultOrderRepository
    @Inject
    constructor(
        private val context: Context,
        private val repairOrderDao: RepairOrderDao,
        private val customerDao: CustomerDao,
        private val categoryDao: OrderCategoryDao,
        private val timelineEventDao: TimelineEventDao,
        private val signatureDao: SignatureDao,
        private val messageLogDao: MessageLogDao,
        private val auditLogDao: AuditLogDao,
        private val database: RepairAppDatabase,
        private val fileCleanupService: OrderFileCleanupService,
        private val orderNumberGenerator: OrderNumberGenerator = OrderNumberGenerator(),
        private val clockMillis: () -> Long = { System.currentTimeMillis() },
    ) : OrderRepository {
        private val eventWriter =
            OrderWorkflowEventWriter(
                context = context,
                timelineEventDao = timelineEventDao,
                auditLogDao = auditLogDao,
            )

        override fun observeOrders(
            actor: AuthenticatedUser,
            filter: OrderFilter,
        ): Flow<List<Order>> =
            repairOrderDao
                .observeFiltered(
                    status = filter.status,
                    customerId = filter.customerId,
                    useStatuses = filter.statuses.isNotEmpty(),
                    statuses = filter.statuses.toList(),
                ).distinctUntilChanged()
                .map { list -> list.map { it.toDomain() } }

        override suspend fun countAllOrders(): Int = repairOrderDao.countAll()

        override fun searchOrders(
            actor: AuthenticatedUser,
            filter: OrderFilter,
        ): Flow<List<OrderListItem>> =
            repairOrderDao
                .search(
                    query = filter.query,
                    status = filter.status,
                    customerId = filter.customerId,
                    useStatuses = filter.statuses.isNotEmpty(),
                    statuses = filter.statuses.toList(),
                ).distinctUntilChanged()
                .map { list ->
                    list.map { projection ->
                        OrderListItem(
                            order = projection.order.toDomain(),
                            customer = projection.customer?.toDomain(),
                            attachments =
                                OrderAttachmentCounts(
                                    images = projection.imagesCount,
                                    videos = projection.videosCount,
                                    documents = projection.documentsCount,
                                    signatures = projection.signaturesCount,
                                    finalPhotos = projection.finalPhotosCount,
                                ),
                        )
                    }
                }

        override fun observeOrderList(
            actor: AuthenticatedUser,
            filter: OrderFilter,
        ): Flow<List<OrderListItem>> =
            repairOrderDao
                .observeOrderListProjection(
                    status = filter.status,
                    customerId = filter.customerId,
                    useStatuses = filter.statuses.isNotEmpty(),
                    statuses = filter.statuses.toList(),
                ).distinctUntilChanged()
                .map { list ->
                    list.map { projection ->
                        OrderListItem(
                            order = projection.order.toDomain(),
                            customer = projection.customer?.toDomain(),
                            attachments =
                                OrderAttachmentCounts(
                                    images = projection.imagesCount,
                                    videos = projection.videosCount,
                                    documents = projection.documentsCount,
                                    signatures = projection.signaturesCount,
                                    finalPhotos = projection.finalPhotosCount,
                                ),
                        )
                    }
                }

        override fun observeOrderDetail(orderId: String): Flow<OrderDetail?> = repairOrderDao.observeOrderDetailProjection(orderId).map { it?.toDomain() }.distinctUntilChanged()

        override suspend fun createOrder(
            actor: AuthenticatedUser,
            input: RepairOrderInput,
        ): Result<Order> =
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
                    order.toDomain()
                }
            }

        override suspend fun changeStatus(
            actor: AuthenticatedUser,
            orderId: String,
            status: OrderStatus,
            note: String,
            closureReason: ClosureReason?,
        ): Result<Unit> =
            runCatching {
                database.withTransaction {
                    val order = requireWritableOrder(actor, orderId)
                    if (order.status == status) return@withTransaction
                    OrderStatusPolicy.requireAllowed(order.status, status, closureReason)
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

        override suspend fun recordResult(
            actor: AuthenticatedUser,
            orderId: String,
            input: DiagnosisResultInput,
        ): Result<Unit> =
            runCatching {
                database.withTransaction {
                    val errors = input.validationErrors()
                    if (errors.isNotEmpty()) {
                        val messages =
                            errors.map { error ->
                                when (error) {
                                    DiagnosisValidationError.SUMMARY_REQUIRED -> context.getString(R.string.error_result_summary_required)
                                    DiagnosisValidationError.POSITIVE_ESTIMATE_REQUIRED -> context.getString(R.string.error_positive_estimate_required)
                                }
                            }
                        require(false) { messages.joinToString("\n") }
                    }
                    val order = requireWritableOrder(actor, orderId)
                    require((order.status == OrderStatus.DIAGNOSIS) || (order.status == OrderStatus.RESULT)) {
                        context.getString(R.string.error_diagnosis_only_during)
                    }
                    if (order.status != OrderStatus.RESULT) {
                        OrderStatusPolicy.requireAllowed(order.status, OrderStatus.RESULT)
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

        override suspend fun reassign(
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

        override suspend fun addInternalNote(
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

        override suspend fun recordFeedback(
            actor: AuthenticatedUser,
            orderId: String,
            input: FeedbackInput,
        ): Result<Unit> =
            runCatching {
                database.withTransaction {
                    val errors = input.validationErrors()
                    if (errors.isNotEmpty()) {
                        val messages =
                            errors.map { error ->
                                when (error) {
                                    FeedbackValidationError.REJECT_REASON_REQUIRED -> context.getString(R.string.error_reject_reason_required)
                                }
                            }
                        require(false) { messages.joinToString("\n") }
                    }
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
                        OrderStatusPolicy.requireAllowed(order.status, targetStatus, closureReason)
                        OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(actor, targetStatus)
                    }
                    val isClosing = targetStatus == OrderStatus.CLOSED
                    repairOrderDao.upsert(
                        order.copy(
                            status = targetStatus,
                            customerActionStatus = customerActionStatus,
                            closeNote =
                                if (isClosing) {
                                    input.note
                                        .capitalizeFirst()
                                        .ifBlank { null }
                                } else {
                                    null
                                },
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

        override suspend fun recordPdfShared(
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

        override suspend fun recordMediaRemoved(
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

        override suspend fun togglePaymentStatus(
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

        override suspend fun deleteOrder(
            actor: AuthenticatedUser,
            orderId: String,
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

        override suspend fun deleteSignature(
            actor: AuthenticatedUser,
            signatureId: String,
        ): Result<Unit> =
            runCatching {
                val sig =
                    requireNotNull(signatureDao.findById(signatureId)) { context.getString(R.string.error_signature_not_found) }
                if (sig.createdByUserId != actor.id) {
                    PermissionService.requirePermission(
                        actor,
                        UserPermission.EDIT_ALL_ORDERS,
                    )
                }
                signatureDao.deleteById(signatureId)
                fileCleanupService.deleteSpecificFile(sig.localPath)
                recordMediaRemoved(actor, sig.orderId, sig.fileName)
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
