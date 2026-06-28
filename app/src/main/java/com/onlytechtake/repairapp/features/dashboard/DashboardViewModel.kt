package com.onlytechtake.repairapp.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    repository: RepairAppRepository,
) : ViewModel() {
    val summary: StateFlow<AppSummary?> =
        repository
            .observeSummary()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    class Factory(
        private val repository: RepairAppRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return DashboardViewModel(repository) as T
        }
    }
}
