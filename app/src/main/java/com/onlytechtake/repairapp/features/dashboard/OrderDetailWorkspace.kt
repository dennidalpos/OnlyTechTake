package com.onlytechtake.repairapp.features.dashboard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.features.workspace.OrderStatusAction
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.attachmentCounts
import com.onlytechtake.repairapp.features.workspace.availableOrderStatusActions
import com.onlytechtake.repairapp.features.workspace.formatUiDateTime
import com.onlytechtake.repairapp.features.workspace.label
import com.onlytechtake.repairapp.features.workspace.nextActionLabel
import com.onlytechtake.repairapp.features.workspace.requiresConfirmation
import com.onlytechtake.repairapp.features.workspace.statusTemplateCode

@Composable
fun OrderDetailWorkspace(viewModel: WorkspaceViewModel) {
    val detail by viewModel.selectedOrder.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val context = LocalContext.current
    var note by rememberSaveable { mutableStateOf("") }
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var statusAction by remember { mutableStateOf<OrderStatusAction?>(null) }
    var statusNote by rememberSaveable { mutableStateOf("") }
    var closureReason by rememberSaveable { mutableStateOf<ClosureReason?>(null) }
    var resultSummary by rememberSaveable { mutableStateOf("") }
    var resultAmount by rememberSaveable { mutableStateOf("") }
    var quoteDecision by rememberSaveable { mutableStateOf<QuoteDecision?>(null) }
    var quoteRejectNote by rememberSaveable { mutableStateOf("") }

    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let(viewModel::exportSelectedOrderPdf)
    }

    val selected = detail
    if (selected == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(DashboardSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
        ) {
            Text("Nessuna commessa selezionata")
            TouchButton(text = "Commesse", onClick = { viewModel.navigateTo(WorkspaceDestination.ORDERS) })
        }
        return
    }

    LaunchedEffect(selected.order.id, selected.order.status) {
        quoteDecision = null
        quoteRejectNote = ""
    }

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    fun exitOrderDetail() {
        val hasUnsavedLocalWork = note.isNotBlank() || uiState.preparedMessage != null
        if (hasUnsavedLocalWork) {
            requestConfirm("Uscire dalla scheda?", "Nota non salvata o anteprima WhatsApp verranno scartate.") {
                note = ""
                viewModel.selectOrder(null)
            }
        } else {
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    ActionRow {
                        TouchButton(text = "Esci", tone = ButtonTone.NEUTRAL, onClick = ::exitOrderDetail)
                        TouchButton(
                            text = "PDF",
                            tone = ButtonTone.INFO,
                            onClick = {
                                requestConfirm("Esportare PDF?", "Creare il riepilogo commessa tramite selettore di sistema?") {
                                    exportPdfLauncher.launch("${selected.order.orderNumber}.pdf")
                                }
                            },
                        )
                    }
                    SectionTitle("Scheda commessa")
                    Text("${selected.order.orderNumber} - ${selected.order.title}", style = MaterialTheme.typography.titleMedium)
                    Text("Cliente: ${selected.customer?.displayName().orEmpty()}")
                    OperatorText(text = "Stato corrente: ${selected.order.status.label()}")
                    OperatorText(text = "Prossima azione: ${nextActionLabel(selected.order)}")
                    selected.order.closureReason?.let { Text("Motivo chiusura: ${it.label()}") }
                    Text("Operatore: ${selected.assignedUser?.displayName.orEmpty()}")
                    Text("Aggiornata: ${formatUiDateTime(selected.order.updatedAt)}")
                    Text(attachmentCounts(selected.media, selected.signatures).label())
                    selected.order.description?.takeIf { it.isNotBlank() }?.let {
                        Text("Problema: $it")
                    }
                }
            }
        }

        if (selected.order.status == OrderStatus.RESULT) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Text("Esito preventivo", style = MaterialTheme.typography.titleMedium)
                        QuoteDecisionRow(
                            text = "Preventivo accettato",
                            checked = quoteDecision == QuoteDecision.ACCEPTED,
                            testTag = RepairAppTestTags.QuoteAcceptedToggle,
                            onClick = { quoteDecision = toggleQuoteDecision(quoteDecision, QuoteDecision.ACCEPTED) },
                        )
                        QuoteDecisionRow(
                            text = "Preventivo rifiutato",
                            checked = quoteDecision == QuoteDecision.REJECTED,
                            testTag = RepairAppTestTags.QuoteRejectedToggle,
                            onClick = { quoteDecision = toggleQuoteDecision(quoteDecision, QuoteDecision.REJECTED) },
                        )
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
                                            onSuccess = { viewModel.selectOrder(null) },
                                        )
                                    }
                                    null -> Unit
                                }
                            },
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text("Azioni", style = MaterialTheme.typography.titleMedium)
                    val actions = availableOrderStatusActions(selected.order.status)
                        .filterNot { selected.order.status == OrderStatus.RESULT && it == OrderStatusAction.START_REPAIR }
                    if (actions.isEmpty()) {
                        Text("Nessuna azione disponibile per questo stato.")
                    } else {
                        ActionRow {
                            actions.forEach { action ->
                                TouchButton(
                                    text = action.label,
                                    tone = action.tone(),
                                    testTag = RepairAppTestTags.statusAction(action.name),
                                    onClick = {
                                        statusAction = action
                                        statusNote = ""
                                        closureReason = null
                                        resultSummary = ""
                                        resultAmount = ""
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text("Note interne", style = MaterialTheme.typography.titleMedium)
                    selected.order.internalNotes?.takeIf { it.isNotBlank() }?.let { OperatorText(it) }
                    Field(note, { note = it }, "Nuova nota")
                    TouchButton(
                        text = "Salva nota",
                        tone = ButtonTone.SUCCESS,
                        enabled = note.isNotBlank(),
                        onClick = {
                            val noteToSave = note
                            viewModel.addNote(selected.order.id, noteToSave)
                            note = ""
                        },
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
            val templateCode = statusTemplateCode(selected.order)
            val templateTitle = templates.firstOrNull { it.active && it.code == templateCode }?.title ?: templateCode
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    SectionTitle("WhatsApp")
                    OperatorText(
                        text = "Stato attuale: ${selected.order.status.label()}\nTemplate: $templateTitle",
                    )
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Invia stato WhatsApp",
                        tone = ButtonTone.INFO,
                        onClick = { viewModel.prepareMessage(selected.order.id, templateCode) },
                    )
                }
            }
        }

        uiState.preparedMessage?.let { prepared ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                        SectionTitle("Anteprima WhatsApp")
                        Field(uiState.preparedMessageBody, viewModel::updatePreparedMessageBody, "Messaggio")
                        ActionRow {
                            TouchButton(
                                text = "Apri WhatsApp",
                                tone = ButtonTone.INFO,
                                onClick = {
                                    requestConfirm("Aprire WhatsApp?", "Registrare la preparazione e aprire WhatsApp per l'invio manuale?") {
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
                                text = "Inviato",
                                tone = ButtonTone.SUCCESS,
                                onClick = { requestConfirm("Invio confermato?", "Registrare che l'operatore ha inviato manualmente il messaggio al cliente?") { viewModel.confirmSentMessage() } },
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
                SectionTitle("Timeline")
                selected.timeline.forEach { event ->
                    CompactExpandableRow(
                        title = event.title,
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
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text("Annulla")
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aggiornare lo stato della commessa?")
                    if (isResultStep) {
                        Field(resultSummary, { resultSummary = it }, "Sintesi risultato", required = true, showError = resultSummary.isBlank(), imeAction = ImeAction.Next, testTag = RepairAppTestTags.StatusResultSummaryField)
                        Field(resultAmount, { resultAmount = it }, "Importo riparazione opzionale", singleLine = true, imeAction = ImeAction.Done, keyboardType = KeyboardType.Number)
                    } else {
                        Field(
                            statusNote,
                            { statusNote = it },
                            if (isClosure) "Nota chiusura" else "Nota opzionale",
                            required = isClosure,
                            showError = isClosure && statusNote.isBlank(),
                            imeAction = ImeAction.Done,
                            testTag = RepairAppTestTags.StatusNoteField,
                        )
                    }
                    if (isClosure) {
                        Text("Motivo chiusura", style = MaterialTheme.typography.titleSmall)
                        ActionRow {
                            ClosureReason.entries
                                .forEach { reason ->
                                val selectedReason = closureReason == reason
                                TouchButton(
                                    text = reason.label(),
                                    tone = if (selectedReason) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                                    selected = selectedReason,
                                    stateDescription = if (selectedReason) "Motivo selezionato" else "Motivo non selezionato",
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
                        isResultStep -> resultSummary.isNotBlank()
                        isClosure -> closureReason != null && statusNote.isNotBlank()
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
                        )
                        statusAction = null
                    },
                    modifier = Modifier.testTag(RepairAppTestTags.StatusDialogConfirmButton),
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { statusAction = null }) {
                    Text("Annulla")
                }
            },
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Text(text)
    }
}

private fun applyStatusAction(
    action: OrderStatusAction,
    orderId: String,
    viewModel: WorkspaceViewModel,
    note: String,
    closureReason: ClosureReason?,
    resultSummary: String,
    resultAmount: String,
) {
    if (action == OrderStatusAction.RECORD_RESULT) {
        val estimateAmountCents = parseEuroCentsOrNull(resultAmount)
        viewModel.recordResult(
            orderId = orderId,
            summary = resultSummary,
            estimateAmountCents = estimateAmountCents,
            onSuccess = {
                if (estimateAmountCents != null) {
                    viewModel.prepareMessage(orderId, "whatsapp_preventivo_intervento")
                }
            },
        )
        return
    }
    val targetStatus = action.targetStatus ?: return
    viewModel.changeStatus(
        orderId = orderId,
        status = targetStatus,
        note = note,
        closureReason = closureReason,
        onSuccess = {
            if (targetStatus == OrderStatus.CLOSED && closureReason == ClosureReason.NOT_REPAIRABLE) {
                viewModel.prepareMessage(orderId, "whatsapp_non_riparabile")
            } else if (targetStatus == OrderStatus.CLOSED) {
                viewModel.selectOrder(null)
            }
        },
    )
}

private fun OrderStatusAction.tone(): ButtonTone =
    when (this) {
        OrderStatusAction.CLOSE_ORDER -> ButtonTone.DANGER
        OrderStatusAction.READY_FOR_PICKUP -> ButtonTone.SUCCESS
        OrderStatusAction.RECORD_RESULT -> ButtonTone.INFO
        else -> ButtonTone.PRIMARY
    }

private fun parseEuroCentsOrNull(value: String): Long? {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isBlank()) return null
    val parts = normalized.split('.', limit = 2)
    val euros = parts.getOrNull(0)?.filter(Char::isDigit)?.toLongOrNull() ?: return null
    val cents = parts.getOrNull(1)
        ?.filter(Char::isDigit)
        ?.take(2)
        ?.padEnd(2, '0')
        ?.toLongOrNull()
        ?: 0L
    return euros * 100L + cents
}
