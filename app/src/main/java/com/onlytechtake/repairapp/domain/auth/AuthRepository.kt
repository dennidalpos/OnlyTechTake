package com.onlytechtake.repairapp.domain.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<AuthenticatedUser?>

    fun observeAuthState(): Flow<AuthState>

    suspend fun createFirstAdmin(
        displayName: String,
        username: String,
        password: CharArray,
    ): AuthenticatedUser

    suspend fun login(
        username: String,
        password: CharArray,
    ): LoginResult

    suspend fun loginDemo(): LoginResult

    suspend fun changePassword(
        username: String,
        currentPassword: CharArray,
        newPassword: CharArray,
    ): Result<Unit>

    suspend fun getRecoveryKit(): Result<String>

    suspend fun resetAdminWithRecoveryCode(
        code: String,
        newPassword: CharArray,
    ): Result<Unit>

    fun touchSession()

    suspend fun logout()
}
