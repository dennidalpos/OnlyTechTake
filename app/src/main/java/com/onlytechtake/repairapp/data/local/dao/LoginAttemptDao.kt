package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.LoginAttemptEntity

@Dao
interface LoginAttemptDao {
    @Upsert
    suspend fun upsert(attempt: LoginAttemptEntity)

    @Query("SELECT COUNT(*) FROM login_attempts WHERE username = :username AND successful = 0 AND attemptAt > :since")
    suspend fun countFailedAttempts(
        username: String,
        since: Long,
    ): Int

    @Query("DELETE FROM login_attempts WHERE username = :username")
    suspend fun clearAttempts(username: String)

    @Query("DELETE FROM login_attempts WHERE attemptAt < :before")
    suspend fun cleanupOldAttempts(before: Long)
}
