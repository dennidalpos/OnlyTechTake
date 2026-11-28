package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.di.IoDispatcher
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.logic.OrderStatusAction
import com.onlytechtake.repairapp.domain.logic.RepairWorkflowEngine
import com.onlytechtake.repairapp.domain.models.*
import com.onlytechtake.repairapp.domain.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.domain.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.usecases.GenerateOrderStatusPdfUseCase
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.features.workspace.WorkspaceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        savedStateHandle: SavedStateHandle,
        private val authRepository: AuthRepository,
        private val orderRepository: OrderRepository,
        private val orderMediaRepository: OrderMediaRepository,
        private val orderDocumentRepository: OrderDocumentRepository,
        private val templateRepository: TemplateRepository,
        private val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase,
        private val shareDocumentIntentFactory: ShareDocumentIntentFactory,
        private val workspaceManager: WorkspaceManager,
        private val actionRunner: AuthenticatedActionRunner,
        private val workflowEngine: RepairWorkflowEngine,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _orderId = MutableStateFlow<String?>(savedStateHandle["orderId"])
        val orderId: StateFlow<String?> = _orderId.asStateFlow()

        val message = workspaceManager.message

        private val _isProcessing = MutableStateFlow(value = false)
        val isProcessing = _isProcessing.asStateFlow()

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

        init {
            observeAutoSaveSignal()
        }

        private fun observeAutoSaveSignal() {
            viewModelScope.launch {
                workspaceManager.autoSaveSignal.collect { signal ->
                    if (signal > 0 && preparedMessage.value != null) {
                        recordPreparedMessage()
                    }
                }
            }
        }

        private val notesExpandedFlow = MutableStateFlow(value = false)
        private val photosExpandedFlow = MutableStateFlow(value = false)
        private val timelineExpandedFlow = MutableStateFlow(value = false)
        private val signaturesExpandedFlow = MutableStateFlow(value = false)

        @OptIn(ExperimentalCoroutinesApi::class)
        val viewState: StateFlow<OrderDetailViewState> =
            combine(
                _orderId.filterNotNull().flatMapLatest { id -> orderRepository.observeOrderDetail(id) },
                _orderId.filterNotNull().flatMapLatest { id -> orderDocumentRepository.observeByOrder(id) },
                _isProcessing,
                _isGeneratingStatusPdf,
                _statusPdfError,
                _showPdfSentConfirmation,
                _preparedMessage,
                _preparedMessageBody,
                notesExpandedFlow,
                photosExpandedFlow,
                timelineExpandedFlow,
                signaturesExpandedFlow,
            ) { args ->
                OrderDetailViewState(
                    detail = args[0] as? OrderDetail,
                    documents = (args[1] as? List<*>)?.filterIsInstance<OrderDocument>() ?: emptyList(),
                    isProcessing = args[2] as? Boolean ?: false,
                    isGeneratingStatusPdf = args[3] as? Boolean ?: false,
                    statusPdfError = args[4] as? String,
                    showPdfSentConfirmation = args[5] as? Boolean ?: false,
                    preparedMessage = args[6] as? PreparedMessage,
                    preparedMessageBody = args[7] as? String ?: "",
                    notesExpanded = args[8] as? Boolean ?: false,
                    photosExpanded = args[9] as? Boolean ?: false,
                    timelineExpanded = args[10] as? Boolean ?: false,
                    signaturesExpanded = args[11] as? Boolean ?: false,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderDetailViewState())

        fun toggleNotesExpanded() {
            notesExpandedFlow.value = !notesExpandedFlow.value
        }

        fun togglePhotosExpanded() {
            photosExpandedFlow.value = !photosExpandedFlow.value
        }

        fun toggleTimelineExpanded() {
            timelineExpandedFlow.value = !timelineExpandedFlow.value
        }

        fun toggleSignaturesExpanded() {
            signaturesExpandedFlow.value = !signaturesExpandedFlow.value
        }

        fun setOrderId(id: String?) {
            _orderId.value = id
        }

        fun setFormDirty(dirty: Boolean) {
            workspaceManager.setFormDirty(dirty)
        }

        fun clearMessage() {
            workspaceManager.clearMessage()
        }

        fun changeStatus(
            status: OrderStatus,
            note: String = "",
            closureReason: ClosureReason? = null,
            onSuccess: () -> Unit = {},
        ) {
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
                onSuccess = onSuccess,
            ) { actor ->
                orderRepository.changeStatus(actor, id, status, note, closureReason)
            }
        }

        fun recordResult(
            summary: String,
            outcome: DiagnosisOutcome,
            estimateMillis: Long?,
            onSuccess: () -> Unit = {},
        ) {
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
                onSuccess = onSuccess,
            ) { actor ->
                orderRepository.recordResult(
                    actor,
                    id,
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
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
                onSuccess = onSuccess,
            ) { actor ->
                orderRepository.recordFeedback(actor, id, FeedbackInput(type, note, closureReason))
            }
        }

        fun togglePaymentStatus(isPaid: Boolean) {
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) { actor ->
                orderRepository.togglePaymentStatus(actor, id, isPaid)
            }
        }

        fun addNote(note: String) {
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) { actor ->
                orderRepository.addInternalNote(actor, id, note)
            }
        }

        fun createPhotoFile(
            orderNumber: String,
        ): Result<File> {
            val id = _orderId.value ?: return Result.failure(IllegalStateException("No order ID"))
            return runCatching { orderMediaRepository.createPhotoFile(id, orderNumber) }
        }

        fun createVideoFile(
            orderNumber: String,
        ): Result<File> {
            val id = _orderId.value ?: return Result.failure(IllegalStateException("No order ID"))
            return runCatching { orderMediaRepository.createVideoFile(id, orderNumber) }
        }

        fun registerCapturedPhoto(
            file: File,
        ) {
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) { actor ->
                orderMediaRepository.registerCapturedPhoto(actor, id, file)
            }
        }

        fun registerCapturedVideo(
            file: File,
        ) {
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) { actor ->
                orderMediaRepository.registerCapturedVideo(actor, id, file)
            }
        }

        fun importAttachment(
            uri: Uri,
        ) {
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) { actor ->
                orderMediaRepository.importAttachment(actor, id, uri)
            }
        }

        fun deleteMedia(mediaId: String) {
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) { actor ->
                orderMediaRepository.deleteMedia(actor, mediaId)
            }
        }

        fun deleteSignature(signatureId: String) {
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) { actor ->
                orderRepository.deleteSignature(actor, signatureId)
            }
        }

        fun deleteOrder(onDeleted: () -> Unit) {
            val id = _orderId.value ?: return
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
                onSuccess = {
                    setOrderId(null)
                    onDeleted()
                },
            ) { actor ->
                orderRepository.deleteOrder(actor, id)
            }
        }

        fun replaceImage(
            mediaId: String,
            bitmap: Bitmap,
        ) {
            actionRunner.run(
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) { actor ->
                orderMediaRepository.replaceImage(actor, mediaId, bitmap)
            }
        }

        fun reportCameraError(message: String) {
            workspaceManager.showMessage(message, isError = true)
        }

        fun prepareMessage(anchor: TemplateAnchor) {
            val id = _orderId.value ?: return
            viewModelScope.launch {
                _isProcessing.value = true
                val actor = currentUser.value
                actor?.let { user ->
                    templateRepository
                        .prepareMessage(user, id, anchor)
                        .onSuccess { message ->
                            _preparedMessage.value = message
                            _preparedMessageBody.value = message.body
                        }.onFailure {
                            workspaceManager.showMessage(
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
            actionRunner.run(scope = viewModelScope) { actor ->
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
            actionRunner.run(scope = viewModelScope) { actor ->
                templateRepository.confirmSent(actor, prepared, preparedMessageBody.value)
            }
        }

        fun generateStatusPdf(includeSignature: Boolean) {
            val id = _orderId.value ?: return
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

                generateOrderStatusPdfUseCase(id, actor.id, includeSignature)
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
            val id = _orderId.value ?: return
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
                    workspaceManager.showMessage(
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
                scope = viewModelScope,
                successMessageRes = R.string.success_operation_completed,
                autoDismissMillis = 1000L,
                setLoading = { _isProcessing.value = it },
            ) {
                orderDocumentRepository.delete(documentId)
            }
        }

        fun onPdfSentConfirmed() {
            val id = _orderId.value ?: return
            _showPdfSentConfirmation.value = false
            val docId = _lastGeneratedDocumentId.value ?: return
            viewModelScope.launch {
                val doc =
                    withContext(ioDispatcher) {
                        orderDocumentRepository.findById(docId)
                    }
                if (doc != null) {
                    actionRunner.run(scope = viewModelScope) { actor ->
                        orderDocumentRepository.markShared(docId, System.currentTimeMillis())
                        orderRepository.recordPdfShared(actor, id, doc.fileName)
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

        fun onActionClick(
            action: OrderStatusAction,
            diagnosisOutcome: DiagnosisOutcome?,
            decision: com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision?,
            quoteRejectNote: String,
            requestConfirm: (String, String, () -> Unit) -> Unit,
            openStatusAction: (OrderStatusAction, DiagnosisOutcome?) -> Unit,
            onSuccess: () -> Unit = {},
        ) {
            val detail = viewState.value.detail ?: return
            val isResultStep = detail.order.status == OrderStatus.RESULT
            val isRepairable = diagnosisOutcome == DiagnosisOutcome.REPAIRABLE
            val isNotRepairable = diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE

            val isActionMatchDecision =
                (isResultStep && isRepairable && (decision != null)) &&
                    (action == OrderStatusAction.START_REPAIR || action == OrderStatusAction.CLOSE_ORDER)

            val isActionMatchNotRepairable =
                isResultStep && isNotRepairable && (action == OrderStatusAction.READY_FOR_PICKUP)

            when {
                isActionMatchDecision -> {
                    if (decision == com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision.ACCEPTED) {
                        recordFeedback(
                            CustomerFeedbackType.ACCEPT,
                            context.getString(R.string.order_quote_accepted_log),
                        ) {
                            prepareMessage(TemplateAnchor.WHATSAPP_REPAIRING)
                            onSuccess()
                        }
                    } else {
                        recordFeedback(
                            CustomerFeedbackType.REJECT,
                            quoteRejectNote,
                            ClosureReason.CUSTOMER_DECLINED,
                        ) {
                            prepareMessage(TemplateAnchor.WHATSAPP_NOT_REPAIRABLE)
                            onSuccess()
                        }
                    }
                }

                isActionMatchNotRepairable -> {
                    requestConfirm(
                        context.getString(R.string.dialog_confirm_title),
                        context.getString(R.string.order_confirm_ready_not_repairable),
                    ) {
                        changeStatus(
                            status = OrderStatus.READY_FOR_PICKUP,
                            note = context.getString(R.string.order_not_repairable_title),
                        ) {
                            prepareMessage(TemplateAnchor.WHATSAPP_NOT_REPAIRABLE)
                            onSuccess()
                        }
                    }
                }

                else -> {
                    openStatusAction(action, diagnosisOutcome)
                }
            }
        }

        fun getPrimaryActionOverride(quoteAccepted: Boolean?): OrderStatusAction? {
            val detail = viewState.value.detail ?: return null
            return workflowEngine.getPrimaryActionOverride(
                detail.order.status,
                detail.order.diagnosisOutcome,
                quoteAccepted,
            )
        }

        fun getWhatsAppAnchor(): TemplateAnchor {
            val detail = viewState.value.detail ?: return TemplateAnchor.WHATSAPP_UPDATE
            return workflowEngine.getWhatsAppTemplateForStatus(detail.order)
        }
    }
