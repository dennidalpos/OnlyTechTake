package com.onlytechtake.repairapp.features.dashboard.customers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.models.Customer
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.repositories.CustomerRepository
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.features.workspace.WorkspaceManager
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class CustomersViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        private val customerRepository: CustomerRepository,
        private val orderWorkflowRepository: OrderRepository,
        private val workspaceManager: WorkspaceManager,
        private val actionRunner: AuthenticatedActionRunner,
    ) : ViewModel() {
        val message = workspaceManager.message

        private val _searchQuery = MutableStateFlow("")
        val searchQuery = _searchQuery.asStateFlow()

        private val _showAll = MutableStateFlow(value = false)
        val showAll = _showAll.asStateFlow()

        private val _isFormDirty = MutableStateFlow(value = false)
        val isFormDirty = _isFormDirty.asStateFlow()

        val currentUser = authRepository.currentUser

        init {
            observeResetSignal()
        }

        private fun observeResetSignal() {
            viewModelScope.launch {
                workspaceManager.resetSignal.collect { signal ->
                    if (signal > 0) {
                        _searchQuery.value = ""
                        _showAll.value = false
                    }
                }
            }
        }

        val customerSuggestions: StateFlow<List<Customer>?> =
            searchQuery
                .debounce(300.milliseconds)
                .flatMapLatest { query ->
                    customerRepository.search(query)
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    null,
                )

        fun setShowAll(enabled: Boolean) {
            _showAll.value = enabled
            if (enabled) {
                _searchQuery.value = ""
            }
        }

        fun setFormDirty(dirty: Boolean) {
            _isFormDirty.value = dirty
            workspaceManager.setFormDirty(dirty)
        }

        fun clearMessage() {
            workspaceManager.clearMessage()
        }

        fun setSearchQuery(query: String) {
            _searchQuery.value = query
        }

        fun saveCustomer(
            input: CustomerInput,
            onSaved: (Customer) -> Unit = {},
        ) {
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
            ) { actor ->
                customerRepository.save(actor, input).onSuccess {
                    onSaved(it)
                    setFormDirty(dirty = false)
                }
            }
        }

        fun deleteCustomer(
            customerId: String,
            onDeleted: () -> Unit = {},
        ) {
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
            ) { actor ->
                customerRepository.delete(actor, customerId).onSuccess {
                    onDeleted()
                }
            }
        }

        fun observeOrdersForCustomer(
            customerId: String,
            closed: Boolean,
        ): Flow<List<OrderListItem>> =
            currentUser.flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    val filter =
                        OrderFilter(
                            customerId = customerId,
                            statuses =
                                if (closed) {
                                    setOf(OrderStatus.CLOSED)
                                } else {
                                    OrderStatus.entries
                                        .asSequence()
                                        .filter { it != OrderStatus.CLOSED }
                                        .toSet()
                                },
                        )
                    orderWorkflowRepository.observeOrderList(user, filter)
                }
            }
    }
