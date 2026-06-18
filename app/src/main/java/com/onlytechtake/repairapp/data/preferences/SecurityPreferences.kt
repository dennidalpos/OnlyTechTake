package com.onlytechtake.repairapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by preferencesDataStore(name = "security_preferences")

data class SecuritySettings(
    val isBackupEncryptionEnabled: Boolean = false,
    val isDatabaseEncryptionEnabled: Boolean = false,
    // La password non dovrebbe essere salvata in chiaro nelle preferenze se possibile,
    // ma per questa implementazione "fresh" e locale seguiamo la richiesta dell'utente.
    val backupEncryptionPassword: String = "",
)

class SecurityPreferences(
    private val context: Context,
) {
    val settings: Flow<SecuritySettings> =
        context.securityDataStore.data.map { preferences ->
            SecuritySettings(
                isBackupEncryptionEnabled = preferences[BACKUP_ENCRYPTION_ENABLED] ?: false,
                isDatabaseEncryptionEnabled = preferences[DATABASE_ENCRYPTION_ENABLED] ?: false,
                backupEncryptionPassword = preferences[BACKUP_ENCRYPTION_PASSWORD] ?: "",
            )
        }

    suspend fun setBackupEncryptionEnabled(enabled: Boolean) {
        context.securityDataStore.edit { preferences ->
            preferences[BACKUP_ENCRYPTION_ENABLED] = enabled
        }
    }

    suspend fun setDatabaseEncryptionEnabled(enabled: Boolean) {
        context.securityDataStore.edit { preferences ->
            preferences[DATABASE_ENCRYPTION_ENABLED] = enabled
        }
    }

    suspend fun setBackupEncryptionPassword(password: String) {
        context.securityDataStore.edit { preferences ->
            preferences[BACKUP_ENCRYPTION_PASSWORD] = password
        }
    }

    suspend fun getSettings(): SecuritySettings = settings.first()

    private companion object {
        val BACKUP_ENCRYPTION_ENABLED = booleanPreferencesKey("backup_encryption_enabled")
        val DATABASE_ENCRYPTION_ENABLED = booleanPreferencesKey("database_encryption_enabled")
        val BACKUP_ENCRYPTION_PASSWORD = stringPreferencesKey("backup_encryption_password")
    }
}
