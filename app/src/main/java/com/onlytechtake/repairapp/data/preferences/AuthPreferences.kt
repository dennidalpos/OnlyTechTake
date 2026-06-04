package com.onlytechtake.repairapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_preferences")

data class FailedLoginPreference(
    val attempts: Int,
    val lockedUntilMillis: Long?,
)

class AuthPreferences(
    private val context: Context,
) {
    val appConfigured: Flow<Boolean> =
        context.authDataStore.data.map { preferences ->
            preferences[APP_CONFIGURED] ?: false
        }

    val lastUserId: Flow<String?> =
        context.authDataStore.data.map { preferences ->
            preferences[LAST_USER_ID]
        }

    suspend fun markConfigured() {
        context.authDataStore.edit { preferences ->
            preferences[APP_CONFIGURED] = true
        }
    }

    suspend fun recordLogin(userId: String, loginAt: Long) {
        context.authDataStore.edit { preferences ->
            preferences[LAST_USER_ID] = userId
            preferences[LAST_LOGIN_AT] = loginAt
        }
    }

    suspend fun failedLoginState(username: String): FailedLoginPreference? {
        val key = username.normalizedLoginKey()
        val states = parseFailedLoginStates(context.authDataStore.data.first()[FAILED_LOGIN_STATES])
        return states[key]
    }

    suspend fun recordFailedLoginState(username: String, state: FailedLoginPreference) {
        val key = username.normalizedLoginKey()
        context.authDataStore.edit { preferences ->
            val states = parseFailedLoginStates(preferences[FAILED_LOGIN_STATES]).toMutableMap()
            states[key] = state
            preferences[FAILED_LOGIN_STATES] = serializeFailedLoginStates(states)
        }
    }

    suspend fun clearFailedLoginState(username: String) {
        val key = username.normalizedLoginKey()
        context.authDataStore.edit { preferences ->
            val states = parseFailedLoginStates(preferences[FAILED_LOGIN_STATES]).toMutableMap()
            states.remove(key)
            if (states.isEmpty()) {
                preferences.remove(FAILED_LOGIN_STATES)
            } else {
                preferences[FAILED_LOGIN_STATES] = serializeFailedLoginStates(states)
            }
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun parseFailedLoginStates(raw: String?): Map<String, FailedLoginPreference> {
        if (raw.isNullOrBlank()) return emptyMap()

        return raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split(STATE_SEPARATOR)
                if (parts.size != 3) return@mapNotNull null
                val username = runCatching { URLDecoder.decode(parts[0], UTF_8) }.getOrNull()
                    ?: return@mapNotNull null
                val attempts = parts[1].toIntOrNull() ?: return@mapNotNull null
                val lockedUntil = parts[2].toLongOrNull()?.takeIf { it > 0L }
                username to FailedLoginPreference(attempts, lockedUntil)
            }
            .toMap()
    }

    private fun serializeFailedLoginStates(states: Map<String, FailedLoginPreference>): String =
        states.entries
            .sortedBy { it.key }
            .joinToString("\n") { (username, state) ->
                listOf(
                    URLEncoder.encode(username, UTF_8),
                    state.attempts.toString(),
                    (state.lockedUntilMillis ?: 0L).toString(),
                ).joinToString(STATE_SEPARATOR)
            }

    private fun String.normalizedLoginKey(): String = trim().lowercase()

    private companion object {
        const val UTF_8 = "UTF-8"
        const val STATE_SEPARATOR = "|"
        val APP_CONFIGURED = booleanPreferencesKey("app_configured")
        val LAST_USER_ID = stringPreferencesKey("last_user_id")
        val LAST_LOGIN_AT = longPreferencesKey("last_login_at")
        val FAILED_LOGIN_STATES = stringPreferencesKey("failed_login_states")
    }
}
