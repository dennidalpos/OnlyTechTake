package com.onlytechtake.repairapp.features.dashboard.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.onlytechtake.repairapp.domain.models.ClosureReason
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision

class OrderDetailWorkspaceState(
    initialNote: String = "",
    initialShowInternalNoteDialog: Boolean = false,
    initialStatusNote: String = "",
    initialResultSummary: String = "",
    initialResultAmount: String = "",
    initialQuoteRejectNote: String = "",
) {
    var note by mutableStateOf(initialNote)
    var showInternalNoteDialog by mutableStateOf(initialShowInternalNoteDialog)

    var confirmTitle by mutableStateOf<String?>(null)
    var confirmText by mutableStateOf("")
    var confirmAction by mutableStateOf<(() -> Unit)?>(null)

    var statusAction by mutableStateOf<OrderStatusAction?>(null)
    var statusNote by mutableStateOf(initialStatusNote)
    var closureReason by mutableStateOf<ClosureReason?>(null)
    var resultSummary by mutableStateOf(initialResultSummary)
    var resultAmount by mutableStateOf(initialResultAmount)
    var diagnosisOutcome by mutableStateOf<DiagnosisOutcome?>(null)

    var quoteDecision by mutableStateOf<QuoteDecision?>(null)
    var quoteRejectNote by mutableStateOf(initialQuoteRejectNote)

    fun requestConfirm(
        title: String,
        text: String,
        action: () -> Unit,
    ) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    fun dismissConfirm() {
        confirmTitle = null
        confirmAction = null
    }

    fun openStatusAction(
        action: OrderStatusAction,
        currentOutcome: DiagnosisOutcome?,
    ) {
        statusAction = action
        statusNote = ""
        closureReason =
            if (action == OrderStatusAction.CLOSE_ORDER) ClosureReason.COMPLETED else null
        resultSummary = ""
        resultAmount = ""
        diagnosisOutcome = currentOutcome
    }

    fun dismissStatusAction() {
        statusAction = null
    }

    fun openInternalNote() {
        note = ""
        showInternalNoteDialog = true
    }

    fun dismissInternalNote() {
        showInternalNoteDialog = false
    }

    fun resetQuoteDecision() {
        quoteDecision = null
        quoteRejectNote = ""
    }

    companion object {
        val Saver: Saver<OrderDetailWorkspaceState, *> =
            listSaver(
                save = {
                    listOf(
                        it.note,
                        it.showInternalNoteDialog,
                        it.statusNote,
                        it.resultSummary,
                        it.resultAmount,
                        it.quoteRejectNote,
                        it.diagnosisOutcome?.name,
                        it.quoteDecision?.name,
                        it.closureReason?.name,
                    )
                },
            ) { list ->
                OrderDetailWorkspaceState(
                    initialNote = list[0] as String,
                    initialShowInternalNoteDialog = list[1] as Boolean,
                    initialStatusNote = list[2] as String,
                    initialResultSummary = list[3] as String,
                    initialResultAmount = list[4] as String,
                    initialQuoteRejectNote = list[5] as String,
                ).apply {
                    diagnosisOutcome =
                        (list[6] as? String)?.let { name -> DiagnosisOutcome.valueOf(name) }
                    quoteDecision =
                        (list[7] as? String)?.let { name -> QuoteDecision.valueOf(name) }
                    closureReason =
                        (list[8] as? String)?.let { name -> ClosureReason.valueOf(name) }
                }
            }
    }
}

@Composable
fun rememberOrderDetailWorkspaceState(): OrderDetailWorkspaceState =
    rememberSaveable(saver = OrderDetailWorkspaceState.Saver) {
        OrderDetailWorkspaceState()
    }
