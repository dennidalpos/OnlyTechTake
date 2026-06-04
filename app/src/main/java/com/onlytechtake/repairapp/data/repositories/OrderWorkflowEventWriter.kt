package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import java.util.UUID

internal class OrderWorkflowEventWriter(
    private val timelineEventDao: TimelineEventDao? = null,
    private val auditLogDao: AuditLogDao? = null,
) {
    suspend fun orderCreated(orderId: String, actorUserId: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.ORDER_CREATED,
                title = "Commessa creata",
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
        now: Long,
    ) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.STATUS_CHANGED,
                title = "Stato aggiornato",
                description = "${oldStatus.name} -> ${newStatus.name}",
                oldStatus = oldStatus.name,
                newStatus = newStatus.name,
                actorUserId = actorUserId,
                createdAt = now,
                visibleToCustomer = true,
                requiresCustomerFeedback = newStatus == OrderStatus.PREVENTIVO_INVIATO,
                expectedFeedbackType = if (newStatus == OrderStatus.PREVENTIVO_INVIATO) {
                    CustomerFeedbackType.ACCEPT
                } else {
                    null
                },
            ),
        )
        audit(actorUserId, AuditAction.ORDER_STATUS_CHANGED, orderId, null, now)
    }

    suspend fun orderReassigned(orderId: String, actorUserId: String, assignedToUserId: String?, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.ORDER_REASSIGNED,
                title = "Commessa riassegnata",
                description = assignedToUserId ?: "Non assegnata",
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
                title = "Nota interna aggiunta",
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
                title = "Commessa archiviata",
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
                title = "Feedback cliente registrato",
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

    suspend fun signatureCaptured(orderId: String, actorUserId: String, signerName: String, now: Long) {
        writeTimeline(
            timelineEvent(
                orderId = orderId,
                type = TimelineEventType.SIGNATURE_CAPTURED,
                title = "Firma acquisita",
                description = signerName,
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
        requireNotNull(timelineEventDao) { "Timeline writer non configurato" }.upsert(event)
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
        actorUserId: String,
        action: AuditAction,
        orderId: String?,
        customerId: String?,
        now: Long,
    ) {
        requireNotNull(auditLogDao) { "Audit writer non configurato" }.upsert(
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
