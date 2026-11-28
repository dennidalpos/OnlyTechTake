package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_media",
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
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["createdByUserId"]),
        Index(value = ["capturedAt"]),
    ],
)
data class OrderMediaEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val mediaType: String,
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val createdByUserId: String?,
    val capturedAt: Long,
    val createdAt: Long,
)
