package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Embedded
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity

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
