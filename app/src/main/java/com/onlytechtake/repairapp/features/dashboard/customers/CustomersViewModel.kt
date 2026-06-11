package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.error.ErrorHandler
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CustomersViewModel(
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
    private val errorHandler: ErrorHandler,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val customerSuggestions: StateFlow<List<CustomerEntity>> = _searchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) customerRepository.observeRecent()
            else customerRepository.search(query)
        }
        .catch { errorHandler.handleError(it, "CustomersViewModel") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val actionRunner = AuthenticatedActionRunner(
        authRepository = authRepository,
        currentUser = authRepository.currentUser,
        scope = viewModelScope,
        showSuccess = { /* Handle success feedback */ },
        showError = { errorHandler.handleError(it) },
        showMessage = { _, _ -> /* Handle messages */ }
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCustomer(input: CustomerInput, onSaved: (CustomerEntity) -> Unit = {}) {
        actionRunner.run {
            customerRepository.save(input).onSuccess(onSaved)
        }
    }

    class Factory(
        private val customerRepository: CustomerRepository,
        private val authRepository: AuthRepository,
        private val errorHandler: ErrorHandler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CustomersViewModel(customerRepository, authRepository, errorHandler) as T
        }
    }
}
