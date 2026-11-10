package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlytechtake.repairapp.domain.models.OrderDocumentType

@Entity(
    tableName = "order_documents",
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
        Index(value = ["createdByUserId"]),
        Index(value = ["createdAt"]),
    ],
)
data class OrderDocumentEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val type: OrderDocumentType,
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val includesCustomerSignature: Boolean,
    val createdByUserId: String,
    val createdAt: Long,
    val sharedAt: Long?,
)
