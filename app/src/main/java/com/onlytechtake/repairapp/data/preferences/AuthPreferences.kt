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
    val lastUsername: Flow<String?> =
        context.authDataStore.data.map { preferences ->
            preferences[LAST_USERNAME]
        }

    suspend fun markConfigured() {
        context.authDataStore.edit { preferences ->
            preferences[APP_CONFIGURED] = true
        }
    }

    suspend fun recordLogin(
        username: String,
        loginAt: Long,
    ) {
        context.authDataStore.edit { preferences ->
            preferences[LAST_USERNAME] = username
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
        val LAST_USERNAME = stringPreferencesKey("last_username")
        val LAST_LOGIN_AT = longPreferencesKey("last_login_at")
    }
}
