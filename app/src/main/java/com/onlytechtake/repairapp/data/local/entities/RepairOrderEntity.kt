package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlytechtake.repairapp.domain.models.ClosureReason
import com.onlytechtake.repairapp.domain.models.CustomerActionStatus
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.OrderPriority
import com.onlytechtake.repairapp.domain.models.OrderStatus

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
            childColumns = ["closedByUserId"],
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
        Index(value = ["closedByUserId"]),
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
    val internalNotes: String? = null,
    val diagnosisOutcome: DiagnosisOutcome? = null,
    val estimateAmountCents: Long? = null,
    val finalAmountCents: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long? = null,
    val closureReason: ClosureReason? = null,
    val customerActionStatus: CustomerActionStatus? = null,
    val closeNote: String? = null,
    val closedByUserId: String? = null,
    val categoryPathSnapshot: String? = null,
    val lastCustomerContactAt: Long? = null,
)
