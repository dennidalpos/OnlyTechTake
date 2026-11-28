package com.onlytechtake.repairapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uiDataStore by preferencesDataStore(name = "ui_preferences")

data class UiSettings(
    val darkTheme: Boolean = true,
)

class UiPreferences(
    private val context: Context,
) {
    val settings: Flow<UiSettings> =
        context.uiDataStore.data.map { preferences ->
            UiSettings(
                darkTheme = preferences[DARK_THEME] ?: true,
            )
        }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.uiDataStore.edit { preferences ->
            preferences[DARK_THEME] = enabled
        }
    }

    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
