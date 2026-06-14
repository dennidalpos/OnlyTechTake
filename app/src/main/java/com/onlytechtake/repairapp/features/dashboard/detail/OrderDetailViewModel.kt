package com.onlytechtake.repairapp.features.dashboard.detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.graphics.Bitmap

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModel(
    private val orderId: String,
    private val authRepository: AuthRepository,
    private val orderRepository: OrderWorkflowRepository,
    private val orderMediaRepository: OrderMediaRepository,
    private val orderDocumentRepository: OrderDocumentRepository,
    private val templateRepository: TemplateRepository,
    private val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase,
    private val shareDocumentIntentFactory: ShareDocumentIntentFactory,
    private val onSetFormDirty: (Boolean) -> Unit,
) : ViewModel() {

    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _isGeneratingStatusPdf = MutableStateFlow(false)
    val isGeneratingStatusPdf = _isGeneratingStatusPdf.asStateFlow()

    private val _statusPdfError = MutableStateFlow<String?>(null)
    val statusPdfError = _statusPdfError.asStateFlow()

    private val _showPdfSentConfirmation = MutableStateFlow(false)
    val showPdfSentConfirmation = _showPdfSentConfirmation.asStateFlow()

    private val _lastGeneratedDocumentId = MutableStateFlow<String?>(null)
    val lastGeneratedDocumentId = _lastGeneratedDocumentId.asStateFlow()

    private val _pendingShareIntent = MutableStateFlow<android.content.Intent?>(null)
    val pendingShareIntent = _pendingShareIntent.asStateFlow()

    private val _preparedMessage = MutableStateFlow<PreparedMessage?>(null)
    val preparedMessage = _preparedMessage.asStateFlow()

    private val _preparedMessageBody = MutableStateFlow("")
    val preparedMessageBody = _preparedMessageBody.asStateFlow()

    val currentUser = authRepository.currentUser

    val orderDetail = orderRepository.observeOrderDetail(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val documents = orderDocumentRepository.observeByOrder(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFormDirty(dirty: Boolean) {
        onSetFormDirty(dirty)
    }

    fun changeStatus(status: OrderStatus, note: String = "", closureReason: ClosureReason? = null, onSuccess: () -> Unit = {}) {
        launchWithUser(onSuccess = {
            clearPreparedMessage()
            onSuccess()
        }) { actor ->
            orderRepository.changeStatus(actor, orderId, status, note, closureReason)
        }
    }

    fun recordResult(summary: String, diagnosisOutcome: DiagnosisOutcome, estimateAmountCents: Long?, onSuccess: () -> Unit = {}) {
        launchWithUser(onSuccess = {
            clearPreparedMessage()
            onSuccess()
        }) { actor ->
            orderRepository.recordResult(actor, orderId, DiagnosisResultInput(summary, diagnosisOutcome, estimateAmountCents))
        }
    }

    fun recordFeedback(
        feedback: CustomerFeedbackType,
        note: String,
        closureReason: ClosureReason? = null,
        onSuccess: () -> Unit = {},
    ) {
        launchWithUser(onSuccess = {
            clearPreparedMessage()
            onSuccess()
        }) { actor ->
            orderRepository.recordFeedback(actor, orderId, FeedbackInput(feedback, note, closureReason))
        }
    }

    fun togglePaymentStatus(isPaid: Boolean) {
        launchWithUser { actor ->
            com.onlytechtake.repairapp.core.permissions.PermissionService.requirePermission(actor, com.onlytechtake.repairapp.data.local.entities.UserPermission.MANAGE_PAYMENTS)
            orderRepository.togglePaymentStatus(actor, orderId, isPaid)
        }
    }

    fun addNote(note: String) {
        launchWithUser { actor ->
            orderRepository.addInternalNote(actor, orderId, note)
        }
    }

    fun createPhotoFile(id: String, orderNumber: String): Result<File> =
        runCatching { orderMediaRepository.createPhotoFile(id, orderNumber) }

    fun registerCapturedPhoto(id: String, file: File) {
        launchWithUser { actor ->
            orderMediaRepository.registerCapturedPhoto(actor, id, file)
        }
    }

    fun registerCapturedFinalPhoto(id: String, file: File) {
        launchWithUser { actor ->
            orderMediaRepository.registerCapturedFinalPhoto(actor, id, file)
        }
    }

    fun importAttachment(id: String, uri: Uri) {
        launchWithUser { actor ->
            orderMediaRepository.importAttachment(actor, id, uri)
        }
    }

    fun deleteMedia(mediaId: String) {
        launchWithUser { actor ->
            orderMediaRepository.deleteMedia(actor, mediaId)
        }
    }

    fun replaceImage(mediaId: String, bitmap: Bitmap) {
        launchWithUser { actor ->
            orderMediaRepository.replaceImage(actor, mediaId, bitmap)
        }
    }

    fun reportCameraError(message: String) {
        _message.value = WorkspaceMessage(message, true)
    }

    fun prepareMessage(templateCode: String) {
        launchWithUser { actor ->
            templateRepository.prepareMessage(actor, orderId, templateCode).map { prepared ->
                _preparedMessage.value = prepared
                _preparedMessageBody.value = prepared.body
                setFormDirty(true)
            }
        }
    }

    fun updatePreparedMessageBody(body: String) {
        _preparedMessageBody.value = body
        setFormDirty(true)
    }

    fun clearPreparedMessage() {
        _preparedMessage.value = null
        _preparedMessageBody.value = ""
        setFormDirty(false)
    }

    fun recordPreparedMessage() {
        val prepared = _preparedMessage.value ?: return
        launchWithUser { actor ->
            templateRepository.recordPrepared(actor, prepared, _preparedMessageBody.value)
        }
    }

    fun confirmSentMessage() {
        val prepared = _preparedMessage.value ?: return
        launchWithUser(onSuccess = { clearPreparedMessage() }) { actor ->
            templateRepository.confirmSent(actor, prepared, _preparedMessageBody.value)
        }
    }

    fun generateStatusPdf(includeSignature: Boolean = true) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            _isGeneratingStatusPdf.value = true
            _statusPdfError.value = null
            generateOrderStatusPdfUseCase(orderId, actor.id, includeSignature)
                .onSuccess { document ->
                    _isGeneratingStatusPdf.value = false
                    _lastGeneratedDocumentId.value = document.id
                    triggerShare(document.localPath, "Stato avanzamento ${document.fileName}")
                    _showPdfSentConfirmation.value = true
                }
                .onFailure { error ->
                    _isGeneratingStatusPdf.value = false
                    _statusPdfError.value = error.message
                }
        }
    }

    fun onShareDocumentClicked(documentId: String) {
        viewModelScope.launch {
            val document = orderDocumentRepository.findById(documentId)
            if (document != null) {
                _lastGeneratedDocumentId.value = documentId
                triggerShare(document.localPath, "Documento ${document.fileName}")
                _showPdfSentConfirmation.value = true
            } else {
                _message.value = WorkspaceMessage("Documento non trovato", true)
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
                        orderRepository.recordMediaRemoved(actor, orderId, document.fileName)
                    },
                    onFailure = { error -> _message.value = WorkspaceMessage(error.message ?: "Errore", true) }
                )
            }
        }
    }

    fun onPdfSentConfirmed() {
        val actor = currentUser.value ?: return
        val documentId = _lastGeneratedDocumentId.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            orderDocumentRepository.markShared(documentId, now)
            val doc = orderDocumentRepository.findById(documentId)
            doc?.let {
                orderRepository.recordPdfShared(actor, orderId, it.fileName)
            }
            _showPdfSentConfirmation.value = false
            _lastGeneratedDocumentId.value = null
        }
    }

    fun onPdfSentDismissed() {
        _showPdfSentConfirmation.value = false
    }

    private fun triggerShare(localPath: String, title: String) {
        val file = File(localPath)
        if (file.exists()) {
            _pendingShareIntent.value = shareDocumentIntentFactory.createPdfShareIntent(file, title)
        }
    }

    fun clearPendingShareIntent() {
        _pendingShareIntent.value = null
    }

    private fun launchWithUser(
        onSuccess: () -> Unit = {},
        block: suspend (com.onlytechtake.repairapp.domain.auth.AuthenticatedUser) -> Result<*>
    ) {
        val user = currentUser.value
        if (user != null) {
            viewModelScope.launch {
                block(user).fold(
                    onSuccess = { onSuccess() },
                    onFailure = { error -> _message.value = WorkspaceMessage(error.message ?: "Errore", true) }
                )
            }
        }
    }

    class Factory(
        private val orderId: String,
        private val authRepository: AuthRepository,
        private val orderRepository: OrderWorkflowRepository,
        private val orderMediaRepository: OrderMediaRepository,
        private val orderDocumentRepository: OrderDocumentRepository,
        private val templateRepository: TemplateRepository,
        private val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase,
        private val shareDocumentIntentFactory: ShareDocumentIntentFactory,
        private val onSetFormDirty: (Boolean) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrderDetailViewModel(
                orderId, authRepository, orderRepository, orderMediaRepository,
                orderDocumentRepository, templateRepository, generateOrderStatusPdfUseCase,
                shareDocumentIntentFactory, onSetFormDirty
            ) as T
        }
    }
}
