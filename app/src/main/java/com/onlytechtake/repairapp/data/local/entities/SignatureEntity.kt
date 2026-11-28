package com.onlytechtake.repairapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "signatures",
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
        Index(value = ["signedAt"]),
    ],
)
data class SignatureEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val signerName: String,
    @ColumnInfo(defaultValue = "0") val privacyAccepted: Boolean,
    @ColumnInfo(defaultValue = "0") val termsAccepted: Boolean,
    val privacyTextSnapshot: String,
    @ColumnInfo(defaultValue = "0") val privacyTemplateVersion: Int,
    val termsTextSnapshot: String,
    @ColumnInfo(defaultValue = "0") val termsTemplateVersion: Int,
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val createdByUserId: String?,
    val signedAt: Long,
    val createdAt: Long,
)
