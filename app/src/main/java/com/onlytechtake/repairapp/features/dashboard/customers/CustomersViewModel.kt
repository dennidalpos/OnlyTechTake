package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CustomersViewModel(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val orderWorkflowRepository: OrderWorkflowRepository,
    private val onSetFormDirty: (Boolean) -> Unit,
) : ViewModel() {
    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showAll = MutableStateFlow(false)
    val showAll = _showAll.asStateFlow()

    private val _isFormDirty = MutableStateFlow(false)
    val isFormDirty = _isFormDirty.asStateFlow()

    val currentUser = authRepository.currentUser

    val customerSuggestions =
        _searchQuery
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { query ->
                when {
                    query.isNotBlank() -> customerRepository.search(query)
                    _showAll.value -> customerRepository.observeAll()
                    else -> customerRepository.observeRecent()
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setShowAll(show: Boolean) {
        _showAll.value = show
        // Trigger a refresh of customerSuggestions if query is blank
        if (_searchQuery.value.isBlank()) {
            _searchQuery.value = _searchQuery.value
        }
    }

    fun setFormDirty(dirty: Boolean) {
        _isFormDirty.value = dirty
        onSetFormDirty(dirty)
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCustomer(
        input: CustomerInput,
        onSaved: (CustomerEntity) -> Unit = {},
    ) {
        viewModelScope.launch {
            customerRepository.save(currentUser.value, input).fold(
                onSuccess = {
                    onSaved(it)
                    _searchQuery.value = ""
                    setFormDirty(false)
                },
                onFailure = { error ->
                    _message.value = WorkspaceMessage(error.message ?: "Errore", true)
                },
            )
        }
    }

    fun observeOrdersForCustomer(
        customerId: String,
        openOnly: Boolean,
    ) = currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            orderWorkflowRepository.observeOrderList(
                user,
                OrderFilter(
                    customerId = customerId,
                    statuses =
                        if (openOnly) {
                            OrderStatus
                                .values()
                                .filter { it != OrderStatus.CLOSED }
                                .toSet()
                        } else {
                            emptySet()
                        },
                ),
            )
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val customerRepository: CustomerRepository,
        private val orderWorkflowRepository: OrderWorkflowRepository,
        private val onSetFormDirty: (Boolean) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CustomersViewModel(
                authRepository,
                customerRepository,
                orderWorkflowRepository,
                onSetFormDirty,
            ) as T
    }
}
