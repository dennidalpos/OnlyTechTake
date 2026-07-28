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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.LoadingBrandIndicator
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.domain.logic.OrderStatusAction
import com.onlytechtake.repairapp.domain.logic.statusTemplateAnchor
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.models.UserPermission
import com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision

@Composable
fun OrderDetailWorkspace(
    viewModel: OrderDetailViewModel,
    showCloseButton: Boolean = true,
    onBack: () -> Unit,
) {
    val state by viewModel.viewState.collectAsState()
    val message by viewModel.message.collectAsState()
    val pendingShareIntent by viewModel.pendingShareIntent.collectAsState()
    val actor by viewModel.currentUser.collectAsState()

    val context = LocalContext.current
    val workspaceState = rememberOrderDetailWorkspaceState()
    val decision = workspaceState.quoteDecision
    val shareStatusChooserTitle = stringResource(R.string.order_share_status_chooser)

    val canDeleteOrder = actor?.permissions?.contains(UserPermission.EDIT_ALL_ORDERS) == true

    val exitConfirmTitle = stringResource(R.string.order_exit_confirm_title)
    val exitConfirmText = stringResource(R.string.order_exit_confirm_text)

    val selected = state.detail
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

    val hasUnsavedLocalWork = (workspaceState.note.isNotBlank() || (state.preparedMessage != null))
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
                type = if (it.isError) BannerType.Error else BannerType.Success,
                onDismiss = viewModel::clearMessage,
                modifier = Modifier.padding(RepairAppSpacing.compactGap),
            )
        }

        if (state.isProcessing) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().padding(RepairAppSpacing.compactGap),
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
                modifier = Modifier.weight(1f).testTag(RepairAppTestTags.OrderDetailLazyColumn).imePadding(),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                contentPadding = PaddingValues(top = RepairAppSpacing.compactGap, bottom = RepairAppSpacing.bottomContentPadding),
            ) {
                item(key = "header") { OrderHeader(selected, context) }

                item(key = "actions") {
                    val isResultStep = selected.order.status == OrderStatus.RESULT
                    val isRepairable = selected.order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE
                    val isNotRepairable = selected.order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE

                    val primaryOverride =
                        when {
                            isResultStep && isRepairable && decision == QuoteDecision.REJECTED -> OrderStatusAction.CLOSE_ORDER
                            isResultStep && isNotRepairable -> OrderStatusAction.READY_FOR_PICKUP
                            else -> null
                        }

                    val primaryLabel =
                        if (isResultStep && isRepairable && decision == QuoteDecision.REJECTED) {
                            stringResource(R.string.action_close_order)
                        } else {
                            null
                        }

                    val deleteConfirmTitle = stringResource(R.string.action_delete_confirm_title, stringResource(R.string.label_repair_order))
                    val deleteConfirmText = stringResource(R.string.action_delete_confirm_text, stringResource(R.string.label_repair_order))

                    OrderActionCard(
                        status = selected.order.status,
                        isPaid = selected.isPaid,
                        whatsAppAnchor = statusTemplateAnchor(selected.order),
                        onActionClick = { action ->
                            viewModel.onActionClick(
                                action = action,
                                diagnosisOutcome = selected.order.diagnosisOutcome,
                                decision = workspaceState.quoteDecision,
                                quoteRejectNote = workspaceState.quoteRejectNote,
                                requestConfirm = workspaceState::requestConfirm,
                                openStatusAction = workspaceState::openStatusAction,
                            )
                        },
                        onTogglePayment = { viewModel.togglePaymentStatus(!selected.isPaid) },
                        onSendWhatsApp = viewModel::prepareMessage,
                        onDeleteOrder =
                            if (canDeleteOrder) {
                                {
                                    workspaceState.requestConfirm(
                                        deleteConfirmTitle,
                                        deleteConfirmText,
                                    ) {
                                        viewModel.deleteOrder { onBack() }
                                    }
                                }
                            } else {
                                null
                            },
                        isPrimaryActionEnabled = if (isResultStep && isRepairable) decision != null else true,
                        primaryActionLabel = primaryLabel,
                        primaryActionOverride = primaryOverride,
                        showSecondaryActions = !isResultStep,
                    )
                }

                if (selected.order.status == OrderStatus.RESULT) {
                    item(key = "quote_decision") {
                        when (selected.order.diagnosisOutcome) {
                            DiagnosisOutcome.REPAIRABLE -> {
                                QuoteDecisionPanel(
                                    quoteDecision = workspaceState.quoteDecision,
                                    quoteRejectNote = workspaceState.quoteRejectNote,
                                    onQuoteDecisionChange = { workspaceState.quoteDecision = it },
                                    onQuoteRejectNoteChange = { workspaceState.quoteRejectNote = it },
                                )
                            }

                            DiagnosisOutcome.NOT_REPAIRABLE -> {
                                StandardCard {
                                    Column(
                                        modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                                    ) {
                                        Text(stringResource(R.string.order_not_repairable_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                        com.onlytechtake.repairapp.features.dashboard.common
                                            .OperatorText(stringResource(R.string.order_not_repairable_body))
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }

                item(key = "notes") {
                    val hasNotes = selected.order.internalNotes?.isNotBlank() == true
                    StandardCard(onClick = viewModel::toggleNotesExpanded, highlight = state.notesExpanded || hasNotes) {
                        Column(modifier = Modifier.padding(RepairAppSpacing.compactGap)) {
                            ExpandableHeader(title = stringResource(R.string.order_internal_notes), isExpanded = state.notesExpanded, showBadge = hasNotes && !state.notesExpanded, badgeText = if (hasNotes) "1" else null)
                            if (state.notesExpanded) {
                                InternalNotesPanel(notes = selected.order.internalNotes, onAddNoteClick = workspaceState::openInternalNote)
                            }
                        }
                    }
                }

                item(key = "photos") {
                    val mediaCount = selected.media.size
                    StandardCard(onClick = viewModel::togglePhotosExpanded, highlight = state.photosExpanded || (mediaCount > 0)) {
                        Column(modifier = Modifier.padding(RepairAppSpacing.compactGap)) {
                            ExpandableHeader(title = stringResource(R.string.intake_step_media), isExpanded = state.photosExpanded, showBadge = (mediaCount > 0) && !state.photosExpanded, badgeText = mediaCount.toString())
                            if (state.photosExpanded) OrderPhotoSection(selected, viewModel)
                        }
                    }
                }

                item(key = "signatures") {
                    val signatureCount = selected.signatures.size
                    StandardCard(onClick = viewModel::toggleSignaturesExpanded, highlight = state.signaturesExpanded || (signatureCount > 0)) {
                        Column(modifier = Modifier.padding(RepairAppSpacing.compactGap)) {
                            ExpandableHeader(title = stringResource(R.string.signature_section_title), isExpanded = state.signaturesExpanded, showBadge = (signatureCount > 0) && !state.signaturesExpanded, badgeText = signatureCount.toString())
                            if (state.signaturesExpanded) OrderSignatureSection(detail = selected, onDeleteSignature = viewModel::deleteSignature)
                        }
                    }
                }

                item(key = "documents") {
                    OrderDocumentSection(
                        isGeneratingPdf = state.isGeneratingStatusPdf,
                        pdfError = state.statusPdfError,
                        documents = state.documents,
                        hasSignatures = selected.signatures.isNotEmpty(),
                        onGeneratePdf = { viewModel.generateStatusPdf(includeSignature = it) },
                        onShareDocument = viewModel::onShareDocumentClicked,
                        onWhatsAppDocument = { _ -> viewModel.onWhatsAppDocumentClicked() },
                        onDeleteDocument = viewModel::onDeleteDocumentClicked,
                        requestConfirm = workspaceState::requestConfirm,
                    )
                }

                item(key = "timeline") {
                    val timelineCount = selected.timeline.size
                    StandardCard(onClick = viewModel::toggleTimelineExpanded, highlight = state.timelineExpanded) {
                        Column(modifier = Modifier.padding(RepairAppSpacing.compactGap)) {
                            ExpandableHeader(title = stringResource(R.string.order_timeline), isExpanded = state.timelineExpanded, showBadge = (timelineCount > 0) && !state.timelineExpanded, badgeText = timelineCount.toString())
                            if (state.timelineExpanded) OrderTimelineSection(timeline = selected.timeline)
                        }
                    }
                }

                if (showCloseButton) {
                    item(key = "footer") {
                        AppButton(modifier = Modifier.fillMaxWidth().padding(top = RepairAppSpacing.screenHorizontal), text = stringResource(R.string.action_close), variant = AppButtonVariant.Secondary, onClick = onBack)
                    }
                }
            }
        }
    }

    OrderConfirmDialog(workspaceState)
    OrderStatusActionDialog(state = workspaceState, context = context, onRecordResult = viewModel::recordResult, onChangeStatus = viewModel::changeStatus)
    InternalNoteDialog(state = workspaceState, currentInternalNotes = selected.order.internalNotes, onAddNote = viewModel::addNote)
    OrderPdfSentConfirmationDialog(show = state.showPdfSentConfirmation, onDismiss = viewModel::onPdfSentDismissed, onConfirm = viewModel::onPdfSentConfirmed)
    WhatsAppPreviewDialog(prepared = state.preparedMessage, body = state.preparedMessageBody, onBodyChange = viewModel::updatePreparedMessageBody, onOpenWhatsApp = {
        viewModel.recordPreparedMessage()
        context.startActivity(Intent(Intent.ACTION_VIEW, "https://wa.me/${state.preparedMessage?.phone}?text=${Uri.encode(state.preparedMessageBody)}".toUri()))
    }, onConfirmSent = {
        viewModel.confirmSentMessage()
        viewModel.clearPreparedMessage()
    }, onClose = viewModel::clearPreparedMessage)
}

@Composable
private fun ExpandableHeader(
    title: String,
    isExpanded: Boolean,
    showBadge: Boolean = false,
    badgeText: String? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(RepairAppSpacing.tinyGap))
            if (showBadge) {
                androidx.compose.foundation.layout
                    .Spacer(Modifier.padding(4.dp))
                com.onlytechtake.repairapp.core.design.components
                    .AppBadge(text = badgeText ?: "!", containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(if (isExpanded) stringResource(R.string.action_close) else stringResource(R.string.content_description_open), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
