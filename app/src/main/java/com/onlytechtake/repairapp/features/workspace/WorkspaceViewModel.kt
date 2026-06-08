package com.onlytechtake.repairapp.features.workspace

import android.net.Uri
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val dataResetRepository: DataResetRepository,
    private val createIntakeOrderUseCase: CreateIntakeOrderUseCase = CreateIntakeOrderUseCase(
        customerRepository = customerRepository,
        orderRepository = orderRepository,
        orderMediaRepository = orderMediaRepository,
        signatureRepository = signatureRepository,
    ),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = mutableUiState

    val currentUser = authRepository.currentUser
    val users = adminUserRepository.observeUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeUsers = adminUserRepository.observeActiveUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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

    private val mediaActions = WorkspaceMediaActions(
        authRepository = authRepository,
        currentUser = currentUser,
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
        authRepository = authRepository,
        currentUser = currentUser,
        mutableUiState = mutableUiState,
        scope = viewModelScope,
        templateRepository = templateRepository,
        showSuccess = ::showSuccess,
        showError = ::showError,
        showMessage = ::showMessage,
    )

    private val adminActions = WorkspaceAdminActions(
        authRepository = authRepository,
        currentUser = currentUser,
        mutableUiState = mutableUiState,
        scope = viewModelScope,
        backupRepository = backupRepository,
        dataResetRepository = dataResetRepository,
        orderRepository = orderRepository,
        showSuccess = ::showSuccess,
        showError = ::showError,
        showMessage = ::showMessage,
    )

    private val catalogActions = WorkspaceCatalogActions(
        authRepository = authRepository,
        currentUser = currentUser,
        scope = viewModelScope,
        adminUserRepository = adminUserRepository,
        customerRepository = customerRepository,
        catalogRepository = catalogRepository,
        showSuccess = ::showSuccess,
        showError = ::showError,
        showMessage = ::showMessage,
    )

    init {
        viewModelScope.launch {
            templateRepository.ensureDefaults(currentUser.value?.id)
        }
    }

    fun clearMessage() {
        mutableUiState.update { it.copy(message = null) }
    }

    fun reportCameraError(message: String) {
        showMessage(message, true)
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

    fun goHome() {
        navigateTo(WorkspaceDestination.HOME)
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
        name: String,
        description: String,
        active: Boolean,
        sortOrder: Int,
    ) = catalogActions.saveCategory(id, name, description, active, sortOrder)

    fun changeStatus(orderId: String, status: OrderStatus, note: String = "", closureReason: ClosureReason? = null) = withUser { actor ->
        orderRepository.changeStatus(actor, orderId, status, note, closureReason)
    }

    fun changeStatus(
        orderId: String,
        status: OrderStatus,
        note: String = "",
        closureReason: ClosureReason? = null,
        onSuccess: () -> Unit,
    ) = withUser(onSuccess = onSuccess) { actor ->
        orderRepository.changeStatus(actor, orderId, status, note, closureReason)
    }

    fun recordResult(orderId: String, summary: String, estimateAmountCents: Long?) = withUser { actor ->
        orderRepository.recordResult(actor, orderId, summary, estimateAmountCents)
    }

    fun reassign(orderId: String, userId: String?) = withUser { actor ->
        orderRepository.reassign(actor, orderId, userId)
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
    ) = withUser(onSuccess = onSuccess) { actor ->
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

    fun prepareMessage(orderId: String, templateCode: String) =
        messageActions.prepareMessage(orderId, templateCode)

    fun updatePreparedMessageBody(body: String) =
        messageActions.updatePreparedMessageBody(body)

    fun recordPreparedMessage() = messageActions.recordPreparedMessage()

    fun confirmSentMessage() = messageActions.confirmSentMessage()

    fun exportConfiguration(uri: Uri) =
        adminActions.exportConfiguration(uri)

    fun exportDatabase(uri: Uri) =
        adminActions.exportDatabase(uri)

    fun exportCompleteBackup(uri: Uri) =
        adminActions.exportCompleteBackup(uri)

    fun importCompleteBackup(uri: Uri) =
        adminActions.importCompleteBackup(uri)

    fun exportSelectedOrderPdf(uri: Uri) =
        mediaActions.exportSelectedOrderPdf(backupRepository, uri)

    fun resetAllData(confirmation: String, onReset: () -> Unit) =
        adminActions.resetAllData(confirmation, onReset)

    fun archiveDiscardedClosedOrders() =
        adminActions.archiveDiscardedClosedOrders()

    fun deleteArchivedOrders() =
        adminActions.deleteArchivedOrders()

    private fun withUser(
        onSuccess: () -> Unit = {},
        block: suspend (com.onlytechtake.repairapp.domain.auth.AuthenticatedUser) -> Result<*>,
    ) {
        viewModelScope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            block(actor).fold(
                onSuccess = {
                    showSuccess("Operazione completata")
                    onSuccess()
                },
                onFailure = { showError(it) },
            )
        }
    }

    private fun showSuccess(text: String) = showMessage(text, false)

    private fun showError(error: Throwable) = showMessage(error.message ?: "Operazione non riuscita", true)

    private fun showMessage(text: String, isError: Boolean) {
        mutableUiState.update { it.copy(message = WorkspaceMessage(text, isError)) }
    }

}
