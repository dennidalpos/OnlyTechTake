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

    suspend fun login(username: String, password: CharArray): LoginResult

    fun touchSession()

    suspend fun logout()
}
