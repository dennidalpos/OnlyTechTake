package com.onlytechtake.repairapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_preferences")

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

    suspend fun clear() {
        context.authDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private companion object {
        val APP_CONFIGURED = booleanPreferencesKey("app_configured")
        val LAST_USER_ID = stringPreferencesKey("last_user_id")
        val LAST_LOGIN_AT = longPreferencesKey("last_login_at")
    }
}
