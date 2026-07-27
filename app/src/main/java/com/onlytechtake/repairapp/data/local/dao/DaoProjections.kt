package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.domain.models.ClosureReason
import com.onlytechtake.repairapp.domain.models.CustomerType
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.OrderPriority
import com.onlytechtake.repairapp.domain.models.OrderStatus

data class OrderExportRow(
    val orderNumber: String,
    val title: String,
    val status: OrderStatus,
    val priority: OrderPriority,
    val customerName: String?,
    val customerType: CustomerType?,
    val phone: String?,
    val email: String?,
    val categoryName: String?,
    val assignedUserName: String?,
    val signerName: String?,
    val description: String?,
    val internalNotes: String?,
    val diagnosisOutcome: DiagnosisOutcome? = null,
    val isPaid: Boolean,
    val estimateAmountCents: Long?,
    val finalAmountCents: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long?,
    val closureReason: ClosureReason?,
)

data class OrderListItemProjection(
    @Embedded val order: RepairOrderEntity,
    @Embedded(prefix = "cust_") val customer: CustomerEntity?,
    val imagesCount: Int,
    val videosCount: Int,
    val documentsCount: Int,
    val signaturesCount: Int,
    val finalPhotosCount: Int,
)

data class OrderDetailProjection(
    @Embedded val order: RepairOrderEntity,
    @Relation(parentColumn = "customerId", entityColumn = "id")
    val customer: CustomerEntity?,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: OrderCategoryEntity?,
    @Relation(parentColumn = "assignedToUserId", entityColumn = "id")
    val assignedUser: UserEntity?,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val timeline: List<TimelineEventEntity>,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val media: List<OrderMediaEntity>,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val signatures: List<SignatureEntity>,
)
