package com.onlytechtake.repairapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_templates",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["updatedByUserId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["channel"]),
        Index(value = ["active"]),
        Index(value = ["updatedByUserId"]),
    ],
)
data class MessageTemplateEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val channel: MessageChannel,
    val body: String,
    @ColumnInfo(defaultValue = "1") val active: Boolean,
    @ColumnInfo(defaultValue = "1") val version: Int,
    val updatedByUserId: String?,
    val updatedAt: Long,
)
