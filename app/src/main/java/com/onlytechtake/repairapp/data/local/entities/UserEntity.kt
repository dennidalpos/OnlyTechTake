package com.onlytechtake.repairapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.onlytechtake.repairapp.domain.models.UserPermission
import com.onlytechtake.repairapp.domain.models.UserRole

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
    val permissions: Set<UserPermission> = emptySet(),
    @ColumnInfo(defaultValue = "1") val active: Boolean,
    @ColumnInfo(defaultValue = "0") val forcePasswordChange: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
