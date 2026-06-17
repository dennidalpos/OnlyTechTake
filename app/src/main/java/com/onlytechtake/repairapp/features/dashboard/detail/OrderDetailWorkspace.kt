package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.core.utils.parseEuroCentsOrNull
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.core.design.LoadingBrandIndicator
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.features.dashboard.common.*
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.canConfirmDiagnosisResult
import com.onlytechtake.repairapp.domain.workspace.requiresConfirmation
import com.onlytechtake.repairapp.domain.workspace.shouldShowDiagnosisEstimate
import com.onlytechtake.repairapp.domain.workspace.statusTemplateAnchor

@Composable
fun OrderDetailWorkspace(
    viewModel: OrderDetailViewModel,
    onBack: () -> Unit,
) {
    val message by viewModel.message.collectAsState()
    val detail by viewModel.orderDetail.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val preparedMessage by viewModel.preparedMessage.collectAsState()
    val preparedMessageBody by viewModel.preparedMessageBody.collectAsState()
    val isGeneratingStatusPdf by viewModel.isGeneratingStatusPdf.collectAsState()
    val statusPdfError by viewModel.statusPdfError.collectAsState()
    val showPdfSentConfirmation by viewModel.showPdfSentConfirmation.collectAsState()
    val pendingShareIntent by viewModel.pendingShareIntent.collectAsState()
    val context = LocalContext.current
    val workspaceState = rememberOrderDetailWorkspaceState()
    val shareStatusChooserTitle = stringResource(R.string.order_share_status_chooser)

    val exitConfirmTitle = stringResource(R.string.order_exit_confirm_title)
    val exitConfirmText = stringResource(R.string.order_exit_confirm_text)
    val waOpenConfirmTitle = stringResource(R.string.order_wa_open_confirm_title)
    val waOpenConfirmText = stringResource(R.string.order_wa_open_confirm_text)
    val waSentConfirmTitle = stringResource(R.string.order_wa_sent_confirm_title)
    val waSentConfirmText = stringResource(R.string.order_wa_sent_confirm_text)
    val diagnosisOutcomeConfirmTitle = stringResource(R.string.order_confirm_diagnosis_outcome_title)
    val diagnosisOutcomeConfirmText = stringResource(R.string.order_confirm_diagnosis_outcome_text)
    val deleteDocumentConfirmTitle = stringResource(R.string.order_confirm_delete_document_title)
    val deleteDocumentConfirmText = stringResource(R.string.order_confirm_delete_document_text)

    val selected = detail
    if (selected == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(RepairAppSpacing.screenHorizontal),
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

    Column(Modifier.fillMaxSize()) {
        message?.let {
            DismissibleAppMessageBanner(
                message = it.text,
                type = if (it.isError) BannerType.Error else BannerType.Info,
                onDismiss = viewModel::clearMessage,
                modifier = Modifier.padding(RepairAppSpacing.compactGap),
            )
        }

        AppScreenContainer(scrollable = false) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag(RepairAppTestTags.OrderDetailLazyColumn)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                contentPadding = PaddingValues(
                    top = RepairAppSpacing.compactGap,
                    bottom = RepairAppSpacing.bottomContentPadding,
                )
            ) {
                item(key = "header", contentType = "header") {
                    OrderHeader(selected, context)
                }

                item(key = "actions", contentType = "actions") {
                    OrderActionCard(
                        status = selected.order.status,
                        isPaid = selected.isPaid,
                        whatsAppAnchor = statusTemplateAnchor(selected.order),
                        onActionClick = { action ->
                            workspaceState.openStatusAction(action, selected.order.diagnosisOutcome)
                        },
                        onTogglePayment = { viewModel.togglePaymentStatus(!selected.isPaid) },
                        onSendWhatsApp = viewModel::prepareMessage,
                    )
                }

                if ((selected.order.status == OrderStatus.RESULT) &&
                    (selected.order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE)
                ) {
                    item(key = "quote_decision", contentType = "decision") {
                        QuoteDecisionPanel(
                            quoteDecision = workspaceState.quoteDecision,
                            quoteRejectNote = workspaceState.quoteRejectNote,
                            onQuoteDecisionChange = { workspaceState.quoteDecision = it },
                            onQuoteRejectNoteChange = { workspaceState.quoteRejectNote = it },
                            onConfirm = { feedback, note, closureReason ->
                                if (feedback == CustomerFeedbackType.ACCEPT) {
                                    viewModel.recordFeedback(feedback, note)
                                } else {
                                    viewModel.recordFeedback(feedback, note, closureReason) { onBack() }
                                }
                            },
                        )
                    }
                }

                if ((selected.order.status == OrderStatus.RESULT) &&
                    (selected.order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE)
                ) {
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
                                AppButton(
                                    text = stringResource(R.string.order_not_repairable_ready),
                                    variant = AppButtonVariant.Success,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        workspaceState.requestConfirm(
                                            diagnosisOutcomeConfirmTitle,
                                            diagnosisOutcomeConfirmText,
                                        ) {
                                            viewModel.changeStatus(
                                                status = OrderStatus.READY_FOR_PICKUP,
                                                note = "Cliente avvisato: dispositivo non riparabile",
                                            ) {
                                                viewModel.prepareMessage(TemplateAnchor.WHATSAPP_NOT_REPAIRABLE)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                preparedMessage
                    ?.takeIf { it.orderId == selected.order.id }
                    ?.let { prepared ->
                    item(key = "wa_preview", contentType = "whatsapp") {
                        WhatsAppPreviewPanel(
                            prepared = prepared,
                            body = preparedMessageBody,
                            onBodyChange = viewModel::updatePreparedMessageBody,
                            onOpenWhatsApp = {
                                workspaceState.requestConfirm(waOpenConfirmTitle, waOpenConfirmText) {
                                    viewModel.recordPreparedMessage()
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://wa.me/${prepared.phone}?text=${Uri.encode(preparedMessageBody)}".toUri(),
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            onConfirmSent = {
                                workspaceState.requestConfirm(waSentConfirmTitle, waSentConfirmText) { viewModel.confirmSentMessage() }
                            },
                            onClose = viewModel::clearPreparedMessage,
                        )
                    }
                }

                item(key = "notes", contentType = "notes") {
                    InternalNotesPanel(
                        notes = selected.order.internalNotes,
                        onAddNoteClick = {
                            workspaceState.openInternalNote()
                        },
                    )
                }

                item(key = "photos", contentType = "photos") {
                    OrderPhotoSection(selected, viewModel)
                }

                item(key = "signatures", contentType = "signatures") {
                    OrderSignatureSection(selected)
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
                        onDeleteDocument = { docId ->
                            workspaceState.requestConfirm(
                                deleteDocumentConfirmTitle,
                                deleteDocumentConfirmText
                            ) {
                                viewModel.onDeleteDocumentClicked(docId)
                            }
                        },
                        requestConfirm = { title, text, action ->
                            workspaceState.requestConfirm(title, text, action)
                        }
                    )
                }

                item(key = "timeline", contentType = "timeline") {
                    OrderTimelineSection(timeline = selected.timeline)
                }

                item(key = "footer", contentType = "actions") {
                    AppButton(
                        modifier = Modifier.fillMaxWidth().padding(top = RepairAppSpacing.screenHorizontal),
                        text = "Chiudi ed esci",
                        variant = AppButtonVariant.Secondary,
                        onClick = onBack
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
                diagnosisOutcome = outcome,
                estimateAmountCents = estimateAmountCents,
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
        onChangeStatus = { targetStatus, statusNote, closureReason ->
            viewModel.changeStatus(
                status = targetStatus,
                note = statusNote,
                closureReason = closureReason,
            ) {
                if ((targetStatus == OrderStatus.CLOSED) && (closureReason == ClosureReason.NOT_REPAIRABLE)) {
                    viewModel.prepareMessage(TemplateAnchor.WHATSAPP_NOT_REPAIRABLE)
                } else if (targetStatus == OrderStatus.READY_FOR_PICKUP) {
                    viewModel.prepareMessage(TemplateAnchor.WHATSAPP_READY)
                } else if (targetStatus == OrderStatus.CLOSED) {
                    onBack()
                }
            }
        }
    )

    InternalNoteDialog(
        state = workspaceState,
        currentInternalNotes = selected.order.internalNotes,
        onAddNote = viewModel::addNote
    )

    OrderPdfSentConfirmationDialog(
        show = showPdfSentConfirmation,
        onDismiss = viewModel::onPdfSentDismissed,
        onConfirm = viewModel::onPdfSentConfirmed
    )
}
