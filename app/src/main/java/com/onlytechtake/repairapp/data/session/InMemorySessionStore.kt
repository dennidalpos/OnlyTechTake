package com.onlytechtake.repairapp.data.session

import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemorySessionStore {
    private val mutableCurrentUser = MutableStateFlow<AuthenticatedUser?>(null)
    private var lastActivityAt: Long? = null

    val currentUser: StateFlow<AuthenticatedUser?> = mutableCurrentUser

    fun setCurrentUser(user: AuthenticatedUser, now: Long) {
        mutableCurrentUser.value = user
        lastActivityAt = now
    }

    fun touch(now: Long) {
        if (mutableCurrentUser.value != null) {
            lastActivityAt = now
        }
    }

    fun isExpired(now: Long, timeoutMillis: Long): Boolean {
        mutableCurrentUser.value ?: return false
        val activityAt = lastActivityAt ?: return true
        return now - activityAt >= timeoutMillis
    }

    fun clear() {
        mutableCurrentUser.value = null
        lastActivityAt = null
    }
}
