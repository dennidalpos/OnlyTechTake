package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["actorUserId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = RepairOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["action"]),
        Index(value = ["actorUserId"]),
        Index(value = ["orderId"]),
        Index(value = ["customerId"]),
        Index(value = ["createdAt"]),
    ],
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val action: AuditAction,
    val actorUserId: String?,
    val orderId: String?,
    val customerId: String?,
    val detailsJson: String?,
    val createdAt: Long,
)
