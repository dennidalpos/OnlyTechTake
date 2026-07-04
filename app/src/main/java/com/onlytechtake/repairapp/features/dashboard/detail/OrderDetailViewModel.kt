package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.workspace.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class OrderDetailViewModel(
    private val context: Context,
    private val orderId: String,
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val orderMediaRepository: OrderMediaRepository,
    private val orderDocumentRepository: OrderDocumentRepository,
    private val templateRepository: TemplateRepository,
    private val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase,
    private val shareDocumentIntentFactory: ShareDocumentIntentFactory,
    private val diagnosticLogger: DiagnosticLogger,
    private val onSetFormDirty: (Boolean) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _isProcessing = MutableStateFlow(value = false)
    val isProcessing = _isProcessing.asStateFlow()

    private val actionRunner =
        AuthenticatedActionRunner(
            context = context,
            authRepository = authRepository,
            currentUser = authRepository.currentUser,
            scope = viewModelScope,
            showMessage = { text, isError, duration ->
                _message.value = WorkspaceMessage(text, isError, duration)
            },
            setLoading = { _isProcessing.value = it },
            diagnosticLogger = diagnosticLogger,
            ioDispatcher = ioDispatcher,
        )

    private val _isGeneratingStatusPdf = MutableStateFlow(value = false)
    val isGeneratingStatusPdf = _isGeneratingStatusPdf.asStateFlow()

    private val _statusPdfError = MutableStateFlow<String?>(null)
    val statusPdfError = _statusPdfError.asStateFlow()

    private val _showPdfSentConfirmation = MutableStateFlow(value = false)
    val showPdfSentConfirmation = _showPdfSentConfirmation.asStateFlow()

    private val _lastGeneratedDocumentId = MutableStateFlow<String?>(null)
    val lastGeneratedDocumentId = _lastGeneratedDocumentId.asStateFlow()

    private val _pendingShareIntent = MutableStateFlow<Intent?>(null)
    val pendingShareIntent = _pendingShareIntent.asStateFlow()

    private val _preparedMessage = MutableStateFlow<PreparedMessage?>(null)
    val preparedMessage = _preparedMessage.asStateFlow()

    private val _preparedMessageBody = MutableStateFlow("")
    val preparedMessageBody = _preparedMessageBody.asStateFlow()

    val currentUser = authRepository.currentUser

    val orderDetail: StateFlow<OrderDetail?> =
        orderRepository
            .observeOrderDetail(orderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val documents: StateFlow<List<OrderDocumentEntity>> =
        orderDocumentRepository
            .observeByOrder(orderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFormDirty(dirty: Boolean) {
        onSetFormDirty(dirty)
    }

    fun clearMessage() {
        _message.value = null
    }

    fun changeStatus(
        status: OrderStatus,
        note: String = "",
        closureReason: ClosureReason? = null,
        onSuccess: () -> Unit = {},
    ) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
            onSuccess = onSuccess,
        ) { actor ->
            orderRepository.changeStatus(actor, orderId, status, note, closureReason)
        }
    }

    fun recordResult(
        summary: String,
        outcome: DiagnosisOutcome,
        estimateMillis: Long?,
        onSuccess: () -> Unit = {},
    ) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
            onSuccess = onSuccess,
        ) { actor ->
            orderRepository.recordResult(
                actor,
                orderId,
                DiagnosisResultInput(summary, outcome, estimateMillis),
            )
        }
    }

    fun recordFeedback(
        type: CustomerFeedbackType,
        note: String,
        closureReason: ClosureReason? = null,
        onSuccess: () -> Unit = {},
    ) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
            onSuccess = onSuccess,
        ) { actor ->
            orderRepository.recordFeedback(actor, orderId, FeedbackInput(type, note, closureReason))
        }
    }

    fun togglePaymentStatus(isPaid: Boolean) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            orderRepository.togglePaymentStatus(actor, orderId, isPaid)
        }
    }

    fun addNote(note: String) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            orderRepository.addInternalNote(actor, orderId, note)
        }
    }

    fun createPhotoFile(
        orderNumber: String,
    ): Result<File> = runCatching { orderMediaRepository.createPhotoFile(orderId, orderNumber) }

    fun createVideoFile(
        orderNumber: String,
    ): Result<File> = runCatching { orderMediaRepository.createVideoFile(orderId, orderNumber) }

    fun registerCapturedPhoto(
        file: File,
    ) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            orderMediaRepository.registerCapturedPhoto(actor, orderId, file)
        }
    }

    fun registerCapturedVideo(
        file: File,
    ) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            orderMediaRepository.registerCapturedVideo(actor, orderId, file)
        }
    }

    fun importAttachment(
        uri: Uri,
    ) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            orderMediaRepository.importAttachment(actor, orderId, uri)
        }
    }

    fun deleteMedia(mediaId: String) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            orderMediaRepository.deleteMedia(actor, mediaId)
        }
    }

    fun deleteSignature(signatureId: String) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            orderRepository.deleteSignature(actor, signatureId)
        }
    }

    fun deleteOrder(onDeleted: () -> Unit) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
            onSuccess = onDeleted,
        ) { actor ->
            orderRepository.deleteOrder(actor, orderId)
        }
    }

    fun replaceImage(
        mediaId: String,
        bitmap: Bitmap,
    ) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            orderMediaRepository.replaceImage(actor, mediaId, bitmap)
        }
    }

    fun reportCameraError(message: String) {
        _message.value = WorkspaceMessage(message, isError = true)
    }

    fun prepareMessage(anchor: TemplateAnchor) {
        viewModelScope.launch {
            _isProcessing.value = true
            val actor = currentUser.value
            actor?.let { user ->
                templateRepository
                    .prepareMessage(user, orderId, anchor)
                    .onSuccess { message ->
                        _preparedMessage.value = message
                        _preparedMessageBody.value = message.body
                    }.onFailure {
                        _message.value =
                            WorkspaceMessage(
                                it.message ?: context.getString(R.string.error_unknown),
                                isError = true,
                            )
                    }
            }
            _isProcessing.value = false
        }
    }

    fun updatePreparedMessageBody(body: String) {
        _preparedMessageBody.value = body
        setFormDirty(dirty = true)
    }

    fun clearPreparedMessage() {
        _preparedMessage.value = null
        _preparedMessageBody.value = ""
        setFormDirty(dirty = false)
    }

    fun recordPreparedMessage() {
        val prepared = _preparedMessage.value ?: return
        actionRunner.run { actor ->
            val result =
                templateRepository.recordPrepared(actor, prepared, preparedMessageBody.value)
            withContext(Dispatchers.Main) {
                setFormDirty(dirty = false)
            }
            result
        }
    }

    fun confirmSentMessage() {
        val prepared = _preparedMessage.value ?: return
        actionRunner.run { actor ->
            templateRepository.confirmSent(actor, prepared, preparedMessageBody.value)
        }
    }

    fun generateStatusPdf(includeSignature: Boolean) {
        viewModelScope.launch {
            _isGeneratingStatusPdf.value = true
            _statusPdfError.value = null
            _lastGeneratedDocumentId.value = null

            val actor = currentUser.value
            if (actor == null) {
                _statusPdfError.value = context.getString(R.string.error_session_expired)
                _isGeneratingStatusPdf.value = false
                return@launch
            }

            generateOrderStatusPdfUseCase(orderId, actor.id, includeSignature)
                .onSuccess { doc ->
                    _lastGeneratedDocumentId.value = doc.id
                    _showPdfSentConfirmation.value = true
                }.onFailure {
                    _statusPdfError.value =
                        it.message ?: context.getString(R.string.error_init_failed)
                }
            _isGeneratingStatusPdf.value = false
        }
    }

    fun onShareDocumentClicked(documentId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val doc =
                withContext(ioDispatcher) {
                    orderDocumentRepository.findById(documentId)
                }
            if (doc != null) {
                val intent =
                    shareDocumentIntentFactory.createPdfShareIntent(
                        File(doc.localPath),
                        context.getString(R.string.share_title_document, doc.id),
                    )
                _lastGeneratedDocumentId.value = doc.id
                _showPdfSentConfirmation.value = true
                _pendingShareIntent.value = intent
            } else {
                _message.value =
                    WorkspaceMessage(
                        context.getString(R.string.error_document_not_found),
                        isError = true,
                    )
            }
            _isProcessing.value = false
        }
    }

    fun onWhatsAppDocumentClicked() {
        prepareMessage(TemplateAnchor.WHATSAPP_PDF_STATUS)
    }

    fun onDeleteDocumentClicked(documentId: String) {
        actionRunner.run(
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) {
            orderDocumentRepository.delete(documentId)
        }
    }

    fun onPdfSentConfirmed() {
        _showPdfSentConfirmation.value = false
        val docId = _lastGeneratedDocumentId.value ?: return
        viewModelScope.launch {
            val doc =
                withContext(ioDispatcher) {
                    orderDocumentRepository.findById(docId)
                }
            if (doc != null) {
                actionRunner.run { actor ->
                    orderDocumentRepository.markShared(docId, System.currentTimeMillis())
                    orderRepository.recordPdfShared(actor, orderId, doc.fileName)
                }
            }
            _lastGeneratedDocumentId.value = null
        }
    }

    fun onPdfSentDismissed() {
        _showPdfSentConfirmation.value = false
    }

    fun clearPendingShareIntent() {
        _pendingShareIntent.value = null
    }

    class Factory(
        private val context: Context,
        private val orderId: String,
        private val authRepository: AuthRepository,
        private val orderRepository: OrderRepository,
        private val orderMediaRepository: OrderMediaRepository,
        private val orderDocumentRepository: OrderDocumentRepository,
        private val templateRepository: TemplateRepository,
        private val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase,
        private val shareDocumentIntentFactory: ShareDocumentIntentFactory,
        private val diagnosticLogger: DiagnosticLogger,
        private val onSetFormDirty: (Boolean) -> Unit,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OrderDetailViewModel(
                context,
                orderId,
                authRepository,
                orderRepository,
                orderMediaRepository,
                orderDocumentRepository,
                templateRepository,
                generateOrderStatusPdfUseCase,
                shareDocumentIntentFactory,
                diagnosticLogger,
                onSetFormDirty,
                ioDispatcher,
            ) as T
    }
}
