package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "login_attempts",
    indices = [
        Index(value = ["username"]),
        Index(value = ["attemptAt"]),
    ],
)
data class LoginAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val successful: Boolean,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val attemptAt: Long,
)
