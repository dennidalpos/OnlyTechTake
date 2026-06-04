package com.onlytechtake.repairapp.domain.auth

import com.onlytechtake.repairapp.data.local.entities.UserRole

data class AuthenticatedUser(
    val id: String,
    val displayName: String,
    val username: String,
    val role: UserRole,
)

data class AuthState(
    val configured: Boolean = false,
    val currentUser: AuthenticatedUser? = null,
    val lastUsername: String = "",
)

sealed interface LoginResult {
    data class Success(val user: AuthenticatedUser) : LoginResult
    data object InvalidCredentials : LoginResult
    data object InactiveUser : LoginResult
    data object LockedOut : LoginResult
}
