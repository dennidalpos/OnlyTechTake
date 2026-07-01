package com.onlytechtake.repairapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderFilterMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ordersDataStore by preferencesDataStore(name = "orders_preferences")

class OrdersPreferences(private val context: Context) {
    fun getFilter(userId: String): Flow<OrderFilter> =
        context.ordersDataStore.data.map { preferences ->
            val modes = preferences[modesKey(userId)]?.mapNotNull {
                runCatching { OrderFilterMode.valueOf(it) }.getOrNull()
            }?.toSet() ?: OrderFilter.DEFAULT_MODES

            val status = preferences[statusKey(userId)]?.let {
                runCatching { OrderStatus.valueOf(it) }.getOrNull()
            }

            val statuses = preferences[statusesKey(userId)]?.mapNotNull {
                runCatching { OrderStatus.valueOf(it) }.getOrNull()
            }?.toSet() ?: emptySet()

            OrderFilter(
                modes = modes,
                query = preferences[queryKey(userId)] ?: "",
                status = status,
                statuses = statuses,
                customerId = preferences[customerIdKey(userId)],
            )
        }

    suspend fun saveFilter(userId: String, filter: OrderFilter) {
        context.ordersDataStore.edit { preferences ->
            preferences[modesKey(userId)] = filter.modes.map { it.name }.toSet()
            preferences[queryKey(userId)] = filter.query
            filter.status?.let { preferences[statusKey(userId)] = it.name } ?: preferences.remove(statusKey(userId))
            preferences[statusesKey(userId)] = filter.statuses.map { it.name }.toSet()
            filter.customerId?.let { preferences[customerIdKey(userId)] = it } ?: preferences.remove(customerIdKey(userId))
        }
    }

    private companion object {
        fun modesKey(userId: String) = stringSetPreferencesKey("modes_$userId")
        fun queryKey(userId: String) = stringPreferencesKey("query_$userId")
        fun statusKey(userId: String) = stringPreferencesKey("status_$userId")
        fun statusesKey(userId: String) = stringSetPreferencesKey("statuses_$userId")
        fun customerIdKey(userId: String) = stringPreferencesKey("customer_id_$userId")
    }
}
