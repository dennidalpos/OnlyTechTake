package com.onlytechtake.repairapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["active"]),
    ],
)
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: UserRole,
    @ColumnInfo(defaultValue = "1") val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
