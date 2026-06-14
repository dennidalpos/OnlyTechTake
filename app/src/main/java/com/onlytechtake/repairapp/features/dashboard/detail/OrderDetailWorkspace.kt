package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.Color
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
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.core.utils.parseEuroCentsOrNull
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.ExportDestination
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.CompactExpandableRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.OrderStatusStepper
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.applyStatusAction
import com.onlytechtake.repairapp.features.workspace.attachmentCounts
import com.onlytechtake.repairapp.features.workspace.label as attachmentLabel
import com.onlytechtake.repairapp.features.workspace.tone
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.availableOrderStatusActions
import com.onlytechtake.repairapp.domain.workspace.canConfirmDiagnosisResult
import com.onlytechtake.repairapp.domain.workspace.requiresConfirmation
import com.onlytechtake.repairapp.domain.workspace.shouldShowDiagnosisEstimate
import com.onlytechtake.repairapp.domain.workspace.statusTemplateCode

@Composable
fun OrderDetailWorkspace(viewModel: WorkspaceViewModel) {
    val detail by viewModel.selectedOrder.collectAsState()
    val documents by viewModel.selectedOrderDocuments.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
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
            if (uiState.selectedOrderId != null) {
                androidx.compose.material3.CircularProgressIndicator()
                Text("Caricamento commessa...", modifier = Modifier.padding(top = 8.dp))
            } else {
                Text(stringResource(R.string.order_none_selected))
                TouchButton(text = stringResource(R.string.dashboard_orders), onClick = { viewModel.navigateTo(WorkspaceDestination.ORDERS) })
            }
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

    LaunchedEffect(uiState.pendingShareIntent) {
        uiState.pendingShareIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, "Invia stato avanzamento"))
            viewModel.clearPendingShareIntent()
        }
    }

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    val hasUnsavedLocalWork = (note.isNotBlank() || (uiState.preparedMessage != null))
    BackHandler(enabled = hasUnsavedLocalWork) {
        requestConfirm(exitConfirmTitle, exitConfirmText) {
            note = ""
            viewModel.selectOrder(null)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            StandardCard(highlight = true) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                ) {
                    OrderStatusStepper(selected.order.status)
                    Text(
                        text = selected.order.status.label(context),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    if (selected.isPaid) {
                        Text(
                            text = "PAGATO",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF237A3B),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "${selected.order.orderNumber} - ${selected.order.title}",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = stringResource(
                            R.string.orders_customer_label,
                            selected.customer?.displayName(context).orEmpty()
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Operatore: ${selected.assignedUser?.displayName.orEmpty()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatUiDateTime(selected.order.updatedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (selected.order.description?.isNotBlank() == true) {
                        OperatorText(
                            text = stringResource(R.string.order_problem_label, selected.order.description),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = attachmentCounts(selected.media, selected.signatures).attachmentLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        selected.order.diagnosisOutcome?.let {
                            Text(
                                text = "• ${it.label(context)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    selected.order.closureReason?.let {
                        Text(
                            text = stringResource(R.string.closure_reason_prefix, it.label(context)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            StandardCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    SectionTitle("Azioni Commessa")
                    
                    ActionRow {
                        val actions = availableOrderStatusActions(selected.order.status)
                        val templateCode = statusTemplateCode(selected.order)

                        actions.forEach { action ->
                            TouchButton(
                                text = action.label,
                                tone = action.tone(),
                                onClick = {
                                    statusAction = action
                                    statusNote = ""
                                    closureReason = null
                                    resultSummary = ""
                                    resultAmount = ""
                                    diagnosisOutcome = selected.order.diagnosisOutcome
                                }
                            )
                        }

                        if (selected.order.status == OrderStatus.READY_FOR_PICKUP || selected.order.status == OrderStatus.CLOSED) {
                            val isPaid = selected.isPaid
                            TouchButton(
                                text = if (isPaid) stringResource(R.string.order_not_paid_button) else stringResource(R.string.order_paid_button),
                                tone = if (isPaid) ButtonTone.WARNING else ButtonTone.SUCCESS,
                                onClick = { viewModel.togglePaymentStatus(selected.order.id, !isPaid) }
                            )
                        }

                        TouchButton(
                            text = stringResource(R.string.order_whatsapp_send),
                            tone = ButtonTone.INFO,
                            onClick = { viewModel.prepareMessage(selected.order.id, templateCode) }
                        )
                    }
                }
            }
        }

        if ((selected.order.status == OrderStatus.RESULT) &&
            (selected.order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE)
        ) {
            item {
                StandardCard {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Text("Esito preventivo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        QuoteDecisionRow(
                            text = "Preventivo accettato",
                            checked = quoteDecision == QuoteDecision.ACCEPTED,
                            testTag = RepairAppTestTags.QuoteAcceptedToggle,
                        ) {
                            quoteDecision = toggleQuoteDecision(quoteDecision, QuoteDecision.ACCEPTED)
                        }
                        QuoteDecisionRow(
                            text = "Preventivo rifiutato",
                            checked = quoteDecision == QuoteDecision.REJECTED,
                            testTag = RepairAppTestTags.QuoteRejectedToggle,
                        ) {
                            quoteDecision = toggleQuoteDecision(quoteDecision, QuoteDecision.REJECTED)
                        }
                        if (quoteDecision == QuoteDecision.REJECTED) {
                            Field(
                                value = quoteRejectNote,
                                onValueChange = { quoteRejectNote = it },
                                label = "Motivo rifiuto",
                                required = true,
                                showError = quoteRejectNote.isBlank(),
                            )
                        }
                        TouchButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Conferma esito preventivo",
                            tone = if (quoteDecision == QuoteDecision.REJECTED) ButtonTone.DANGER else ButtonTone.SUCCESS,
                            enabled = canConfirmQuoteDecision(quoteDecision, quoteRejectNote),
                            testTag = RepairAppTestTags.QuoteConfirmButton,
                            onClick = {
                                when (quoteDecision) {
                                    QuoteDecision.ACCEPTED -> {
                                        viewModel.recordFeedback(
                                            orderId = selected.order.id,
                                            feedback = CustomerFeedbackType.ACCEPT,
                                            note = "Preventivo accettato",
                                        )
                                    }
                                    QuoteDecision.REJECTED -> {
                                        viewModel.recordFeedback(
                                            orderId = selected.order.id,
                                            feedback = CustomerFeedbackType.REJECT,
                                            note = quoteRejectNote,
                                            closureReason = ClosureReason.CUSTOMER_DECLINED,
                                        ) { viewModel.selectOrder(null) }
                                    }
                                    null -> Unit
                                }
                            },
                        )
                    }
                }
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
                                        orderId = selected.order.id,
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

        uiState.preparedMessage
            ?.takeIf { it.orderId == selected.order.id }
            ?.let { prepared ->
            item {
                StandardCard(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        SectionTitle(stringResource(R.string.order_wa_preview_title))
                        Field(uiState.preparedMessageBody, viewModel::updatePreparedMessageBody, stringResource(R.string.order_wa_message_label))
                        ActionRow {
                            TouchButton(
                                text = stringResource(R.string.order_wa_open),
                                tone = ButtonTone.INFO,
                                onClick = {
                                    requestConfirm(waOpenConfirmTitle, waOpenConfirmText) {
                                        viewModel.recordPreparedMessage()
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://wa.me/${prepared.phone}?text=${Uri.encode(uiState.preparedMessageBody)}".toUri(),
                                        )
                                        context.startActivity(intent)
                                    }
                                },
                            )
                            TouchButton(
                                text = stringResource(R.string.order_wa_sent),
                                tone = ButtonTone.SUCCESS,
                                onClick = { requestConfirm(waSentConfirmTitle, waSentConfirmText) { viewModel.confirmSentMessage() } },
                            )
                            TouchButton(
                                text = stringResource(R.string.order_wa_close_preview),
                                tone = ButtonTone.NEUTRAL,
                                onClick = viewModel::clearPreparedMessage,
                            )
                        }
                    }
                }
            }
        }

        item {
            StandardCard {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text(stringResource(R.string.order_internal_notes), style = MaterialTheme.typography.titleMedium)
                    selected.order.internalNotes?.takeIf { it.isNotBlank() }?.let {
                        OperatorText(it)
                    }
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.order_internal_notes),
                        tone = ButtonTone.NEUTRAL,
                        onClick = {
                            note = ""
                            showInternalNoteDialog = true
                        }
                    )
                }
            }
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
                            enabled = !uiState.isGeneratingStatusPdf,
                            onClick = {
                                if (selected.signatures.isEmpty()) {
                                    requestConfirm(missingSignatureTitle, missingSignatureText) {
                                        viewModel.onGenerateStatusPdfClicked(selected.order.id, includeSignature = false)
                                    }
                                } else {
                                    viewModel.onGenerateStatusPdfClicked(selected.order.id, includeSignature = true)
                                }
                            },
                        )
                    }
                    if (uiState.isGeneratingStatusPdf) {
                        Text(stringResource(R.string.order_generating_pdf), style = MaterialTheme.typography.bodyMedium)
                    }
                    uiState.statusPdfError?.let { error ->
                        Text("Errore: $error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (documents.isNotEmpty()) {
                        Text(stringResource(R.string.order_document_history), style = MaterialTheme.typography.titleSmall)
                        documents.forEach { doc ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                                ) {
                                Row(
                                    modifier = Modifier.weight(1f).clickable { viewModel.onShareDocumentClicked(doc.id) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.fileName, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = stringResource(R.string.order_document_generated, formatUiDateTime(doc.createdAt)) + (if (doc.sharedAt != null) stringResource(R.string.order_document_sent) else ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
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
                SectionTitle(stringResource(R.string.order_timeline))
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
                            onClick = {
                                diagnosisOutcome = DiagnosisOutcome.REPAIRABLE
                            },
                        )
                        QuoteDecisionRow(
                            text = stringResource(R.string.order_diagnosis_not_repairable),
                            checked = diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE,
                            testTag = RepairAppTestTags.DiagnosisNotRepairableToggle,
                            onClick = {
                                diagnosisOutcome = DiagnosisOutcome.NOT_REPAIRABLE
                                resultAmount = ""
                            },
                        )
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
                        if (closureReason == ClosureReason.OTHER) {
                            Field(
                                value = statusNote,
                                onValueChange = { statusNote = it },
                                label = stringResource(R.string.order_closure_note_label),
                                required = true,
                                showError = statusNote.isBlank(),
                                imeAction = ImeAction.Done,
                                testTag = RepairAppTestTags.StatusNoteField,
                            )
                        }
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
                        isClosure -> closureReason != null && (closureReason != ClosureReason.OTHER || statusNote.isNotBlank())
                        else -> true
                    },
                    onClick = {
                        applyStatusAction(
                            action = action,
                            orderId = selected.order.id,
                            viewModel = viewModel,
                            note = statusNote,
                            closureReason = closureReason,
                            resultSummary = resultSummary,
                            resultAmount = resultAmount,
                            diagnosisOutcome = diagnosisOutcome,
                        )
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
                        viewModel.addNote(selected.order.id, note)
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

    if (uiState.showPdfSentConfirmation) {
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

@Composable
private fun QuoteDecisionRow(
    text: String,
    checked: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(vertical = DashboardSpacing.compactGap / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun toggleQuoteDecision(current: QuoteDecision?, new: QuoteDecision): QuoteDecision? =
    if (current == new) null else new

private fun canConfirmQuoteDecision(decision: QuoteDecision?, rejectNote: String): Boolean =
    when (decision) {
        QuoteDecision.ACCEPTED -> true
        QuoteDecision.REJECTED -> rejectNote.isNotBlank()
        null -> false
    }
