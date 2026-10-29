package com.onlytechtake.repairapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.onlytechtake.repairapp.domain.models.OrderFilter
import com.onlytechtake.repairapp.domain.models.OrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ordersDataStore by preferencesDataStore(name = "orders_preferences")

class OrdersPreferences(
    private val context: Context,
) {
    fun getFilter(userId: String): Flow<OrderFilter> =
        context.ordersDataStore.data.map { preferences ->
            val status =
                preferences[statusKey(userId)]?.let {
                    runCatching { OrderStatus.valueOf(it) }.getOrNull()
                }

            val statuses =
                preferences[statusesKey(userId)]
                    ?.asSequence()
                    ?.mapNotNull {
                        runCatching { OrderStatus.valueOf(it) }.getOrNull()
                    }?.toSet() ?: emptySet()

            OrderFilter(
                query = preferences[queryKey(userId)] ?: "",
                status = status,
                statuses = statuses,
                customerId = preferences[customerIdKey(userId)],
            )
        }

    suspend fun saveFilter(
        userId: String,
        filter: OrderFilter,
    ) {
        context.ordersDataStore.edit { preferences ->
            preferences[queryKey(userId)] = filter.query
            filter.status?.let { preferences[statusKey(userId)] = it.name } ?: preferences.remove(
                statusKey(userId),
            )
            preferences[statusesKey(userId)] = filter.statuses.map { it.name }.toSet()
            filter.customerId?.let { preferences[customerIdKey(userId)] = it }
                ?: preferences.remove(customerIdKey(userId))
        }
    }

    private companion object {
        fun queryKey(userId: String) = stringPreferencesKey("query_$userId")

        fun statusKey(userId: String) = stringPreferencesKey("status_$userId")

        fun statusesKey(userId: String) = stringSetPreferencesKey("statuses_$userId")

        fun customerIdKey(userId: String) = stringPreferencesKey("customer_id_$userId")
    }
}
