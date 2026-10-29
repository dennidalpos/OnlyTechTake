package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_settings",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["updatedByUserId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["updatedByUserId"]),
        Index(value = ["updatedAt"]),
    ],
)
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedByUserId: String?,
    val updatedAt: Long,
)
