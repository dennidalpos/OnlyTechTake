package com.onlytechtake.repairapp.features.dashboard.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.error.ErrorHandler
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModel(
    private val orderRepository: OrderWorkflowRepository,
    private val authRepository: AuthRepository,
    private val errorHandler: ErrorHandler,
) : ViewModel() {

    private val _filter = MutableStateFlow(OrderFilter())
    val filter = _filter.asStateFlow()

    val orderListItems: StateFlow<List<OrderListItem>> = combine(authRepository.currentUser, _filter) { user, filter ->
        user to filter
    }
    .flatMapLatest { (user, filter) ->
        if (user == null) flowOf(emptyList())
        else orderRepository.observeOrderList(user, filter)
    }
    .catch { errorHandler.handleError(it, "OrdersViewModel") }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: OrderFilter) {
        _filter.value = filter
    }

    class Factory(
        private val orderRepository: OrderWorkflowRepository,
        private val authRepository: AuthRepository,
        private val errorHandler: ErrorHandler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrdersViewModel(orderRepository, authRepository, errorHandler) as T
        }
    }
}
