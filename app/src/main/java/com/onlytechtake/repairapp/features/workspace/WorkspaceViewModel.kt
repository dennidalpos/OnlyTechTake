package com.onlytechtake.repairapp.features.workspace

import android.net.Uri
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.ExportDestination
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModel(
    private val authRepository: AuthRepository,
    private val adminUserRepository: AdminUserRepository,
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val orderRepository: OrderWorkflowRepository,
    private val orderMediaRepository: OrderMediaRepository,
    private val signatureRepository: SignatureRepository,
    private val templateRepository: TemplateRepository,
    private val backupRepository: BackupRepository,
    private val backupReminderPreferences: BackupReminderPreferences,
    private val dataResetRepository: DataResetRepository,
    private val createIntakeOrderUseCase: CreateIntakeOrderUseCase,
    private val orderDocumentRepository: OrderDocumentRepository,
    private val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase,
    private val shareDocumentIntentFactory: ShareDocumentIntentFactory,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = mutableUiState

    private val mutableQuickIntakeDraft = MutableStateFlow(QuickIntakeDraft())
    val quickIntakeDraft: StateFlow<QuickIntakeDraft> = mutableQuickIntakeDraft.asStateFlow()

    private val _isFormDirty = MutableStateFlow(false)
    val isFormDirty = _isFormDirty.asStateFlow()

    fun setFormDirty(dirty: Boolean) {
        _isFormDirty.value = dirty
    }

    fun updateQuickIntakeDraft(transform: (QuickIntakeDraft) -> QuickIntakeDraft) {
        mutableQuickIntakeDraft.update(transform)
    }

    fun resetQuickIntakeDraft() {
        mutableQuickIntakeDraft.value = QuickIntakeDraft()
        setFormDirty(false)
        setCustomerSearchQuery("")
    }

    fun openCategoryCreationFromIntake() {
        mutableQuickIntakeDraft.update {
            it.copy(
                currentStep = 2,
                returnToIntakeAfterCategorySave = true,
            )
        }
        navigateTo(WorkspaceDestination.CONFIG_CATEGORIES)
    }

    val currentUser = authRepository.currentUser
    val users = adminUserRepository.observeUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val customers = customerRepository.observeRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val customerSuggestions = uiState
        .map { it.customerSearchQuery.trim() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) customerRepository.observeRecent() else customerRepository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = catalogRepository.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates = templateRepository.observeTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val messageMode = templateRepository.observeMessageMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessageMode.MANUAL_WHATSAPP)
    val backupReminderSettings = backupReminderPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupReminderSettings())

    private val _exportDestination = MutableStateFlow(ExportDestination.LOCAL)
    val exportDestination = _exportDestination.asStateFlow()

    private val _isCloudAvailable = MutableStateFlow(false)
    val isCloudAvailable = _isCloudAvailable.asStateFlow()

    val orderListItems = combine(currentUser, uiState) { user, state -> user to state.orderFilter }
        .flatMapLatest { (user, filter) ->
            if (user == null) flowOf(emptyList()) else orderRepository.observeOrderList(user, filter)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val operationalQueues = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else orderRepository.observeOrders(user, OrderFilter())
        }
        .map(::buildOperationalQueues)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildOperationalQueues(emptyList()))

    val selectedOrder = uiState
        .flatMapLatest { state ->
            state.selectedOrderId?.let { orderRepository.observeOrderDetail(it) } ?: flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedOrderDocuments = uiState
        .flatMapLatest { state ->
            state.selectedOrderId?.let { orderDocumentRepository.observeByOrder(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val authenticatedActionRunner = AuthenticatedActionRunner(
        authRepository = authRepository,
        currentUser = currentUser,
        scope = viewModelScope,
        showSuccess = ::showSuccess,
        showError = ::showError,
        showMessage = ::showMessage,
    )

    private val mediaActions = WorkspaceMediaActions(
        authenticatedActionRunner = authenticatedActionRunner,
        selectedOrder = selectedOrder,
        mutableUiState = mutableUiState,
        scope = viewModelScope,
        orderMediaRepository = orderMediaRepository,
        signatureRepository = signatureRepository,
        createIntakeOrderUseCase = createIntakeOrderUseCase,
        showSuccess = ::showSuccess,
        showError = ::showError,
        showMessage = ::showMessage,
    )

    private val messageActions = WorkspaceMessageActions(
        authenticatedActionRunner = authenticatedActionRunner,
        mutableUiState = mutableUiState,
        templateRepository = templateRepository,
        showSuccess = ::showSuccess,
        showError = ::showError,
        showMessage = ::showMessage,
    )

    private val adminActions = WorkspaceAdminActions(
        authenticatedActionRunner = authenticatedActionRunner,
        mutableUiState = mutableUiState,
        backupRepository = backupRepository,
        backupReminderPreferences = backupReminderPreferences,
        dataResetRepository = dataResetRepository,
        orderRepository = orderRepository,
        showSuccess = ::showSuccess,
        showError = ::showError,
    )

    private val catalogActions = WorkspaceCatalogActions(
        authenticatedActionRunner = authenticatedActionRunner,
        scope = viewModelScope,
        adminUserRepository = adminUserRepository,
        customerRepository = customerRepository,
        catalogRepository = catalogRepository,
        showSuccess = ::showSuccess,
        showError = ::showError,
    )

    init {
        viewModelScope.launch {
            templateRepository.ensureDefaults(currentUser.value?.id)
            _isCloudAvailable.value = backupRepository.isCloudProviderAvailable()
            if (_isCloudAvailable.value) {
                _exportDestination.value = ExportDestination.CLOUD
            }
        }
    }

    fun clearMessage() {
        mutableUiState.update { it.copy(message = null) }
    }

    fun reportCameraError(message: String) {
        showMessage(message, isError = true)
    }

    fun setFilter(filter: OrderFilter) {
        mutableUiState.update { it.copy(orderFilter = filter) }
    }

    fun setCustomerSearchQuery(query: String) {
        mutableUiState.update { it.copy(customerSearchQuery = query) }
    }

    fun navigateTo(destination: WorkspaceDestination) {
        mutableUiState.update {
            it.copy(
                destination = destination,
                preparedMessage = null,
                preparedMessageBody = "",
            )
        }
    }

    fun openNewOrder() {
        mutableUiState.update {
            it.copy(
                destination = WorkspaceDestination.NEW_ORDER,
                selectedOrderId = null,
                preparedMessage = null,
                preparedMessageBody = "",
            )
        }
    }

    fun selectOrder(orderId: String?) {
        mutableUiState.update {
            it.copy(
                destination = if (orderId == null) WorkspaceDestination.ORDERS else WorkspaceDestination.ORDER_DETAIL,
                selectedOrderId = orderId,
                preparedMessage = null,
                preparedMessageBody = "",
            )
        }
    }

    fun createOperator(displayName: String, username: String, password: String) =
        catalogActions.createOperator(displayName, username, password)

    fun setUserActive(userId: String, active: Boolean) =
        catalogActions.setUserActive(userId, active)

    fun resetPassword(userId: String, password: String) =
        catalogActions.resetPassword(userId, password)

    fun saveCustomer(input: CustomerInput, onSaved: (CustomerEntity) -> Unit = {}) =
        catalogActions.saveCustomer(input, onSaved)

    fun saveCategory(
        id: String?,
        parentId: String?,
        name: String,
        description: String,
        active: Boolean,
    ) = catalogActions.saveCategory(id, parentId, name, description, active) { savedCategoryId ->
        val shouldReturnToIntake = mutableQuickIntakeDraft.value.returnToIntakeAfterCategorySave
        if (shouldReturnToIntake) {
            mutableQuickIntakeDraft.update {
                it.withCategoryCreatedFromIntake(savedCategoryId)
            }
            navigateTo(WorkspaceDestination.NEW_ORDER)
        }
    }

    fun deleteCategory(categoryId: String) =
        catalogActions.deleteCategory(categoryId)

    fun changeStatus(orderId: String, status: OrderStatus, note: String = "", closureReason: ClosureReason? = null) = withUser(
        onSuccess = messageActions::clearPreparedMessage,
    ) { actor ->
        orderRepository.changeStatus(actor, orderId, status, note, closureReason)
    }

    fun changeStatus(
        orderId: String,
        status: OrderStatus,
        note: String = "",
        closureReason: ClosureReason? = null,
        onSuccess: () -> Unit,
    ) = withUser(
        onSuccess = {
            messageActions.clearPreparedMessage()
            onSuccess()
            showSuccess("Stato aggiornato correttamente")
        },
    ) { actor ->
        orderRepository.changeStatus(actor, orderId, status, note, closureReason)
    }

    fun recordResult(
        orderId: String,
        summary: String,
        diagnosisOutcome: DiagnosisOutcome,
        estimateAmountCents: Long?,
        onSuccess: () -> Unit = {},
    ) = withUser(
        onSuccess = {
            messageActions.clearPreparedMessage()
            onSuccess()
        },
    ) { actor ->
        orderRepository.recordResult(
            actor,
            orderId,
            DiagnosisResultInput(summary, diagnosisOutcome, estimateAmountCents),
        )
    }

    fun reassign(orderId: String, userId: String?) = withUser { actor ->
        orderRepository.reassign(actor, orderId, userId)
    }

    fun togglePaymentStatus(orderId: String, isPaid: Boolean) = withUser { actor ->
        orderRepository.togglePaymentStatus(actor, orderId, isPaid)
    }

    fun addNote(orderId: String, note: String) = withUser { actor ->
        orderRepository.addInternalNote(actor, orderId, note)
    }

    fun deleteOrder(orderId: String, reason: String) = withUser { actor ->
        orderRepository.softDelete(actor, orderId, reason)
    }

    fun createPhotoFile(orderId: String, orderNumber: String): Result<File> =
        mediaActions.createPhotoFile(orderId, orderNumber)

    fun createIntakePhotoFile(sessionId: String): Result<File> =
        mediaActions.createIntakePhotoFile(sessionId)

    fun stageCapturedIntakePhoto(file: File, onStaged: (StagedOrderMedia) -> Unit) =
        mediaActions.stageCapturedIntakePhoto(file, onStaged)

    fun deleteStagedMedia(path: String) =
        mediaActions.deleteStagedMedia(path)

    fun replaceStagedImage(staged: StagedOrderMedia, bitmap: Bitmap, onReplaced: (StagedOrderMedia) -> Unit) =
        mediaActions.replaceStagedImage(staged, bitmap, onReplaced)

    fun importIntakeAttachment(sessionId: String, uri: Uri, onStaged: (StagedOrderMedia) -> Unit) =
        mediaActions.importIntakeAttachment(sessionId, uri, onStaged)

    fun createIntakeOrder(
        input: IntakeOrderInput,
        stagedMedia: List<StagedOrderMedia>,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        signatureBitmap: Bitmap,
        onCreated: () -> Unit = {},
    ) = mediaActions.createIntakeOrder(
        input = input,
        stagedMedia = stagedMedia,
        signerName = signerName,
        privacyAccepted = privacyAccepted,
        termsAccepted = termsAccepted,
        signatureBitmap = signatureBitmap,
        onCreated = onCreated,
    )

    fun registerCapturedPhoto(orderId: String, file: File) =
        mediaActions.registerCapturedPhoto(orderId, file)

    fun registerCapturedFinalPhoto(orderId: String, file: File) =
        mediaActions.registerCapturedFinalPhoto(orderId, file)

    fun importAttachment(orderId: String, uri: Uri) =
        mediaActions.importAttachment(orderId, uri)

    fun deleteMedia(mediaId: String) =
        mediaActions.deleteMedia(mediaId)

    fun replaceImage(mediaId: String, bitmap: Bitmap) =
        mediaActions.replaceImage(mediaId, bitmap)

    fun saveSignature(
        orderId: String,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        bitmap: Bitmap,
    ) = mediaActions.saveSignature(
        orderId = orderId,
        signerName = signerName,
        privacyAccepted = privacyAccepted,
        termsAccepted = termsAccepted,
        bitmap = bitmap,
    )

    fun recordFeedback(
        orderId: String,
        feedback: CustomerFeedbackType,
        note: String,
        closureReason: ClosureReason? = null,
        onSuccess: () -> Unit = {},
    ) = withUser(
        onSuccess = {
            messageActions.clearPreparedMessage()
            onSuccess()
        },
    ) { actor ->
        orderRepository.recordFeedback(actor, orderId, FeedbackInput(feedback, note, closureReason))
    }

    fun prepareStatusMessage(orderId: String, status: OrderStatus) =
        messageActions.prepareStatusMessage(orderId, status)

    fun saveTemplate(code: String, title: String, body: String, active: Boolean) = withUser { actor ->
        templateRepository.saveTemplate(actor, code, title, body, active)
    }

    fun setMessageMode(mode: MessageMode) = withUser { actor ->
        templateRepository.setMessageMode(actor, mode)
    }

    fun setExportDestination(destination: ExportDestination) {
        _exportDestination.value = destination
    }

    fun setBackupReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupReminderPreferences.setEnabled(enabled)
        }
    }

    fun setBackupReminderIntervalDays(days: Int) {
        viewModelScope.launch {
            backupReminderPreferences.setIntervalDays(days)
        }
    }

    fun isBackupReminderDue(settings: BackupReminderSettings, nowMillis: Long = System.currentTimeMillis()): Boolean =
        BackupReminderPolicy.shouldShowReminder(settings, nowMillis)

    fun prepareMessage(orderId: String, templateCode: String) =
        messageActions.prepareMessage(orderId, templateCode)

    fun updatePreparedMessageBody(body: String) =
        messageActions.updatePreparedMessageBody(body)

    fun clearPreparedMessage() = messageActions.clearPreparedMessage()

    fun recordPreparedMessage() = messageActions.recordPreparedMessage()

    fun confirmSentMessage() = messageActions.confirmSentMessage()

    fun exportConfiguration(uri: Uri) =
        adminActions.exportConfiguration(uri)

    fun exportDatabase(uri: Uri) =
        adminActions.exportDatabase(uri)

    fun exportCompleteBackup(uri: Uri) =
        adminActions.exportCompleteBackup(uri)

    fun exportOrdersXlsx(uri: Uri) =
        adminActions.exportOrdersXlsx(uri)

    fun restoreCompleteBackup(uri: Uri, onRestored: () -> Unit) =
        adminActions.restoreCompleteBackup(uri, onRestored)

    fun exportSelectedOrderPdf(uri: Uri) =
        mediaActions.exportSelectedOrderPdf(backupRepository, uri)

    fun resetAllData(confirmation: String, onReset: () -> Unit) =
        adminActions.resetAllData(confirmation, onReset)

    fun archiveDiscardedClosedOrders() =
        adminActions.archiveDiscardedClosedOrders()

    fun deleteArchivedOrders() =
        adminActions.deleteArchivedOrders()

    fun onGenerateStatusPdfClicked(orderId: String, includeSignature: Boolean = true) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isGeneratingStatusPdf = true, statusPdfError = null) }
            generateOrderStatusPdfUseCase(orderId, actor.id, includeSignature)
                .onSuccess { document ->
                    mutableUiState.update { it.copy(
                        isGeneratingStatusPdf = false,
                        lastGeneratedDocumentId = document.id,
                    ) }
                    showSuccess("PDF stato avanzamento generato")
                    // Automatically trigger share after generation
                    triggerShare(document.localPath, "Stato avanzamento ${document.fileName}")
                    mutableUiState.update { it.copy(showPdfSentConfirmation = true) }
                }
                .onFailure { error ->
                    mutableUiState.update { it.copy(isGeneratingStatusPdf = false, statusPdfError = error.message) }
                    showError(error)
                }
        }
    }

    fun onShareDocumentClicked(documentId: String) {
        viewModelScope.launch {
            val document = orderDocumentRepository.findById(documentId)
            if (document != null) {
                mutableUiState.update { it.copy(lastGeneratedDocumentId = documentId) }
                triggerShare(document.localPath, "Documento ${document.fileName}")
                mutableUiState.update { it.copy(showPdfSentConfirmation = true) }
            } else {
                showMessage("Documento non trovato", isError = true)
            }
        }
    }

    fun onDeleteDocumentClicked(documentId: String) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            val document = orderDocumentRepository.findById(documentId)
            if (document != null) {
                orderDocumentRepository.delete(documentId).fold(
                    onSuccess = {
                        showSuccess("Documento eliminato")
                        selectedOrder.value?.order?.id?.let { orderId ->
                            orderRepository.recordMediaRemoved(actor, orderId, document.fileName)
                        }
                    },
                    onFailure = ::showError
                )
            }
        }
    }

    private fun triggerShare(localPath: String, title: String) {
        val file = File(localPath)
        if (file.exists()) {
            val intent = shareDocumentIntentFactory.createPdfShareIntent(file, title)
            // This is tricky because ViewModel doesn't have Context to start activity easily.
            // Usually we'd use a Flow/Event for UI to handle this.
            // I'll add a sharedIntent to UiState.
            mutableUiState.update { it.copy(pendingShareIntent = intent) }
        } else {
            showMessage("File non trovato", isError = true)
        }
    }

    fun onPdfSentConfirmed() {
        val actor = currentUser.value ?: return
        val documentId = uiState.value.lastGeneratedDocumentId ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            orderDocumentRepository.markShared(documentId, now)
            val doc = orderDocumentRepository.findById(documentId)
            selectedOrder.value?.order?.id?.let { orderId ->
                doc?.let {
                    orderRepository.recordPdfShared(actor, orderId, it.fileName)
                }
            }
            mutableUiState.update { it.copy(showPdfSentConfirmation = false, lastGeneratedDocumentId = null) }
            showSuccess("Invio confermato")
        }
    }

    fun onPdfSentDismissed() {
        mutableUiState.update { it.copy(showPdfSentConfirmation = false) }
    }

    fun clearPendingShareIntent() {
        mutableUiState.update { it.copy(pendingShareIntent = null) }
    }

    private fun withUser(
        onSuccess: () -> Unit = {},
        block: suspend (com.onlytechtake.repairapp.domain.auth.AuthenticatedUser) -> Result<*>,
    ) {
        authenticatedActionRunner.run(onSuccess = onSuccess, block = block)
    }

    private fun showSuccess(text: String) = showMessage(text, isError = false)

    private fun showError(error: Throwable) = showMessage(error.message ?: "Operazione non riuscita", isError = true)

    private fun showMessage(text: String, isError: Boolean) {
        mutableUiState.update { it.copy(message = WorkspaceMessage(text, isError)) }
    }

}
