package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.core.utils.parseEuroCentsOrNull
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.features.dashboard.common.*
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.canConfirmDiagnosisResult
import com.onlytechtake.repairapp.domain.workspace.requiresConfirmation
import com.onlytechtake.repairapp.domain.workspace.shouldShowDiagnosisEstimate
import com.onlytechtake.repairapp.domain.workspace.statusTemplateCode

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
    val missingSignatureTitle = stringResource(R.string.order_pdf_missing_signature_title)
    val missingSignatureText = stringResource(R.string.order_pdf_missing_signature_text)
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
            modifier = Modifier.fillMaxSize().padding(DashboardSpacing.screenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.CircularProgressIndicator()
            Text("Caricamento commessa...", modifier = Modifier.padding(top = 8.dp))
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
        LocalMessageBanner(message?.text, message?.isError ?: false)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(DashboardSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
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
                onTogglePayment = viewModel::togglePaymentStatus,
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
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Text("Esito diagnosi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        OperatorText("Il dispositivo non e' riparabile. Avvisa il cliente prima di preparare il ritiro.")
                        TouchButton(
                            text = "Cliente avvisato / pronto per il ritiro",
                            tone = ButtonTone.SUCCESS,
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
            StandardCard {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text(stringResource(R.string.order_customer_documents), style = MaterialTheme.typography.titleMedium)
                    ActionRow {
                        TouchButton(
                            text = stringResource(R.string.order_generate_status_pdf),
                            tone = ButtonTone.INFO,
                            enabled = !isGeneratingStatusPdf,
                            onClick = {
                                if (selected.signatures.isEmpty()) {
                                    requestConfirm(missingSignatureTitle, missingSignatureText) {
                                        viewModel.generateStatusPdf(includeSignature = false)
                                    }
                                } else {
                                    viewModel.generateStatusPdf(includeSignature = true)
                                }
                            },
                        )
                    }
                    if (isGeneratingStatusPdf) {
                        Text(stringResource(R.string.order_generating_pdf), style = MaterialTheme.typography.bodyMedium)
                    }
                    statusPdfError?.let { error ->
                        Text("Errore: $error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (documents.isNotEmpty()) {
                        Text(stringResource(R.string.order_document_history), style = MaterialTheme.typography.titleSmall)
                        documents.forEach { doc ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                ) {
                                Row(
                                    modifier = Modifier.weight(1f).clickable { viewModel.onShareDocumentClicked(doc.id) },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.fileName, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = stringResource(R.string.order_document_generated, formatUiDateTime(doc.createdAt)) + (if (doc.sharedAt != null) stringResource(R.string.order_document_sent) else ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                    TouchButton(
                                        text = stringResource(R.string.order_share),
                                        tone = ButtonTone.NEUTRAL,
                                        onClick = { viewModel.onShareDocumentClicked(doc.id) }
                                    )
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        requestConfirm(
                                            "Eliminare documento?",
                                            "Il file ${doc.fileName} verra' rimosso permanentemente."
                                        ) {
                                            viewModel.onDeleteDocumentClicked(doc.id)
                                        }
                                    }
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Elimina",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
                SectionTitle(
                    text = stringResource(R.string.order_timeline),
                    tooltip = "Cronologia di tutti gli eventi e cambiamenti di stato relativi a questa commessa."
                )
                selected.timeline.forEachIndexed { index, event ->
                    val eventNumber = selected.timeline.size - index
                    CompactExpandableRow(
                        title = "$eventNumber. ${event.title}",
                        subtitle = formatUiDateTime(event.createdAt),
                        detail = event.description.orEmpty(),
                    )
                }
            }
        }

        item {
            TouchButton(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                text = "Chiudi ed esci",
                tone = ButtonTone.NEUTRAL,
                onClick = onBack
            )
        }
    }

    confirmTitle?.let { title ->
        AlertDialog(
            onDismissRequest = {
                confirmTitle = null
                confirmAction = null
            },
            title = { Text(title) },
            text = { Text(confirmText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction?.invoke()
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TouchButton(
                    text = stringResource(R.string.customer_cancel),
                    tone = ButtonTone.NEUTRAL,
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
        AlertDialog(
            onDismissRequest = { statusAction = null },
            title = { Text(action.label) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)
                ) {
                    Text(stringResource(R.string.order_confirm_status_update))
                    if (isResultStep) {
                        Field(
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
                            Field(
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
                        Field(
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
                                TouchButton(
                                    text = reason.label(context),
                                    tone = if (selectedReason) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                                    selected = selectedReason,
                                    stateDescription = if (selectedReason) stringResource(R.string.state_selected) else stringResource(R.string.state_not_selected),
                                    testTag = RepairAppTestTags.closureReason(reason.name),
                                    onClick = { closureReason = reason },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
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
                            val outcome = diagnosisOutcome ?: return@TextButton
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
                            val targetStatus = action.targetStatus ?: return@TextButton
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
                    modifier = Modifier.testTag(RepairAppTestTags.StatusDialogConfirmButton),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TouchButton(
                    text = stringResource(R.string.customer_cancel),
                    tone = ButtonTone.NEUTRAL,
                    onClick = { statusAction = null }
                )
            },
        )
    }

    if (showInternalNoteDialog) {
        AlertDialog(
            onDismissRequest = { showInternalNoteDialog = false },
            title = { Text(stringResource(R.string.order_internal_notes)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)
                ) {
                    selected.order.internalNotes?.takeIf { it.isNotBlank() }?.let {
                        OperatorText(it)
                    }
                    Field(
                        value = note,
                        onValueChange = { note = it },
                        label = stringResource(R.string.order_new_note),
                        imeAction = ImeAction.Done,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = note.isNotBlank(),
                    onClick = {
                        viewModel.addNote(note)
                        note = ""
                        showInternalNoteDialog = false
                    },
                ) {
                    Text(stringResource(R.string.order_save_note))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInternalNoteDialog = false }) {
                    Text(stringResource(R.string.customer_cancel))
                }
            },
        )
    }

    if (showPdfSentConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onPdfSentDismissed() },
            title = { Text(stringResource(R.string.order_pdf_sent_confirm_title)) },
            text = { Text(stringResource(R.string.order_pdf_sent_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.onPdfSentConfirmed()
                    }
                ) {
                    Text(stringResource(R.string.yes_confirmed))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onPdfSentDismissed() }) {
                    Text(stringResource(R.string.no_cancel))
                }
            }
        )
    }
    }
}


