package com.onlytechtake.repairapp.features.dashboard.orders

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.OrdersPreferences
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class OrdersViewModel(
    authRepository: AuthRepository,
    orderRepository: OrderRepository,
    private val backupRepository: BackupRepository? = null,
    backupReminderPreferences: BackupReminderPreferences? = null,
    private val ordersPreferences: OrdersPreferences? = null,
) : ViewModel() {
    private val _orderFilter = MutableStateFlow(OrderFilter())
    val orderFilter = _orderFilter.asStateFlow()

    val currentUser = authRepository.currentUser

    init {
        viewModelScope.launch {
            currentUser
                .filterNotNull()
                .flatMapLatest { user ->
                    ordersPreferences?.getFilter(user.id) ?: flowOf(OrderFilter())
                }.take(1)
                .collect { savedFilter ->
                    _orderFilter.value = savedFilter
                }
        }
    }

    val backupReminderSettings =
        backupReminderPreferences
            ?.settings
            ?.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                BackupReminderSettings(),
            )
            ?: flowOf(
                BackupReminderSettings(enabled = false),
            ).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                BackupReminderSettings(enabled = false),
            )

    val isBackupReminderDue =
        combine(
            currentUser,
            backupReminderSettings,
        ) { user, settings ->
            PermissionService.isAdmin(user) &&
                BackupReminderPolicy.shouldShowReminder(settings, System.currentTimeMillis())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    val orderListItems: StateFlow<List<OrderListItem>?> =
        _orderFilter
            .debounce(300.milliseconds)
            .combine(currentUser) { filter, user -> user to filter }
            .flatMapLatest { (user, filter) ->
                when {
                    user == null -> flowOf(emptyList())
                    filter.query.isNotBlank() -> orderRepository.searchOrders(user, filter)
                    else -> orderRepository.observeOrderList(user, filter)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFilter(filter: OrderFilter) {
        _orderFilter.value = filter
        val user = currentUser.value
        if (user != null && ordersPreferences != null) {
            viewModelScope.launch {
                ordersPreferences.saveFilter(user.id, filter)
            }
        }
    }

    fun exportCompleteBackup(uri: Uri) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            backupRepository?.exportCompleteBackup(user, uri)
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val orderRepository: OrderRepository,
        private val backupRepository: BackupRepository? = null,
        private val backupReminderPreferences: BackupReminderPreferences? = null,
        private val ordersPreferences: OrdersPreferences? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OrdersViewModel(
                authRepository,
                orderRepository,
                backupRepository,
                backupReminderPreferences,
                ordersPreferences,
            ) as T
    }
}
