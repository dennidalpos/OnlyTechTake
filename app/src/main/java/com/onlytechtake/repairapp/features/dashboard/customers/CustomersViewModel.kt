package com.onlytechtake.repairapp.features.dashboard.customers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.models.Customer
import com.onlytechtake.repairapp.domain.models.CustomerInput
import com.onlytechtake.repairapp.domain.models.OrderFilter
import com.onlytechtake.repairapp.domain.models.OrderListItem
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.repositories.CustomerRepository
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
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

        private val _viewState = MutableStateFlow(CustomerViewState())
        val viewState = _viewState.asStateFlow()

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

        fun onSelectedCustomerChange(customer: Customer?) {
            _viewState.value = _viewState.value.copy(selectedCustomer = customer)
        }

        fun onShowFormChange(show: Boolean) {
            _viewState.value = _viewState.value.copy(showForm = show)
            if (!show) clearForm()
        }

        fun onBusinessChange(isBusiness: Boolean) {
            _viewState.value = _viewState.value.copy(isBusiness = isBusiness)
            setFormDirty(true)
        }

        fun onFirstNameChange(name: String) {
            _viewState.value = _viewState.value.copy(firstName = name)
            setFormDirty(true)
        }

        fun onLastNameChange(name: String) {
            _viewState.value = _viewState.value.copy(lastName = name)
            setFormDirty(true)
        }

        fun onCompanyNameChange(name: String) {
            _viewState.value = _viewState.value.copy(companyName = name)
            setFormDirty(true)
        }

        fun onPhoneChange(phone: String) {
            _viewState.value = _viewState.value.copy(phone = phone)
            setFormDirty(true)
        }

        fun onEmailChange(email: String) {
            _viewState.value = _viewState.value.copy(email = email)
            setFormDirty(true)
        }

        fun onTaxCodeChange(code: String) {
            _viewState.value = _viewState.value.copy(taxCode = code)
            setFormDirty(true)
        }

        fun onVatNumberChange(vat: String) {
            _viewState.value = _viewState.value.copy(vatNumber = vat)
            setFormDirty(true)
        }

        fun onAddressChange(address: String) {
            _viewState.value = _viewState.value.copy(address = address)
            setFormDirty(true)
        }

        fun onNotesChange(notes: String) {
            _viewState.value = _viewState.value.copy(notes = notes)
            setFormDirty(true)
        }

        fun editCustomer(customer: Customer) {
            _viewState.value =
                _viewState.value.copy(
                    editingCustomerId = customer.id,
                    isBusiness = customer.type == com.onlytechtake.repairapp.domain.models.CustomerType.BUSINESS,
                    firstName = customer.firstName.orEmpty(),
                    lastName = customer.lastName.orEmpty(),
                    companyName = customer.companyName.orEmpty(),
                    phone = customer.phone.orEmpty(),
                    email = customer.email.orEmpty(),
                    taxCode = customer.taxCode.orEmpty(),
                    vatNumber = customer.vatNumber.orEmpty(),
                    address = customer.address.orEmpty(),
                    notes = customer.notes.orEmpty(),
                    showForm = true,
                )
        }

        fun clearForm() {
            _viewState.value =
                _viewState.value.copy(
                    editingCustomerId = null,
                    isBusiness = false,
                    firstName = "",
                    lastName = "",
                    companyName = "",
                    phone = "",
                    email = "",
                    taxCode = "",
                    vatNumber = "",
                    address = "",
                    notes = "",
                )
            setFormDirty(false)
        }

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
