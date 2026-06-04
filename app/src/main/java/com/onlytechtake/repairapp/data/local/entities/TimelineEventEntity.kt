package com.onlytechtake.repairapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timeline_events",
    foreignKeys = [
        ForeignKey(
            entity = RepairOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
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
        Index(value = ["type"]),
        Index(value = ["createdByUserId"]),
        Index(value = ["createdAt"]),
    ],
)
data class TimelineEventEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val type: TimelineEventType,
    val title: String,
    val description: String?,
    val oldStatus: String?,
    val newStatus: String?,
    @ColumnInfo(defaultValue = "0") val visibleToCustomer: Boolean,
    @ColumnInfo(defaultValue = "0") val requiresCustomerFeedback: Boolean,
    val expectedFeedbackType: CustomerFeedbackType?,
    val customerFeedback: CustomerFeedbackType?,
    val customerFeedbackNote: String?,
    val customerFeedbackAt: Long?,
    val createdByUserId: String,
    val createdAt: Long,
)
