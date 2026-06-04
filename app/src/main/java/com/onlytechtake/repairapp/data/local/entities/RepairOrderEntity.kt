package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repair_orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = OrderCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["assignedToUserId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["createdByUserId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["deletedByUserId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["orderNumber"], unique = true),
        Index(value = ["customerId"]),
        Index(value = ["categoryId"]),
        Index(value = ["status"]),
        Index(value = ["assignedToUserId"]),
        Index(value = ["createdByUserId"]),
        Index(value = ["deletedByUserId"]),
        Index(value = ["deletedAt"]),
        Index(value = ["createdAt"]),
    ],
)
data class RepairOrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val customerId: String,
    val title: String,
    val categoryId: String,
    val status: OrderStatus,
    val priority: OrderPriority,
    val assignedToUserId: String?,
    val createdByUserId: String,
    val description: String?,
    val internalNotes: String?,
    val estimateAmountCents: Long?,
    val finalAmountCents: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long?,
    val closureReason: ClosureReason?,
    val deletedAt: Long?,
    val deletedByUserId: String?,
    val deleteReason: String?,
)
