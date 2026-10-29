package com.onlytechtake.repairapp.data.session

import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SessionState(
    val user: AuthenticatedUser? = null,
    val lastActivityAt: Long? = null,
)

class InMemorySessionStore {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _currentUser = MutableStateFlow<AuthenticatedUser?>(null)
    val currentUser: StateFlow<AuthenticatedUser?> = _currentUser.asStateFlow()

    fun setCurrentUser(
        user: AuthenticatedUser,
        now: Long,
    ) {
        _state.value = SessionState(user, now)
        _currentUser.value = user
    }

    fun touch(now: Long) {
        _state.update {
            if (it.user != null) it.copy(lastActivityAt = now) else it
        }
    }

    fun isExpired(
        now: Long,
        timeoutMillis: Long,
    ): Boolean {
        val current = _state.value
        current.user ?: return false
        val activityAt = current.lastActivityAt ?: return true
        return now - activityAt >= timeoutMillis
    }

    fun clear() {
        _state.value = SessionState()
        _currentUser.value = null
    }
}
