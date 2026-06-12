package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerActionStatus
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import java.util.UUID

internal class OrderWorkflowEventWriter(
    private val context: Context,
    private val timelineEventDao: TimelineEventDao? = null,
    private val auditLogDao: AuditLogDao? = null,
) {
    suspend fun orderCreated(orderId: String, actorUserId: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.ORDER_CREATED,
                title = context.getString(R.string.order_created_title),
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = true,
            ),
        )
    }

    suspend fun statusChanged(
        orderId: String,
        actorUserId: String,
        oldStatus: OrderStatus,
        newStatus: OrderStatus,
        note: String?,
        closureReason: ClosureReason?,
        now: Long,
    ) {
        val readableTransition = "${oldStatus.label(context)} -> ${newStatus.label(context)}"
        val details = listOfNotNull(
            readableTransition,
            note,
            closureReason?.let { context.getString(R.string.closure_reason_prefix, it.label(context)) },
        ).joinToString("\n")
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.STATUS_CHANGED,
                title = context.getString(R.string.status_updated_title),
                description = details,
                oldStatus = oldStatus.name,
                newStatus = newStatus.name,
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = true,
                requiresCustomerFeedback = false,
                expectedFeedbackType = null,
            ),
        )
        audit(actorUserId, AuditAction.ORDER_STATUS_CHANGED, orderId, null, now)
    }

    suspend fun orderReassigned(orderId: String, actorUserId: String, assignedToUserId: String?, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.ORDER_REASSIGNED,
                title = context.getString(R.string.order_reassigned_title),
                description = assignedToUserId ?: context.getString(R.string.unassigned),
                actorUserId = actorUserId,
                createdAt = now,
            ),
        )
        audit(actorUserId, AuditAction.ORDER_REASSIGNED, orderId, null, now)
    }

    suspend fun internalNoteAdded(orderId: String, actorUserId: String, note: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.INTERNAL_NOTE_ADDED,
                title = context.getString(R.string.internal_note_added_title),
                description = note,
                actorUserId = actorUserId,
                createdAt = now,
            ),
        )
    }

    suspend fun orderDeleted(orderId: String, actorUserId: String, reason: String?, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.ORDER_DELETED,
                title = context.getString(R.string.order_archived_title),
                description = reason,
                actorUserId = actorUserId,
                createdAt = now,
            ),
        )
        audit(actorUserId, AuditAction.ORDER_DELETED, orderId, null, now)
    }

    suspend fun feedbackRecorded(
        orderId: String,
        actorUserId: String,
        feedback: CustomerFeedbackType,
        note: String,
        targetStatus: OrderStatus,
        now: Long,
    ) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.CUSTOMER_FEEDBACK_RECORDED,
                title = context.getString(R.string.customer_feedback_recorded_title),
                description = note,
                newStatus = targetStatus.name,
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = true,
                customerFeedback = feedback,
                customerFeedbackAt = now,
            ),
        )
    }

    suspend fun mediaAdded(
        orderId: String,
        actorUserId: String,
        title: String,
        description: String,
        now: Long,
    ) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.MEDIA_ADDED,
                title = title,
                description = description,
                actorUserId = actorUserId,
                createdAt = now,
            ),
        )
    }

    suspend fun mediaRemoved(orderId: String, actorUserId: String, fileName: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.MEDIA_REMOVED,
                title = context.getString(R.string.media_removed_title),
                description = fileName,
                actorUserId = actorUserId,
                createdAt = now,
            ),
        )
    }

    suspend fun signatureCaptured(orderId: String, actorUserId: String, signerName: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.SIGNATURE_CAPTURED,
                title = context.getString(R.string.signature_captured_title),
                description = signerName,
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = true,
            ),
        )
    }

    suspend fun pdfShared(orderId: String, actorUserId: String, fileName: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.PDF_STATUS_SHARED,
                title = context.getString(R.string.pdf_shared_title),
                description = fileName,
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = true,
            ),
        )
    }

    suspend fun whatsappPrepared(orderId: String, actorUserId: String, body: String, now: Long): TimelineEventEntity {
        val event = timelineEvent(
            orderId = orderId,
            type = TimelineEventType.WHATSAPP_PREPARED,
            title = context.getString(R.string.order_wa_prepared_timeline_title),
            description = body,
            actorUserId = actorUserId,
            createdAt = now,
        )
        writeTimeline(event)
        return event
    }

    suspend fun whatsappSent(orderId: String, actorUserId: String, body: String, now: Long): TimelineEventEntity {
        val event = timelineEvent(
            orderId = orderId,
            type = TimelineEventType.WHATSAPP_SENT_CONFIRMED,
            title = context.getString(R.string.order_wa_sent_timeline_title),
            description = body,
            actorUserId = actorUserId,
            createdAt = now,
        )
        writeTimeline(event)
        return event
    }

    suspend fun customerActionStatusChanged(
        orderId: String,
        actorUserId: String,
        status: CustomerActionStatus,
        now: Long,
    ) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.CUSTOMER_ACTION_STATUS_CHANGED,
                title = context.getString(R.string.customer_action_status_updated_title),
                description = status.label(context),
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = false,
            ),
        )
    }

    suspend fun paymentReceived(orderId: String, actorUserId: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.PAYMENT_RECEIVED,
                title = context.getString(R.string.payment_received_title),
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = true,
            ),
        )
    }

    suspend fun paymentRemoved(orderId: String, actorUserId: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.PAYMENT_REMOVED,
                title = context.getString(R.string.payment_removed_title),
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = true,
            ),
        )
    }

    suspend fun dataExported(actorUserId: String, now: Long) {
        audit(actorUserId, AuditAction.DATA_EXPORTED, null, null, now)
    }

    suspend fun dataReset(actorUserId: String, now: Long) {
        audit(actorUserId, AuditAction.DATA_RESET, null, null, now)
    }

    private suspend fun writeTimeline(event: TimelineEventEntity) {
        requireNotNull(timelineEventDao) { context.getString(R.string.timeline_writer_not_configured) }.upsert(event)
    }

    private fun timelineEvent(
        orderId: String,
        type: TimelineEventType,
        title: String,
        actorUserId: String,
        createdAt: Long,
        description: String? = null,
        oldStatus: String? = null,
        newStatus: String? = null,
        visibleToCustomer: Boolean = false,
        requiresCustomerFeedback: Boolean = false,
        expectedFeedbackType: CustomerFeedbackType? = null,
        customerFeedback: CustomerFeedbackType? = null,
        customerFeedbackAt: Long? = null,
    ) = TimelineEventEntity(
        id = UUID.randomUUID().toString(),
        orderId = orderId,
        type = type,
        title = title,
        description = description,
        oldStatus = oldStatus,
        newStatus = newStatus,
        visibleToCustomer = visibleToCustomer,
        requiresCustomerFeedback = requiresCustomerFeedback,
        expectedFeedbackType = expectedFeedbackType,
        customerFeedback = customerFeedback,
        customerFeedbackNote = description,
        customerFeedbackAt = customerFeedbackAt,
        createdByUserId = actorUserId,
        createdAt = createdAt,
    )

    private suspend fun audit(
        actorUserId: String?,
        action: AuditAction,
        orderId: String?,
        customerId: String?,
        now: Long,
    ) {
        requireNotNull(auditLogDao) { context.getString(R.string.audit_writer_not_configured) }.upsert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = action,
                actorUserId = actorUserId,
                orderId = orderId,
                customerId = customerId,
                detailsJson = null,
                createdAt = now,
            ),
        )
    }
}
