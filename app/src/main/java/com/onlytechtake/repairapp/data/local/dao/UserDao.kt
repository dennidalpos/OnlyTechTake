package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("SELECT * FROM users ORDER BY role ASC, active DESC, displayName ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE active = 1 ORDER BY displayName ASC")
    fun observeActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'ADMIN'")
    suspend fun findAllAdmins(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    fun observeCountByRole(role: UserRole): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE role = :role AND active = 1")
    suspend fun countActiveByRole(role: UserRole): Int

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
