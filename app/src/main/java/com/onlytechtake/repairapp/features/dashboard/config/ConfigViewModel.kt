package com.onlytechtake.repairapp.features.dashboard.config

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.error.ErrorHandler
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.ExportDestination
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import kotlinx.coroutines.flow.*

class ConfigViewModel(
    private val catalogRepository: CatalogRepository,
    private val templateRepository: TemplateRepository,
    private val backupRepository: BackupRepository,
    private val authRepository: AuthRepository,
    private val errorHandler: ErrorHandler,
) : ViewModel() {

    val categories: StateFlow<List<OrderCategoryEntity>> = catalogRepository.observeCategories()
        .catch { errorHandler.handleError(it, "ConfigViewModel") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates = templateRepository.observeTemplates()
        .catch { errorHandler.handleError(it, "ConfigViewModel") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messageMode = templateRepository.observeMessageMode()
        .catch { errorHandler.handleError(it, "ConfigViewModel") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessageMode.MANUAL_WHATSAPP)

    private val _exportDestination = MutableStateFlow(ExportDestination.LOCAL)
    val exportDestination = _exportDestination.asStateFlow()

    private val actionRunner = AuthenticatedActionRunner(
        authRepository = authRepository,
        currentUser = authRepository.currentUser,
        scope = viewModelScope,
        showSuccess = { /* success feedback */ },
        showError = { errorHandler.handleError(it) },
        showMessage = { _, _ -> /* message feedback */ }
    )

    fun saveCategory(id: String?, parentId: String?, name: String, description: String, active: Boolean) {
        actionRunner.run { actor ->
            catalogRepository.saveCategory(
                actor,
                CategoryInput(id, parentId, name, description, active)
            )
        }
    }

    fun deleteCategory(categoryId: String) {
        actionRunner.run { actor ->
            catalogRepository.deleteCategory(actor, categoryId)
        }
    }

    fun saveTemplate(code: String, title: String, body: String, active: Boolean) {
        actionRunner.run { actor ->
            templateRepository.saveTemplate(actor, code, title, body, active)
        }
    }

    fun setMessageMode(mode: MessageMode) {
        actionRunner.run { actor ->
            templateRepository.setMessageMode(actor, mode)
        }
    }

    fun setExportDestination(destination: ExportDestination) {
        _exportDestination.value = destination
    }

    fun exportConfiguration(uri: Uri) {
        actionRunner.run { actor ->
            backupRepository.exportConfiguration(actor, uri)
        }
    }

    fun exportDatabase(uri: Uri) {
        actionRunner.run { actor ->
            backupRepository.exportDatabase(actor, uri)
        }
    }

    fun exportOrdersXlsx(uri: Uri) {
        actionRunner.run { actor ->
            backupRepository.exportOrdersXlsx(actor, uri)
        }
    }

    fun exportCompleteBackup(uri: Uri) {
        actionRunner.run { actor ->
            backupRepository.exportCompleteBackup(actor, uri)
        }
    }

    fun restoreCompleteBackup(uri: Uri, onRestored: () -> Unit) {
        actionRunner.run { actor ->
            backupRepository.restoreCompleteBackup(actor, uri).onSuccess { onRestored() }
        }
    }

    class Factory(
        private val catalogRepository: CatalogRepository,
        private val templateRepository: TemplateRepository,
        private val backupRepository: BackupRepository,
        private val authRepository: AuthRepository,
        private val errorHandler: ErrorHandler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConfigViewModel(
                catalogRepository,
                templateRepository,
                backupRepository,
                authRepository,
                errorHandler
            ) as T
        }
    }
}
