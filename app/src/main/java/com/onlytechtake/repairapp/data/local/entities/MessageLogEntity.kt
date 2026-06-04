package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_logs",
    foreignKeys = [
        ForeignKey(
            entity = RepairOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TimelineEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["timelineEventId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["createdByUserId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["timelineEventId"]),
        Index(value = ["customerId"]),
        Index(value = ["channel"]),
        Index(value = ["status"]),
        Index(value = ["createdByUserId"]),
        Index(value = ["createdAt"]),
    ],
)
data class MessageLogEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val timelineEventId: String?,
    val customerId: String,
    val channel: MessageChannel,
    val mode: MessageMode,
    val body: String,
    val status: MessageStatus,
    val expectedReply: CustomerFeedbackType?,
    val actualReply: CustomerFeedbackType?,
    val actualReplyNote: String?,
    val createdByUserId: String,
    val createdAt: Long,
    val sentAt: Long?,
)
