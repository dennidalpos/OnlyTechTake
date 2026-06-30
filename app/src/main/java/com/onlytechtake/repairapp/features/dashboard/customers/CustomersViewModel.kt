package com.onlytechtake.repairapp.features.dashboard.customers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.repositories.CustomerRepository
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderFilterMode
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class CustomersViewModel(
    context: Context,
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val orderWorkflowRepository: OrderRepository,
    private val onSetFormDirty: (Boolean) -> Unit,
    diagnosticLogger: DiagnosticLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showAll = MutableStateFlow(value = false)
    val showAll = _showAll.asStateFlow()

    private val actionRunner =
        AuthenticatedActionRunner(
            context = applicationContext,
            authRepository = authRepository,
            currentUser = authRepository.currentUser,
            scope = viewModelScope,
            showMessage = { text, isError -> _message.value = WorkspaceMessage(text, isError) },
            diagnosticLogger = diagnosticLogger,
            ioDispatcher = ioDispatcher,
        )

    private val _isFormDirty = MutableStateFlow(value = false)

    val currentUser = authRepository.currentUser

    val customerSuggestions: StateFlow<List<CustomerEntity>?> =
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
        actionRunner.run(successMessageRes = R.string.success_operation_completed) { actor ->
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
        actionRunner.run(successMessageRes = R.string.success_operation_completed) { actor ->
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
                        mode = if (closed) OrderFilterMode.CLOSED else OrderFilterMode.ALL,
                    )
                orderWorkflowRepository.observeOrderList(user, filter)
            }
        }

    class Factory(
        private val context: Context,
        private val authRepository: AuthRepository,
        private val customerRepository: CustomerRepository,
        private val orderWorkflowRepository: OrderRepository,
        private val diagnosticLogger: DiagnosticLogger,
        private val onSetFormDirty: (Boolean) -> Unit,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CustomersViewModel(
                context,
                authRepository,
                customerRepository,
                orderWorkflowRepository,
                onSetFormDirty,
                diagnosticLogger,
                ioDispatcher,
            ) as T
    }
}
