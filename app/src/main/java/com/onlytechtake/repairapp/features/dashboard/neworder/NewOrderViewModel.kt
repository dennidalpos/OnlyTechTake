package com.onlytechtake.repairapp.features.dashboard.neworder

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.features.workspace.QuickIntakeDraft
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NewOrderViewModel(
    authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    catalogRepository: CatalogRepository,
    private val orderMediaRepository: OrderMediaRepository,
    private val templateRepository: TemplateRepository,
    private val createIntakeOrderUseCase: CreateIntakeOrderUseCase,
    private val onSetFormDirty: (Boolean) -> Unit,
) : ViewModel() {
    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _quickIntakeDraft = MutableStateFlow(QuickIntakeDraft())
    val quickIntakeDraft = _quickIntakeDraft.asStateFlow()

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    val currentUser = authRepository.currentUser

    val customerSuggestions =
        _customerSearchQuery
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { query -> if (query.isBlank()) 0L else 300L }
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    customerRepository.observeRecent()
                } else {
                    customerRepository.search(
                        query,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories =
        catalogRepository
            .observeActiveCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates =
        templateRepository
            .observeTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val anchorMappings =
        templateRepository
            .observeAnchorMappings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        checkDemoLimit()
    }

    private fun checkDemoLimit() {
        if ((BuildConfig.IS_DEMO_VERSION) && (BuildConfig.MAX_ORDERS > 0)) {
            viewModelScope.launch {
                val totalOrders = createIntakeOrderUseCase.countAllOrders()
                if (totalOrders >= BuildConfig.MAX_ORDERS) {
                    _message.value =
                        WorkspaceMessage(
                            createIntakeOrderUseCase.getDemoLimitErrorMessage(),
                            isError = true,
                        )
                }
            }
        }
    }

    fun setFormDirty(dirty: Boolean) {
        onSetFormDirty(dirty)
    }

    fun clearMessage() {
        _message.value = null
    }

    fun updateQuickIntakeDraft(transform: (QuickIntakeDraft) -> QuickIntakeDraft) {
        _quickIntakeDraft.update(transform)
    }

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun resetQuickIntakeDraft() {
        _quickIntakeDraft.value = QuickIntakeDraft()
        onSetFormDirty(false)
        _customerSearchQuery.value = ""
    }

    fun saveCustomer(
        input: CustomerInput,
        onSaved: (CustomerEntity) -> Unit,
    ) {
        viewModelScope.launch {
            customerRepository.save(currentUser.value, input).fold(
                onSuccess = { onSaved(it) },
            ) { error ->
                _message.value = WorkspaceMessage(error.message ?: "Errore", isError = true)
            }
        }
    }

    fun importIntakeAttachment(
        sessionId: String,
        uri: Uri,
        onStaged: (StagedOrderMedia) -> Unit,
    ) {
        viewModelScope.launch {
            orderMediaRepository.importIntakeAttachment(sessionId, uri).fold(
                onSuccess = { onStaged(it) },
            ) { error ->
                _message.value = WorkspaceMessage(error.message ?: "Errore", isError = true)
            }
        }
    }

    fun deleteStagedMedia(path: String) {
        viewModelScope.launch {
            orderMediaRepository.deleteStagedMedia(path)
        }
    }

    fun replaceStagedImage(
        staged: StagedOrderMedia,
        bitmap: Bitmap,
        onReplaced: (StagedOrderMedia) -> Unit,
    ) {
        viewModelScope.launch {
            orderMediaRepository.replaceStagedImage(staged, bitmap).fold(
                onSuccess = { onReplaced(it) },
            ) { error ->
                _message.value = WorkspaceMessage(error.message ?: "Errore", isError = true)
            }
        }
    }

    fun createIntakePhotoFile(sessionId: String): Result<File> =
        runCatching { orderMediaRepository.createIntakePhotoFile(sessionId) }

    fun stageCapturedIntakePhoto(
        file: File,
        onStaged: (StagedOrderMedia) -> Unit,
    ) {
        viewModelScope.launch {
            orderMediaRepository.stageCapturedIntakePhoto(file).fold(
                onSuccess = { onStaged(it) },
            ) { error ->
                _message.value = WorkspaceMessage(error.message ?: "Errore", isError = true)
            }
        }
    }

    fun reportCameraError(message: String) {
        _message.value = WorkspaceMessage(message, isError = true)
    }

    fun createIntakeOrder(
        input: IntakeOrderInput,
        stagedMedia: List<StagedOrderMedia>,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        signatureBitmap: Bitmap,
        onCreated: () -> Unit,
    ) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            createIntakeOrderUseCase
                .execute(
                    actor = actor,
                    input = input,
                    stagedMedia = stagedMedia,
                    signerName = signerName,
                    privacyAccepted = privacyAccepted,
                    termsAccepted = termsAccepted,
                    signatureBitmap = signatureBitmap,
                ).fold(
                    onSuccess = { onCreated() },
                ) { error ->
                    _message.value = WorkspaceMessage(error.message ?: "Errore", isError = true)
                }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val customerRepository: CustomerRepository,
        private val catalogRepository: CatalogRepository,
        private val orderMediaRepository: OrderMediaRepository,
        private val templateRepository: TemplateRepository,
        private val createIntakeOrderUseCase: CreateIntakeOrderUseCase,
        private val onSetFormDirty: (Boolean) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NewOrderViewModel(
                authRepository,
                customerRepository,
                catalogRepository,
                orderMediaRepository,
                templateRepository,
                createIntakeOrderUseCase,
                onSetFormDirty,
            ) as T
    }
}
