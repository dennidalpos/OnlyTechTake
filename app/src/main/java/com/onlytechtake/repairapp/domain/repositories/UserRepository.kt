package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.domain.models.User
import com.onlytechtake.repairapp.domain.models.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAll(): Flow<List<User>>

    fun observeActiveUsers(): Flow<List<User>>

    suspend fun findById(id: String): User?

    suspend fun findByUsername(username: String): User?

    fun observeById(id: String): Flow<User?>

    suspend fun countActiveByRole(role: UserRole): Int
}
