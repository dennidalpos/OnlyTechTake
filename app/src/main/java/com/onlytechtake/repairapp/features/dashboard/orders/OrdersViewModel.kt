package com.onlytechtake.repairapp.features.dashboard.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.OrdersPreferences
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import com.onlytechtake.repairapp.features.workspace.WorkspaceManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class OrdersViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        orderRepository: OrderRepository,
        backupReminderPreferences: BackupReminderPreferences,
        private val ordersPreferences: OrdersPreferences,
        private val workspaceManager: WorkspaceManager,
    ) : ViewModel() {
        private val _orderFilter = MutableStateFlow(OrderFilter())
        val orderFilter = _orderFilter.asStateFlow()

        val currentUser = authRepository.currentUser

        val backupReminderSettings =
            backupReminderPreferences
                .settings
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    BackupReminderSettings(),
                )

        val orderListItems: StateFlow<List<OrderListItem>?> =
            _orderFilter
                .debounce { filter ->
                    if (filter.query.isNotBlank()) 300.milliseconds else 0.milliseconds
                }.combine(currentUser) { filter, user -> user to filter }
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
            if (user != null) {
                viewModelScope.launch {
                    ordersPreferences.saveFilter(user.id, filter)
                }
            }
        }

        fun setFormDirty(dirty: Boolean) {
            workspaceManager.setFormDirty(dirty)
        }
    }
