package com.onlytechtake.repairapp.features.dashboard.neworder

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.models.*
import com.onlytechtake.repairapp.domain.models.CustomerInput
import com.onlytechtake.repairapp.domain.models.IntakeOrderInput
import com.onlytechtake.repairapp.domain.repositories.CatalogRepository
import com.onlytechtake.repairapp.domain.repositories.CustomerRepository
import com.onlytechtake.repairapp.domain.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.domain.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.usecases.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.features.workspace.QuickIntakeDraft
import com.onlytechtake.repairapp.features.workspace.WorkspaceManager
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class NewOrderViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        private val customerRepository: CustomerRepository,
        catalogRepository: CatalogRepository,
        private val orderMediaRepository: OrderMediaRepository,
        templateRepository: TemplateRepository,
        private val createIntakeOrderUseCase: CreateIntakeOrderUseCase,
        private val workspaceManager: WorkspaceManager,
        private val actionRunner: AuthenticatedActionRunner,
    ) : ViewModel() {
        val message = workspaceManager.message

        private val _quickIntakeDraft = MutableStateFlow(QuickIntakeDraft())
        val quickIntakeDraft = _quickIntakeDraft.asStateFlow()

        private val _customerSearchQuery = MutableStateFlow("")
        val customerSearchQuery = _customerSearchQuery.asStateFlow()

        val currentUser = authRepository.currentUser

        val customerSuggestions: StateFlow<List<Customer>?> =
            customerSearchQuery
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    customerRepository.search(query)
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    null,
                )

        val categories: StateFlow<List<Category>> =
            catalogRepository
                .observeActiveCategories()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val templates: StateFlow<List<Template>> =
            templateRepository
                .observeTemplates()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val anchorMappings: StateFlow<Map<TemplateAnchor, String>> =
            templateRepository
                .observeAnchorMappings()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

        init {
            checkDemoLimit()
            observeResetSignal()
        }

        private fun observeResetSignal() {
            viewModelScope.launch {
                workspaceManager.resetSignal.collect { signal ->
                    if (signal > 0) {
                        resetQuickIntakeDraft()
                    }
                }
            }
        }

        private fun checkDemoLimit() {
            if (BuildConfig.IS_DEMO_VERSION) {
                viewModelScope.launch {
                    val count = createIntakeOrderUseCase.countAllOrders()
                    if (count >= 1) {
                        workspaceManager.showMessage(
                            createIntakeOrderUseCase.getDemoLimitErrorMessage(),
                            isError = false,
                        )
                    }
                }
            }
        }

        fun setFormDirty(dirty: Boolean) {
            workspaceManager.setFormDirty(dirty)
        }

        fun clearMessage() {
            workspaceManager.clearMessage()
        }

        fun updateQuickIntakeDraft(update: (QuickIntakeDraft) -> QuickIntakeDraft) {
            _quickIntakeDraft.value = update(_quickIntakeDraft.value)
        }

        fun setCustomerSearchQuery(query: String) {
            _customerSearchQuery.value = query
        }

        fun resetQuickIntakeDraft() {
            _quickIntakeDraft.value = QuickIntakeDraft()
            _customerSearchQuery.value = ""
            setFormDirty(dirty = false)
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

        fun importIntakeAttachment(
            label: String,
            uri: Uri,
            onSuccess: (StagedOrderMedia) -> Unit,
        ) {
            viewModelScope.launch {
                orderMediaRepository
                    .importIntakeAttachment(label, uri)
                    .onSuccess {
                        onSuccess(it)
                        setFormDirty(dirty = true)
                    }.onFailure {
                        workspaceManager.showMessage(it.message ?: "Errore", isError = true)
                    }
            }
        }

        fun deleteStagedMedia(fileName: String) {
            viewModelScope.launch {
                orderMediaRepository.deleteStagedMedia(fileName)
            }
        }

        fun replaceStagedImage(
            staged: StagedOrderMedia,
            bitmap: Bitmap,
            onSuccess: (StagedOrderMedia) -> Unit,
        ) {
            viewModelScope.launch {
                orderMediaRepository
                    .replaceStagedImage(staged, bitmap)
                    .onSuccess {
                        onSuccess(it)
                        setFormDirty(dirty = true)
                    }.onFailure {
                        workspaceManager.showMessage(it.message ?: "Errore", isError = true)
                    }
            }
        }

        fun createIntakePhotoFile(sessionId: String): Result<File> = runCatching { orderMediaRepository.createIntakePhotoFile(sessionId) }

        fun createIntakeVideoFile(sessionId: String): Result<File> = runCatching { orderMediaRepository.createIntakeVideoFile(sessionId) }

        fun stageCapturedIntakePhoto(
            file: File,
            onSuccess: (StagedOrderMedia) -> Unit,
        ) {
            viewModelScope.launch {
                orderMediaRepository
                    .stageCapturedIntakePhoto(file)
                    .onSuccess {
                        onSuccess(it)
                        setFormDirty(dirty = true)
                    }.onFailure {
                        workspaceManager.showMessage(it.message ?: "Errore", isError = true)
                    }
            }
        }

        fun stageCapturedIntakeVideo(
            file: File,
            onSuccess: (StagedOrderMedia) -> Unit,
        ) {
            viewModelScope.launch {
                orderMediaRepository
                    .stageCapturedIntakeVideo(file)
                    .onSuccess {
                        onSuccess(it)
                        setFormDirty(dirty = true)
                    }.onFailure {
                        workspaceManager.showMessage(it.message ?: "Errore", isError = true)
                    }
            }
        }

        fun reportCameraError(message: String) {
            workspaceManager.showMessage(message, isError = true)
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
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                onSuccess = onCreated,
            ) { actor ->
                createIntakeOrderUseCase.execute(
                    actor = actor,
                    input = input,
                    stagedMedia = stagedMedia,
                    signerName = signerName,
                    privacyAccepted = privacyAccepted,
                    termsAccepted = termsAccepted,
                    signatureBitmap = signatureBitmap,
                )
            }
        }
    }
