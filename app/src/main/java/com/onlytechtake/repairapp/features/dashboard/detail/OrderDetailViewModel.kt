package com.onlytechtake.repairapp.features.dashboard.detail

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.error.ErrorHandler
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.local.entities.*
import com.onlytechtake.repairapp.data.repositories.*
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModel(
    private val orderId: String,
    private val orderRepository: OrderWorkflowRepository,
    private val orderMediaRepository: OrderMediaRepository,
    private val orderDocumentRepository: OrderDocumentRepository,
    private val templateRepository: TemplateRepository,
    private val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase,
    private val shareDocumentIntentFactory: ShareDocumentIntentFactory,
    private val authRepository: AuthRepository,
    private val errorHandler: ErrorHandler,
) : ViewModel() {

    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _isGeneratingStatusPdf = MutableStateFlow(false)
    val isGeneratingStatusPdf = _isGeneratingStatusPdf.asStateFlow()

    private val _statusPdfError = MutableStateFlow<String?>(null)
    val statusPdfError = _statusPdfError.asStateFlow()

    private val _pendingShareIntent = MutableStateFlow<android.content.Intent?>(null)
    val pendingShareIntent = _pendingShareIntent.asStateFlow()

    private val _showPdfSentConfirmation = MutableStateFlow(false)
    val showPdfSentConfirmation = _showPdfSentConfirmation.asStateFlow()

    private var lastGeneratedDocumentId: String? = null

    val selectedOrder = orderRepository.observeOrderDetail(orderId)
        .catch { errorHandler.handleError(it, "OrderDetailViewModel") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedOrderDocuments = orderDocumentRepository.observeByOrder(orderId)
        .catch { errorHandler.handleError(it, "OrderDetailViewModel") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentUser = authRepository.currentUser

    private val actionRunner = AuthenticatedActionRunner(
        authRepository = authRepository,
        currentUser = authRepository.currentUser,
        scope = viewModelScope,
        showSuccess = { showMessage(it, false) },
        showError = { errorHandler.handleError(it) },
        showMessage = { text, isError -> showMessage(text, isError) }
    )

    private fun showMessage(text: String, isError: Boolean) {
        _message.value = WorkspaceMessage(text, isError)
    }

    fun clearMessage() {
        _message.value = null
    }

    fun addNote(note: String) {
        actionRunner.run { actor ->
            orderRepository.addInternalNote(actor, orderId, note)
        }
    }

    fun changeStatus(status: OrderStatus, note: String = "", closureReason: ClosureReason? = null) {
        actionRunner.run { actor ->
            orderRepository.changeStatus(actor, orderId, status, note, closureReason)
        }
    }

    fun recordResult(summary: String, outcome: DiagnosisOutcome, estimateAmountCents: Long?) {
        actionRunner.run { actor ->
            orderRepository.recordResult(actor, orderId, DiagnosisResultInput(summary, outcome, estimateAmountCents))
        }
    }

    fun recordFeedback(feedback: CustomerFeedbackType, note: String, closureReason: ClosureReason? = null, onSuccess: () -> Unit = {}) {
        actionRunner.run(onSuccess = onSuccess) { actor ->
            orderRepository.recordFeedback(actor, orderId, FeedbackInput(feedback, note, closureReason))
        }
    }

    fun createPhotoFile(orderNumber: String): Result<File> =
        runCatching { orderMediaRepository.createPhotoFile(orderId, orderNumber) }

    fun registerCapturedPhoto(file: File) {
        actionRunner.run { actor ->
            orderMediaRepository.registerCapturedPhoto(actor, orderId, file)
        }
    }

    fun registerCapturedFinalPhoto(file: File) {
        actionRunner.run { actor ->
            orderMediaRepository.registerCapturedFinalPhoto(actor, orderId, file)
        }
    }

    fun importAttachment(uri: Uri) {
        actionRunner.run { actor ->
            orderMediaRepository.importAttachment(actor, orderId, uri)
        }
    }

    fun deleteMedia(mediaId: String) {
        actionRunner.run { actor ->
            orderMediaRepository.deleteMedia(actor, mediaId)
        }
    }

    fun replaceImage(mediaId: String, bitmap: Bitmap) {
        actionRunner.run { actor ->
            orderMediaRepository.replaceImage(actor, mediaId, bitmap)
        }
    }

    fun generateStatusPdf(includeSignature: Boolean) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            _isGeneratingStatusPdf.value = true
            _statusPdfError.value = null
            generateOrderStatusPdfUseCase(orderId, actor.id, includeSignature)
                .onSuccess { document ->
                    _isGeneratingStatusPdf.value = false
                    lastGeneratedDocumentId = document.id
                    triggerShare(document.localPath, "Stato avanzamento ${document.fileName}")
                    _showPdfSentConfirmation.value = true
                }
                .onFailure { error ->
                    _isGeneratingStatusPdf.value = false
                    _statusPdfError.value = error.message
                    errorHandler.handleError(error)
                }
        }
    }

    fun shareDocument(documentId: String) {
        viewModelScope.launch {
            val document = orderDocumentRepository.findById(documentId)
            if (document != null) {
                lastGeneratedDocumentId = documentId
                triggerShare(document.localPath, "Documento ${document.fileName}")
                _showPdfSentConfirmation.value = true
            } else {
                showMessage("Documento non trovato", true)
            }
        }
    }

    private fun triggerShare(localPath: String, title: String) {
        val file = File(localPath)
        if (file.exists()) {
            _pendingShareIntent.value = shareDocumentIntentFactory.createPdfShareIntent(file, title)
        } else {
            showMessage("File non trovato", true)
        }
    }

    fun confirmPdfSent() {
        val actor = currentUser.value ?: return
        val docId = lastGeneratedDocumentId ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            orderDocumentRepository.markShared(docId, now)
            val doc = orderDocumentRepository.findById(docId)
            doc?.let {
                orderRepository.recordPdfShared(actor, orderId, it.fileName)
            }
            _showPdfSentConfirmation.value = false
            lastGeneratedDocumentId = null
            showMessage("Invio confermato", false)
        }
    }

    fun dismissPdfSent() {
        _showPdfSentConfirmation.value = false
    }

    fun clearPendingShareIntent() {
        _pendingShareIntent.value = null
    }

    fun prepareMessage(templateCode: String) {
        actionRunner.runCustom { actor ->
            templateRepository.prepareMessage(actor, orderId, templateCode)
                .onSuccess { /* Handle prepared message - this might need more work to connect with UI state if we want to show preview here */ }
                .onFailure { errorHandler.handleError(it) }
        }
    }

    class Factory(
        private val orderId: String,
        private val orderRepository: OrderWorkflowRepository,
        private val orderMediaRepository: OrderMediaRepository,
        private val orderDocumentRepository: OrderDocumentRepository,
        private val templateRepository: TemplateRepository,
        private val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase,
        private val shareDocumentIntentFactory: ShareDocumentIntentFactory,
        private val authRepository: AuthRepository,
        private val errorHandler: ErrorHandler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrderDetailViewModel(
                orderId,
                orderRepository,
                orderMediaRepository,
                orderDocumentRepository,
                templateRepository,
                generateOrderStatusPdfUseCase,
                shareDocumentIntentFactory,
                authRepository,
                errorHandler
            ) as T
        }
    }
}
