package com.onlytechtake.repairapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ownerDataStore by preferencesDataStore(name = "owner_preferences")

class OwnerPreferences(
    private val context: Context,
) {
    val ownerInfo: Flow<OwnerInfo> =
        context.ownerDataStore.data.map { preferences ->
            OwnerInfo(
                name = preferences[NAME] ?: "",
                vatNumber = preferences[VAT_NUMBER] ?: "",
                address = preferences[ADDRESS] ?: "",
                email = preferences[EMAIL] ?: "",
                phone = preferences[PHONE] ?: "",
            )
        }

    suspend fun updateOwnerInfo(info: OwnerInfo) {
        context.ownerDataStore.edit { preferences ->
            preferences[NAME] = info.name
            preferences[VAT_NUMBER] = info.vatNumber
            preferences[ADDRESS] = info.address
            preferences[EMAIL] = info.email
            preferences[PHONE] = info.phone
        }
    }

    private companion object {
        val NAME = stringPreferencesKey("owner_name")
        val VAT_NUMBER = stringPreferencesKey("owner_vat")
        val ADDRESS = stringPreferencesKey("owner_address")
        val EMAIL = stringPreferencesKey("owner_email")
        val PHONE = stringPreferencesKey("owner_phone")
    }
}
