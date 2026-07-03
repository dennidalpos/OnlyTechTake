package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.LoadingBrandIndicator
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.RepairAppTheme
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.di.FakeAppContainer
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.statusTemplateAnchor
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision
import com.onlytechtake.repairapp.features.workspace.SharedWorkspaceViewModel

@Composable
fun OrderDetailWorkspace(
    viewModel: OrderDetailViewModel,
    sharedViewModel: SharedWorkspaceViewModel,
    onBack: () -> Unit,
) {
    val message by viewModel.message.collectAsState()
    val detail by viewModel.orderDetail.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val preparedMessage by viewModel.preparedMessage.collectAsState()
    val preparedMessageBody by viewModel.preparedMessageBody.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isGeneratingStatusPdf by viewModel.isGeneratingStatusPdf.collectAsState()
    val statusPdfError by viewModel.statusPdfError.collectAsState()
    val showPdfSentConfirmation by viewModel.showPdfSentConfirmation.collectAsState()
    val pendingShareIntent by viewModel.pendingShareIntent.collectAsState()
    val context = LocalContext.current
    val workspaceState = rememberOrderDetailWorkspaceState()
    val decision = workspaceState.quoteDecision
    val shareStatusChooserTitle = stringResource(R.string.order_share_status_chooser)

    val actor by viewModel.currentUser.collectAsState()
    val canDeleteOrder =
        actor?.permissions?.contains(com.onlytechtake.repairapp.data.local.entities.UserPermission.EDIT_ALL_ORDERS) == true

    val exitConfirmTitle = stringResource(R.string.order_exit_confirm_title)
    val exitConfirmText = stringResource(R.string.order_exit_confirm_text)
    val diagnosisOutcomeConfirmTitle =
        stringResource(R.string.order_confirm_diagnosis_outcome_title)
    val diagnosisOutcomeConfirmText = stringResource(R.string.order_confirm_diagnosis_outcome_text)
    val deleteOrderConfirmTitle =
        stringResource(
            R.string.action_delete_confirm_title,
            stringResource(R.string.label_repair_order),
        )
    val deleteOrderConfirmText =
        stringResource(
            R.string.action_delete_confirm_text,
            stringResource(R.string.label_repair_order),
        )
    val notRepairableNote = stringResource(R.string.order_not_repairable_note)

    val selected = detail
    if (selected == null) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(RepairAppSpacing.screenHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingBrandIndicator(text = stringResource(R.string.order_loading_detail))
        }
        return
    }

    LaunchedEffect(selected.order.id, selected.order.status) {
        workspaceState.resetQuoteDecision()
    }
    DisposableEffect(selected.order.id) {
        onDispose { viewModel.clearPreparedMessage() }
    }

    LaunchedEffect(pendingShareIntent) {
        pendingShareIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, shareStatusChooserTitle))
            viewModel.clearPendingShareIntent()
        }
    }

    val autoSaveSignal by sharedViewModel.autoSaveSignal.collectAsState()
    LaunchedEffect(autoSaveSignal) {
        if ((autoSaveSignal > 0) && (preparedMessage != null)) {
            viewModel.recordPreparedMessage()
        }
    }

    val hasUnsavedLocalWork = (workspaceState.note.isNotBlank() || (preparedMessage != null))
    BackHandler(enabled = true) {
        if (hasUnsavedLocalWork) {
            workspaceState.requestConfirm(exitConfirmTitle, exitConfirmText) {
                workspaceState.note = ""
                onBack()
            }
        } else {
            onBack()
        }
    }

    var notesExpanded by rememberSaveable { mutableStateOf(value = false) }
    var photosExpanded by rememberSaveable { mutableStateOf(value = false) }
    var timelineExpanded by rememberSaveable { mutableStateOf(value = false) }
    var signaturesExpanded by rememberSaveable { mutableStateOf(value = false) }

    Column(Modifier.fillMaxSize()) {
        message?.let {
            DismissibleAppMessageBanner(
                message = it.text,
                type = if (it.isError) BannerType.Error else BannerType.Success,
                onDismiss = viewModel::clearMessage,
                modifier = Modifier.padding(RepairAppSpacing.compactGap),
            )
        }

        if (isProcessing) {
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(RepairAppSpacing.compactGap),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        AppScreenContainer(scrollable = false) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag(RepairAppTestTags.OrderDetailLazyColumn)
                        .imePadding(),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                contentPadding =
                    PaddingValues(
                        top = RepairAppSpacing.compactGap,
                        bottom = RepairAppSpacing.bottomContentPadding,
                    ),
            ) {
                item(key = "header", contentType = "header") {
                    OrderHeader(selected, context)
                }

                item(key = "actions", contentType = "actions") {
                    val isResultStep = selected.order.status == OrderStatus.RESULT
                    val outcome = selected.order.diagnosisOutcome
                    val isRepairable = outcome == DiagnosisOutcome.REPAIRABLE
                    val isNotRepairable = outcome == DiagnosisOutcome.NOT_REPAIRABLE

                    val primaryOverride =
                        when {
                            (isResultStep && isRepairable && (decision == QuoteDecision.REJECTED)) -> {
                                OrderStatusAction.CLOSE_ORDER
                            }

                            isResultStep && isNotRepairable -> {
                                OrderStatusAction.READY_FOR_PICKUP
                            }

                            else -> {
                                null
                            }
                        }

                    val primaryLabel =
                        if (isResultStep && isRepairable && (decision == QuoteDecision.REJECTED)) {
                            stringResource(R.string.action_close_order)
                        } else {
                            null
                        }

                    val isPrimaryEnabled =
                        if (isResultStep && isRepairable) {
                            decision != null
                        } else {
                            true
                        }

                    val quoteAcceptedLabel = stringResource(R.string.order_quote_accepted)

                    OrderActionCard(
                        status = selected.order.status,
                        isPaid = selected.isPaid,
                        whatsAppAnchor = statusTemplateAnchor(selected.order),
                        onActionClick = { action ->
                            val isActionMatchDecision =
                                (isResultStep && isRepairable && (decision != null)) &&
                                    ((action == primaryOverride) || ((primaryOverride == null) && (action == OrderStatusAction.START_REPAIR)))

                            val isActionMatchNotRepairable =
                                isResultStep && isNotRepairable && (action == OrderStatusAction.READY_FOR_PICKUP)

                            when {
                                isActionMatchDecision -> {
                                    if (decision == QuoteDecision.ACCEPTED) {
                                        viewModel.recordFeedback(
                                            CustomerFeedbackType.ACCEPT,
                                            quoteAcceptedLabel,
                                        ) {
                                            viewModel.prepareMessage(
                                                TemplateAnchor.WHATSAPP_REPAIRING,
                                            )
                                        }
                                    } else {
                                        viewModel.recordFeedback(
                                            CustomerFeedbackType.REJECT,
                                            workspaceState.quoteRejectNote,
                                            ClosureReason.CUSTOMER_DECLINED,
                                        ) {
                                            viewModel.prepareMessage(
                                                TemplateAnchor.WHATSAPP_NOT_REPAIRABLE,
                                            )
                                        }
                                    }
                                }

                                isActionMatchNotRepairable -> {
                                    workspaceState.requestConfirm(
                                        diagnosisOutcomeConfirmTitle,
                                        diagnosisOutcomeConfirmText,
                                    ) {
                                        viewModel.changeStatus(
                                            status = OrderStatus.READY_FOR_PICKUP,
                                            note = notRepairableNote,
                                        ) {
                                            viewModel.prepareMessage(
                                                TemplateAnchor.WHATSAPP_NOT_REPAIRABLE,
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    workspaceState.openStatusAction(
                                        action,
                                        selected.order.diagnosisOutcome,
                                    )
                                }
                            }
                        },
                        onTogglePayment = { viewModel.togglePaymentStatus(!selected.isPaid) },
                        onSendWhatsApp = viewModel::prepareMessage,
                        onDeleteOrder =
                            if (canDeleteOrder) {
                                {
                                    workspaceState.requestConfirm(
                                        deleteOrderConfirmTitle,
                                        deleteOrderConfirmText,
                                    ) {
                                        viewModel.deleteOrder { onBack() }
                                    }
                                }
                            } else {
                                null
                            },
                        isPrimaryActionEnabled = isPrimaryEnabled,
                        primaryActionLabel = primaryLabel,
                        primaryActionOverride = primaryOverride,
                        showSecondaryActions = !isResultStep,
                    )
                }

                if (selected.order.status == OrderStatus.RESULT) {
                    when (selected.order.diagnosisOutcome) {
                        DiagnosisOutcome.REPAIRABLE -> {
                            item(key = "quote_decision", contentType = "decision") {
                                QuoteDecisionPanel(
                                    quoteDecision = workspaceState.quoteDecision,
                                    quoteRejectNote = workspaceState.quoteRejectNote,
                                    onQuoteDecisionChange = { workspaceState.quoteDecision = it },
                                ) {
                                    workspaceState.quoteRejectNote = it
                                }
                            }
                        }

                        DiagnosisOutcome.NOT_REPAIRABLE -> {
                            item(key = "not_repairable", contentType = "info") {
                                StandardCard {
                                    Column(
                                        modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                                    ) {
                                        Text(
                                            stringResource(R.string.order_not_repairable_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        OperatorText(stringResource(R.string.order_not_repairable_body))
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }

                item(key = "notes", contentType = "notes") {
                    val hasNotes = selected.order.internalNotes?.isNotBlank() == true
                    StandardCard(
                        onClick = { notesExpanded = !notesExpanded },
                        highlight = notesExpanded || hasNotes,
                        modifier = Modifier.testTag(RepairAppTestTags.OrderNotesSectionCard),
                    ) {
                        Column(modifier = Modifier.padding(RepairAppSpacing.compactGap)) {
                            ExpandableHeader(
                                title = stringResource(R.string.order_internal_notes),
                                isExpanded = notesExpanded,
                                showBadge = hasNotes && !notesExpanded,
                                badgeText = if (hasNotes) "1" else null,
                            )
                            if (notesExpanded) {
                                Column(modifier = Modifier.padding(horizontal = RepairAppSpacing.tinyGap)) {
                                    InternalNotesPanel(
                                        notes = selected.order.internalNotes,
                                    ) {
                                        workspaceState.openInternalNote()
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "photos", contentType = "photos") {
                    val mediaCount = selected.media.size
                    StandardCard(
                        onClick = { photosExpanded = !photosExpanded },
                        highlight = photosExpanded || (mediaCount > 0),
                        modifier = Modifier.testTag(RepairAppTestTags.OrderPhotosSectionCard),
                    ) {
                        Column(modifier = Modifier.padding(RepairAppSpacing.compactGap)) {
                            ExpandableHeader(
                                title = stringResource(R.string.intake_step_media),
                                isExpanded = photosExpanded,
                                showBadge = (mediaCount > 0) && !photosExpanded,
                                badgeText = mediaCount.toString(),
                            )
                            if (photosExpanded) {
                                Column(modifier = Modifier.padding(horizontal = RepairAppSpacing.tinyGap)) {
                                    OrderPhotoSection(selected, viewModel)
                                }
                            }
                        }
                    }
                }

                item(key = "signatures", contentType = "signatures") {
                    val signatureCount = selected.signatures.size
                    StandardCard(
                        onClick = { signaturesExpanded = !signaturesExpanded },
                        highlight = signaturesExpanded || (signatureCount > 0),
                        modifier = Modifier.testTag(RepairAppTestTags.OrderSignaturesCard),
                    ) {
                        Column(modifier = Modifier.padding(RepairAppSpacing.compactGap)) {
                            ExpandableHeader(
                                title = stringResource(R.string.signature_section_title),
                                isExpanded = signaturesExpanded,
                                showBadge = (signatureCount > 0) && !signaturesExpanded,
                                badgeText = signatureCount.toString(),
                            )
                            if (signaturesExpanded) {
                                OrderSignatureSection(
                                    detail = selected,
                                    onDeleteSignature = viewModel::deleteSignature,
                                )
                            }
                        }
                    }
                }

                item(key = "documents", contentType = "documents") {
                    OrderDocumentSection(
                        isGeneratingPdf = isGeneratingStatusPdf,
                        pdfError = statusPdfError,
                        documents = documents,
                        hasSignatures = selected.signatures.isNotEmpty(),
                        onGeneratePdf = { includeSignature ->
                            viewModel.generateStatusPdf(includeSignature = includeSignature)
                        },
                        onShareDocument = { viewModel.onShareDocumentClicked(it) },
                        onWhatsAppDocument = { viewModel.onWhatsAppDocumentClicked() },
                        onDeleteDocument = { docId ->
                            viewModel.onDeleteDocumentClicked(docId)
                        },
                    ) { title, text, action ->
                        workspaceState.requestConfirm(title, text, action)
                    }
                }

                item(key = "timeline", contentType = "timeline") {
                    val timelineCount = selected.timeline.size
                    StandardCard(
                        onClick = { timelineExpanded = !timelineExpanded },
                        highlight = timelineExpanded,
                        modifier = Modifier.testTag(RepairAppTestTags.OrderTimelineSection),
                    ) {
                        Column(modifier = Modifier.padding(RepairAppSpacing.compactGap)) {
                            ExpandableHeader(
                                title = stringResource(R.string.order_timeline),
                                isExpanded = timelineExpanded,
                                showBadge = (timelineCount > 0) && !timelineExpanded,
                                badgeText = timelineCount.toString(),
                            )
                            if (timelineExpanded) {
                                OrderTimelineSection(timeline = selected.timeline)
                            }
                        }
                    }
                }

                item(key = "footer", contentType = "actions") {
                    AppButton(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = RepairAppSpacing.screenHorizontal),
                        text = stringResource(R.string.action_close),
                        variant = AppButtonVariant.Secondary,
                        onClick = onBack,
                    )
                }
            }
        }
    }

    OrderConfirmDialog(workspaceState)

    OrderStatusActionDialog(
        state = workspaceState,
        context = context,
        onRecordResult = { summary, outcome, estimateAmountCents ->
            viewModel.recordResult(
                summary = summary,
                outcome = outcome,
                estimateMillis = estimateAmountCents,
            ) {
                viewModel.prepareMessage(
                    if (outcome == DiagnosisOutcome.REPAIRABLE) {
                        TemplateAnchor.WHATSAPP_QUOTE
                    } else {
                        TemplateAnchor.WHATSAPP_NOT_REPAIRABLE
                    },
                )
            }
        },
    ) { targetStatus, statusNote, closureReason ->
        viewModel.changeStatus(
            status = targetStatus,
            note = statusNote,
            closureReason = closureReason,
        ) {
            when (targetStatus) {
                OrderStatus.CLOSED -> {
                    if (closureReason == ClosureReason.NOT_REPAIRABLE) {
                        viewModel.prepareMessage(TemplateAnchor.WHATSAPP_NOT_REPAIRABLE)
                    } else {
                        viewModel.prepareMessage(TemplateAnchor.WHATSAPP_COMPLETED)
                    }
                }

                OrderStatus.IN_PROGRESS -> {
                    viewModel.prepareMessage(TemplateAnchor.WHATSAPP_UPDATE)
                }

                OrderStatus.DIAGNOSIS -> {
                    viewModel.prepareMessage(TemplateAnchor.WHATSAPP_DIAGNOSIS)
                }

                OrderStatus.REPAIRING -> {
                    viewModel.prepareMessage(TemplateAnchor.WHATSAPP_REPAIRING)
                }

                OrderStatus.READY_FOR_PICKUP -> {
                    viewModel.prepareMessage(TemplateAnchor.WHATSAPP_READY)
                }

                else -> {}
            }
        }
    }

    InternalNoteDialog(
        state = workspaceState,
        currentInternalNotes = selected.order.internalNotes,
        onAddNote = viewModel::addNote,
    )

    OrderPdfSentConfirmationDialog(
        show = showPdfSentConfirmation,
        onDismiss = viewModel::onPdfSentDismissed,
        onConfirm = viewModel::onPdfSentConfirmed,
    )

    WhatsAppPreviewDialog(
        prepared = preparedMessage?.takeIf { it.orderId == selected.order.id },
        body = preparedMessageBody,
        onBodyChange = viewModel::updatePreparedMessageBody,
        onOpenWhatsApp = {
            viewModel.recordPreparedMessage()
            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    "https://wa.me/${preparedMessage?.phone}?text=${
                        Uri.encode(
                            preparedMessageBody,
                        )
                    }".toUri(),
                )
            context.startActivity(intent)
        },
        onConfirmSent = {
            viewModel.confirmSentMessage()
            viewModel.clearPreparedMessage()
        },
        onClose = viewModel::clearPreparedMessage,
    )
}

@Composable
private fun ExpandableHeader(
    title: String,
    isExpanded: Boolean,
    showBadge: Boolean = false,
    badgeText: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(RepairAppSpacing.tinyGap),
            )
            if (showBadge) {
                androidx.compose.foundation.layout
                    .Spacer(Modifier.padding(4.dp))
                com.onlytechtake.repairapp.core.design.components.AppBadge(
                    text = badgeText ?: "!",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            if (isExpanded) {
                stringResource(R.string.action_close)
            } else {
                stringResource(
                    R.string.content_description_open,
                )
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    name = "Dark Mode",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun OrderDetailWorkspacePreview() {
    val context = LocalContext.current
    val container = FakeAppContainer(context)
    RepairAppTheme {
        OrderDetailWorkspace(
            viewModel =
                viewModel(
                    factory =
                        OrderDetailViewModel.Factory(
                            context = context,
                            orderId = "PREVIEW-ID",
                            authRepository = container.authRepository,
                            orderRepository = container.orderWorkflowRepository,
                            orderMediaRepository = container.orderMediaRepository,
                            orderDocumentRepository = container.orderDocumentRepository,
                            templateRepository = container.templateRepository,
                            generateOrderStatusPdfUseCase = container.generateOrderStatusPdfUseCase,
                            shareDocumentIntentFactory = container.shareDocumentIntentFactory,
                            diagnosticLogger = container.diagnosticLogger,
                            onSetFormDirty = {},
                        ),
                ),
            sharedViewModel = viewModel(),
        ) {}
    }
}
