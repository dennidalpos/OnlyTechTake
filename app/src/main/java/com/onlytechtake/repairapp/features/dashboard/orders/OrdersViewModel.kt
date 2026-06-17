package com.onlytechtake.repairapp.features.dashboard.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.features.workspace.buildOperationalQueues
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModel(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderWorkflowRepository,
    private val backupRepository: BackupRepository? = null,
    private val backupReminderPreferences: BackupReminderPreferences? = null,
) : ViewModel() {

    private val _orderFilter = MutableStateFlow(OrderFilter())
    val orderFilter = _orderFilter.asStateFlow()

    val currentUser = authRepository.currentUser
    
    val backupReminderSettings = backupReminderPreferences?.settings
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupReminderSettings())
        ?: flowOf(BackupReminderSettings(enabled = false)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupReminderSettings(enabled = false))

    val isBackupReminderDue = combine(
        currentUser,
        backupReminderSettings
    ) { user, settings ->
        PermissionService.isAdmin(user) && 
        BackupReminderPolicy.shouldShowReminder(settings, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val orderListItems = combine(currentUser, _orderFilter) { user, filter -> user to filter }
        .flatMapLatest { (user, filter) ->
            when {
                user == null -> flowOf(emptyList())
                filter.query.isNotBlank() -> orderRepository.searchOrders(user, filter)
                else -> orderRepository.observeOrderList(user, filter)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val operationalQueues = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else orderRepository.observeOrders(user, OrderFilter())
        }
        .map(::buildOperationalQueues)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildOperationalQueues(emptyList()))

    fun setFilter(filter: OrderFilter) {
        _orderFilter.value = filter
    }

    fun changeStatus(orderId: String, status: OrderStatus, note: String = "", closureReason: ClosureReason? = null) {
        launchWithUser { actor ->
            orderRepository.changeStatus(actor, orderId, status, note, closureReason)
        }
    }

    fun exportCompleteBackup(uri: android.net.Uri) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            backupRepository?.exportCompleteBackup(user, uri)
        }
    }

    private fun launchWithUser(
        block: suspend (com.onlytechtake.repairapp.domain.auth.AuthenticatedUser) -> Result<*>
    ) {
        val user = currentUser.value
        if (user != null) {
            viewModelScope.launch {
                block(user)
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val orderRepository: OrderWorkflowRepository,
        private val backupRepository: BackupRepository? = null,
        private val backupReminderPreferences: BackupReminderPreferences? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrdersViewModel(authRepository, orderRepository, backupRepository, backupReminderPreferences) as T
        }
    }
}
