package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

    val exitConfirmTitle = stringResource(R.string.order_exit_confirm_title)
    val exitConfirmText = stringResource(R.string.order_exit_confirm_text)
    val waOpenConfirmTitle = stringResource(R.string.order_wa_open_confirm_title)
    val waOpenConfirmText = stringResource(R.string.order_wa_open_confirm_text)
    val waSentConfirmTitle = stringResource(R.string.order_wa_sent_confirm_title)
    val waSentConfirmText = stringResource(R.string.order_wa_sent_confirm_text)

    var note by rememberSaveable { mutableStateOf("") }
    var showInternalNoteDialog by rememberSaveable { mutableStateOf(value = false) }
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var statusAction by remember { mutableStateOf<OrderStatusAction?>(null) }
    var statusNote by rememberSaveable { mutableStateOf("") }
    var closureReason by rememberSaveable { mutableStateOf<ClosureReason?>(null) }
    var resultSummary by rememberSaveable { mutableStateOf("") }
    var resultAmount by rememberSaveable { mutableStateOf("") }
    var diagnosisOutcome by rememberSaveable { mutableStateOf<DiagnosisOutcome?>(null) }
    var quoteDecision by rememberSaveable { mutableStateOf<QuoteDecision?>(null) }
    var quoteRejectNote by rememberSaveable { mutableStateOf("") }

    val selected = detail
    if (selected == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(RepairAppSpacing.screenHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingBrandIndicator(text = "Caricamento commessa...")
        }
        return
    }

    LaunchedEffect(selected.order.id, selected.order.status) {
        quoteDecision = null
        quoteRejectNote = ""
    }
    DisposableEffect(selected.order.id) {
        onDispose { viewModel.clearPreparedMessage() }
    }

    LaunchedEffect(pendingShareIntent) {
        pendingShareIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, "Invia stato avanzamento"))
            viewModel.clearPendingShareIntent()
        }
    }

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    val hasUnsavedLocalWork = (note.isNotBlank() || (preparedMessage != null))
    BackHandler(enabled = true) {
        if (hasUnsavedLocalWork) {
            requestConfirm(exitConfirmTitle, exitConfirmText) {
                note = ""
                onBack()
            }
        } else {
            onBack()
        }
    }

    Column(Modifier.fillMaxSize()) {
        message?.let { AppMessageBanner(message = it.text, type = if (it.isError) BannerType.Error else BannerType.Info, modifier = Modifier.padding(RepairAppSpacing.compactGap)) }

        AppScreenContainer(scrollable = false) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag(RepairAppTestTags.OrderDetailLazyColumn),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                contentPadding = PaddingValues(bottom = RepairAppSpacing.bottomContentPadding)
            ) {
                item {
                    OrderHeader(selected, context)
                }

                item {
                    OrderActionCard(
                        status = selected.order.status,
                        isPaid = selected.isPaid,
                        onActionClick = { action ->
                            statusAction = action
                            statusNote = ""
                            closureReason = null
                            resultSummary = ""
                            resultAmount = ""
                            diagnosisOutcome = selected.order.diagnosisOutcome
                        },
                        onTogglePayment = { viewModel.togglePaymentStatus(!selected.isPaid) },
                        onSendWhatsApp = viewModel::prepareMessage,
                    )
                }

                if ((selected.order.status == OrderStatus.RESULT) &&
                    (selected.order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE)
                ) {
                    item {
                        QuoteDecisionPanel(
                            quoteDecision = quoteDecision,
                            quoteRejectNote = quoteRejectNote,
                            onQuoteDecisionChange = { quoteDecision = it },
                            onQuoteRejectNoteChange = { quoteRejectNote = it },
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
                    item {
                        StandardCard {
                            Column(
                                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                            ) {
                                Text("Esito diagnosi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                OperatorText("Il dispositivo non e' riparabile. Avvisa il cliente prima di preparare il ritiro.")
                                AppButton(
                                    text = "Cliente avvisato / pronto per il ritiro",
                                    variant = AppButtonVariant.Success,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        requestConfirm(
                                            "Cliente avvisato?",
                                            "La commessa passera' a pronta per il ritiro.",
                                        ) {
                                            viewModel.changeStatus(
                                                status = OrderStatus.READY_FOR_PICKUP,
                                                note = "Cliente avvisato: dispositivo non riparabile",
                                            )
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
                    item {
                        WhatsAppPreviewPanel(
                            prepared = prepared,
                            body = preparedMessageBody,
                            onBodyChange = viewModel::updatePreparedMessageBody,
                            onOpenWhatsApp = {
                                requestConfirm(waOpenConfirmTitle, waOpenConfirmText) {
                                    viewModel.recordPreparedMessage()
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://wa.me/${prepared.phone}?text=${Uri.encode(preparedMessageBody)}".toUri(),
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            onConfirmSent = {
                                requestConfirm(waSentConfirmTitle, waSentConfirmText) { viewModel.confirmSentMessage() }
                            },
                            onClose = viewModel::clearPreparedMessage,
                        )
                    }
                }

                item {
                    InternalNotesPanel(
                        notes = selected.order.internalNotes,
                        onAddNoteClick = {
                            note = ""
                            showInternalNoteDialog = true
                        },
                    )
                }

                item {
                    OrderPhotoSection(selected, viewModel)
                }

                item {
                    OrderSignatureSection(selected)
                }

                item {
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
                            requestConfirm(
                                "Eliminare documento?",
                                "Il file verra' rimosso permanentemente."
                            ) {
                                viewModel.onDeleteDocumentClicked(docId)
                            }
                        },
                        requestConfirm = ::requestConfirm
                    )
                }

                item {
                    OrderTimelineSection(timeline = selected.timeline)
                }

                item {
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

    confirmTitle?.let { title ->
        AppDialog(
            onDismissRequest = {
                confirmTitle = null
                confirmAction = null
            },
            title = title,
            text = confirmText,
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        confirmAction?.invoke()
                        confirmTitle = null
                        confirmAction = null
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = {
                        confirmTitle = null
                        confirmAction = null
                    },
                )
            },
        )
    }

    statusAction?.let { action ->
        val isResultStep = action == OrderStatusAction.RECORD_RESULT
        val isClosure = action.requiresConfirmation()
        AppDialog(
            onDismissRequest = { statusAction = null },
            title = action.label,
            content = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
                ) {
                    Text(stringResource(R.string.order_confirm_status_update))
                    if (isResultStep) {
                        AppTextField(
                            value = resultSummary,
                            onValueChange = { resultSummary = it },
                            label = stringResource(R.string.order_result_summary),
                            required = true,
                            showError = resultSummary.isBlank(),
                            imeAction = ImeAction.Next,
                            testTag = RepairAppTestTags.StatusResultSummaryField,
                        )
                        Text("Esito diagnosi", style = MaterialTheme.typography.titleSmall)
                        QuoteDecisionRow(
                            text = stringResource(R.string.order_diagnosis_repairable),
                            checked = diagnosisOutcome == DiagnosisOutcome.REPAIRABLE,
                            testTag = RepairAppTestTags.DiagnosisRepairableToggle,
                        ) {
                            diagnosisOutcome = DiagnosisOutcome.REPAIRABLE
                        }
                        QuoteDecisionRow(
                            text = stringResource(R.string.order_diagnosis_not_repairable),
                            checked = diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE,
                            testTag = RepairAppTestTags.DiagnosisNotRepairableToggle,
                        ) {
                            diagnosisOutcome = DiagnosisOutcome.NOT_REPAIRABLE
                            resultAmount = ""
                        }
                        if (shouldShowDiagnosisEstimate(diagnosisOutcome)) {
                            AppTextField(
                                value = resultAmount,
                                onValueChange = { resultAmount = it },
                                label = stringResource(R.string.order_estimate_amount),
                                required = true,
                                showError = parseEuroCentsOrNull(resultAmount)?.let { it <= 0L } != false,
                                singleLine = true,
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Decimal,
                                testTag = RepairAppTestTags.DiagnosisEstimateField,
                            )
                        }
                    } else if (isClosure) {
                        AppTextField(
                            value = statusNote,
                            onValueChange = { statusNote = it },
                            label = stringResource(R.string.order_closure_note_label),
                            required = closureReason == ClosureReason.OTHER,
                            showError = (closureReason == ClosureReason.OTHER) && statusNote.isBlank(),
                            imeAction = ImeAction.Done,
                            testTag = RepairAppTestTags.StatusNoteField,
                        )
                    }
                    if (isClosure) {
                        Text(stringResource(R.string.order_closure_reason_title), style = MaterialTheme.typography.titleSmall)
                        ActionRow {
                            ClosureReason.entries.forEach { reason ->
                                val selectedReason = closureReason == reason
                                AppButton(
                                    text = reason.label(context),
                                    variant = if (selectedReason) AppButtonVariant.Success else AppButtonVariant.Secondary,
                                    selected = selectedReason,
                                    stateDescription = if (selectedReason) stringResource(R.string.state_selected) else stringResource(R.string.state_not_selected),
                                    testTag = RepairAppTestTags.closureReason(reason.name),
                                    onClick = { closureReason = reason },
                                    size = AppButtonSize.Compact,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.confirm),
                    enabled = when {
                        isResultStep -> canConfirmDiagnosisResult(
                            summary = resultSummary,
                            outcome = diagnosisOutcome,
                            estimateAmountCents = parseEuroCentsOrNull(resultAmount),
                        )
                        isClosure -> (closureReason != null) && ((closureReason != ClosureReason.OTHER) || statusNote.isNotBlank())
                        else -> true
                    },
                    onClick = {
                        if (action == OrderStatusAction.RECORD_RESULT) {
                            val outcome = diagnosisOutcome ?: return@AppButton
                            val estimateAmountCents = parseEuroCentsOrNull(resultAmount)
                            viewModel.recordResult(
                                summary = resultSummary,
                                diagnosisOutcome = outcome,
                                estimateAmountCents = estimateAmountCents,
                            ) {
                                viewModel.prepareMessage(
                                    if (outcome == DiagnosisOutcome.REPAIRABLE) {
                                        "whatsapp_preventivo_intervento"
                                    } else {
                                        "whatsapp_non_riparabile"
                                    },
                                )
                            }
                        } else {
                            val targetStatus = action.targetStatus ?: return@AppButton
                            viewModel.changeStatus(
                                status = targetStatus,
                                note = statusNote,
                                closureReason = closureReason,
                            ) {
                                if ((targetStatus == OrderStatus.CLOSED) && (closureReason == ClosureReason.NOT_REPAIRABLE)) {
                                    viewModel.prepareMessage("whatsapp_non_riparabile")
                                } else if (targetStatus == OrderStatus.CLOSED) {
                                    onBack()
                                }
                            }
                        }
                        statusAction = null
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.StatusDialogConfirmButton
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = { statusAction = null }
                )
            },
        )
    }

    if (showInternalNoteDialog) {
        AppDialog(
            onDismissRequest = { showInternalNoteDialog = false },
            title = stringResource(R.string.order_internal_notes),
            content = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
                ) {
                    selected.order.internalNotes?.takeIf { it.isNotBlank() }?.let {
                        OperatorText(it)
                    }
                    AppTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = stringResource(R.string.order_new_note),
                        imeAction = ImeAction.Done,
                    )
                }
            },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.order_save_note),
                    enabled = note.isNotBlank(),
                    onClick = {
                        viewModel.addNote(note)
                        note = ""
                        showInternalNoteDialog = false
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = { showInternalNoteDialog = false },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact
                )
            },
        )
    }

    if (showPdfSentConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.onPdfSentDismissed() },
            title = stringResource(R.string.order_pdf_sent_confirm_title),
            text = stringResource(R.string.order_pdf_sent_confirm_text),
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.yes_confirmed),
                    onClick = { 
                        viewModel.onPdfSentConfirmed()
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.no_cancel),
                    onClick = { viewModel.onPdfSentDismissed() },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact
                )
            }
        )
    }
}
