package com.onlytechtake.repairapp.features.dashboard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.features.workspace.OrderStatusAction
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.availableOrderStatusActions
import com.onlytechtake.repairapp.features.workspace.label
import com.onlytechtake.repairapp.features.workspace.requiresConfirmation

@Composable
fun OrderDetailWorkspace(viewModel: WorkspaceViewModel) {
    val detail by viewModel.selectedOrder.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var note by rememberSaveable { mutableStateOf("") }
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let(viewModel::exportSelectedOrderPdf)
    }

    val selected = detail
    if (selected == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nessuna commessa selezionata")
            TouchButton(text = "Commesse", onClick = { viewModel.navigateTo(WorkspaceDestination.ORDERS) })
        }
        return
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    SectionTitle("${selected.order.orderNumber} - ${selected.order.title}")
                    Text("Cliente: ${selected.customer?.displayName().orEmpty()}")
                    Text("Stato: ${selected.order.status.label()}", style = MaterialTheme.typography.titleMedium)
                    Text("Operatore: ${selected.assignedUser?.displayName.orEmpty()}")
                    selected.order.description?.takeIf { it.isNotBlank() }?.let {
                        Text("Problema: $it")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Azioni operative", style = MaterialTheme.typography.titleMedium)
                    val actions = availableOrderStatusActions(selected.order.status)
                    if (actions.isEmpty()) {
                        Text("Nessuna azione disponibile per questo stato.")
                    } else {
                        ActionRow {
                            actions.forEach { action ->
                                TouchButton(
                                    text = action.label,
                                    tone = action.tone(),
                                    onClick = {
                                        if (action.requiresConfirmation()) {
                                            requestConfirm(action.label, "Aggiornare lo stato della commessa?") {
                                                applyStatusAction(action, selected.order.id, viewModel)
                                            }
                                        } else {
                                            applyStatusAction(action, selected.order.id, viewModel)
                                        }
                                    },
                                )
                            }
                            TouchButton(
                                text = "WhatsApp",
                                tone = ButtonTone.INFO,
                                onClick = { viewModel.prepareStatusMessage(selected.order.id, selected.order.status) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Note interne", style = MaterialTheme.typography.titleMedium)
                    selected.order.internalNotes?.takeIf { it.isNotBlank() }?.let { Text(it) }
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OrderPhotoSection(selected, viewModel)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OrderSignatureSection(selected, viewModel)
                }
            }
        }

        uiState.preparedMessage?.let { prepared ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("Anteprima WhatsApp")
                        Field(uiState.preparedMessageBody, viewModel::updatePreparedMessageBody, "Messaggio")
                        ActionRow {
                            Button(
                                onClick = {
                                    requestConfirm("Aprire WhatsApp?", "Registrare e aprire il messaggio?") {
                                        viewModel.recordPreparedMessage()
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://wa.me/${prepared.phone}?text=${Uri.encode(uiState.preparedMessageBody)}".toUri(),
                                        )
                                        context.startActivity(intent)
                                    }
                                },
                            ) {
                                Text("Apri WhatsApp")
                            }
                            Button(onClick = { requestConfirm("Invio confermato?", "Registrare l'invio manuale?") { viewModel.confirmSentMessage() } }) {
                                Text("Inviato")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Timeline")
                    selected.timeline.forEach { event ->
                        Text("${event.title}: ${event.description.orEmpty()}")
                    }
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
}

private fun applyStatusAction(
    action: OrderStatusAction,
    orderId: String,
    viewModel: WorkspaceViewModel,
) {
    when (action) {
        OrderStatusAction.ACCEPT_ESTIMATE -> viewModel.recordFeedback(orderId, CustomerFeedbackType.ACCEPT, "Preventivo accettato")
        OrderStatusAction.REJECT_ESTIMATE -> viewModel.recordFeedback(orderId, CustomerFeedbackType.REJECT, "Preventivo rifiutato")
        else -> action.targetStatus?.let { viewModel.changeStatus(orderId, it) }
    }
}

private fun OrderStatusAction.tone(): ButtonTone =
    when (this) {
        OrderStatusAction.REJECT_ESTIMATE -> ButtonTone.WARNING
        OrderStatusAction.CANCEL -> ButtonTone.DANGER
        OrderStatusAction.DELIVER -> ButtonTone.SUCCESS
        OrderStatusAction.CLOSE -> ButtonTone.SUCCESS
        OrderStatusAction.READY_FOR_PICKUP -> ButtonTone.SUCCESS
        else -> ButtonTone.PRIMARY
    }
