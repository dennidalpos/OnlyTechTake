package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.core.utils.parseEuroCentsOrNull
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.canConfirmDiagnosisResult
import com.onlytechtake.repairapp.domain.workspace.requiresConfirmation
import com.onlytechtake.repairapp.domain.workspace.shouldShowDiagnosisEstimate
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText

@Composable
fun OrderConfirmDialog(
    state: OrderDetailWorkspaceState,
) {
    state.confirmTitle?.let { title ->
        AppDialog(
            onDismissRequest = { state.dismissConfirm() },
            title = title,
            text = state.confirmText,
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        state.confirmAction?.invoke()
                        state.dismissConfirm()
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.GenericConfirmButton,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = { state.dismissConfirm() },
                )
            },
        )
    }
}

@Composable
fun OrderStatusActionDialog(
    state: OrderDetailWorkspaceState,
    context: Context,
    onRecordResult: (summary: String, outcome: DiagnosisOutcome, estimateAmountCents: Long?) -> Unit,
    onChangeStatus: (status: OrderStatus, note: String, closureReason: ClosureReason?) -> Unit,
) {
    state.statusAction?.let { action ->
        val isResultStep = action == OrderStatusAction.RECORD_RESULT
        val isClosure = action.requiresConfirmation()
        AppDialog(
            onDismissRequest = { state.dismissStatusAction() },
            title = action.label,
            content = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(stringResource(R.string.order_confirm_status_update))
                    if (isResultStep) {
                        AppTextField(
                            value = state.resultSummary,
                            onValueChange = { state.resultSummary = it },
                            label = stringResource(R.string.order_result_summary),
                            required = true,
                            showError = state.resultSummary.isBlank(),
                            imeAction = ImeAction.Next,
                            testTag = RepairAppTestTags.StatusResultSummaryField,
                        )
                        Text(
                            stringResource(R.string.order_not_repairable_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        QuoteDecisionRow(
                            text = stringResource(R.string.order_diagnosis_repairable),
                            checked = state.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE,
                            testTag = RepairAppTestTags.DiagnosisRepairableToggle,
                        ) {
                            state.diagnosisOutcome = DiagnosisOutcome.REPAIRABLE
                        }
                        QuoteDecisionRow(
                            text = stringResource(R.string.order_diagnosis_not_repairable),
                            checked = state.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE,
                            testTag = RepairAppTestTags.DiagnosisNotRepairableToggle,
                        ) {
                            state.diagnosisOutcome = DiagnosisOutcome.NOT_REPAIRABLE
                            state.resultAmount = ""
                        }
                        if (shouldShowDiagnosisEstimate(state.diagnosisOutcome)) {
                            AppTextField(
                                value = state.resultAmount,
                                onValueChange = { state.resultAmount = it },
                                label = stringResource(R.string.order_estimate_amount),
                                required = true,
                                showError = parseEuroCentsOrNull(state.resultAmount)?.let { it <= 0L } != false,
                                singleLine = true,
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Decimal,
                                testTag = RepairAppTestTags.DiagnosisEstimateField,
                            )
                        }
                    } else if (isClosure) {
                        AppTextField(
                            value = state.statusNote,
                            onValueChange = { state.statusNote = it },
                            label = stringResource(R.string.order_closure_note_label),
                            required = state.closureReason == ClosureReason.OTHER,
                            showError = (state.closureReason == ClosureReason.OTHER) && state.statusNote.isBlank(),
                            imeAction = ImeAction.Done,
                            testTag = RepairAppTestTags.StatusNoteField,
                        )
                    }
                    if (isClosure) {
                        Text(
                            stringResource(R.string.order_closure_reason_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        ActionRow {
                            ClosureReason.entries.forEach { reason ->
                                val selectedReason = state.closureReason == reason
                                AppButton(
                                    text = reason.label(context),
                                    variant = if (selectedReason) AppButtonVariant.Success else AppButtonVariant.Secondary,
                                    selected = selectedReason,
                                    stateDescription =
                                        if (selectedReason) {
                                            stringResource(R.string.state_selected)
                                        } else {
                                            stringResource(
                                                R.string.state_not_selected,
                                            )
                                        },
                                    testTag = RepairAppTestTags.closureReason(reason.name),
                                    onClick = { state.closureReason = reason },
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
                    enabled =
                        when {
                            isResultStep ->
                                canConfirmDiagnosisResult(
                                    summary = state.resultSummary,
                                    outcome = state.diagnosisOutcome,
                                    estimateAmountCents = parseEuroCentsOrNull(state.resultAmount),
                                )

                            isClosure -> (state.closureReason != null) && ((state.closureReason != ClosureReason.OTHER) || state.statusNote.isNotBlank())
                            else -> true
                        },
                    onClick = {
                        if (action == OrderStatusAction.RECORD_RESULT) {
                            val outcome = state.diagnosisOutcome ?: return@AppButton
                            val estimateAmountCents = parseEuroCentsOrNull(state.resultAmount)
                            onRecordResult(state.resultSummary, outcome, estimateAmountCents)
                        } else {
                            val targetStatus = action.targetStatus ?: return@AppButton
                            onChangeStatus(targetStatus, state.statusNote, state.closureReason)
                        }
                        state.dismissStatusAction()
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.StatusDialogConfirmButton,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = { state.dismissStatusAction() },
                )
            },
        )
    }
}

@Composable
fun InternalNoteDialog(
    state: OrderDetailWorkspaceState,
    currentInternalNotes: String?,
    onAddNote: (String) -> Unit,
) {
    if (state.showInternalNoteDialog) {
        AppDialog(
            onDismissRequest = { state.dismissInternalNote() },
            title = stringResource(R.string.order_internal_notes),
            content = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    currentInternalNotes?.takeIf { it.isNotBlank() }?.let {
                        OperatorText(it)
                    }
                    AppTextField(
                        value = state.note,
                        onValueChange = { state.note = it },
                        label = stringResource(R.string.order_new_note),
                        imeAction = ImeAction.Done,
                    )
                }
            },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.order_save_note),
                    enabled = state.note.isNotBlank(),
                    onClick = {
                        onAddNote(state.note)
                        state.note = ""
                        state.dismissInternalNote()
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.GenericConfirmButton,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = { state.dismissInternalNote() },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

@Composable
fun OrderPdfSentConfirmationDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (show) {
        AppDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.order_pdf_sent_confirm_title),
            text = stringResource(R.string.order_pdf_sent_confirm_text),
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.yes_confirmed),
                    onClick = onConfirm,
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.GenericConfirmButton,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.no_cancel),
                    onClick = onDismiss,
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

@Composable
fun WhatsAppPreviewDialog(
    prepared: com.onlytechtake.repairapp.domain.workspace.PreparedMessage?,
    body: String,
    onBodyChange: (String) -> Unit,
    onOpenWhatsApp: () -> Unit,
    onConfirmSent: () -> Unit,
    onClose: () -> Unit,
) {
    if (prepared != null) {
        AppDialog(
            onDismissRequest = onClose,
            title = stringResource(R.string.order_wa_preview_title),
            content = {
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .testTag(RepairAppTestTags.OrderWhatsAppPreviewPanel),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    Text(
                        text = "Template: ${prepared.templateName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )

                    com.onlytechtake.repairapp.core.design.components.AppMessageBanner(
                        message = stringResource(R.string.order_wa_sending_to, prepared.phone),
                        type = com.onlytechtake.repairapp.core.design.components.BannerType.Info,
                    )

                    AppTextField(
                        value = body,
                        onValueChange = onBodyChange,
                        label = stringResource(R.string.order_wa_message_label),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                    AppButton(
                        text = stringResource(R.string.order_wa_close_preview),
                        variant = AppButtonVariant.Ghost,
                        onClick = onClose,
                        size = AppButtonSize.Compact,
                        testTag = RepairAppTestTags.OrderWhatsAppClosePreviewButton,
                    )
                    AppButton(
                        text = stringResource(R.string.order_wa_sent),
                        variant = AppButtonVariant.Success,
                        onClick = onConfirmSent,
                        size = AppButtonSize.Compact,
                        testTag = RepairAppTestTags.OrderWhatsAppSentButton,
                    )
                }
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.order_wa_open),
                    variant = AppButtonVariant.WhatsApp,
                    onClick = onOpenWhatsApp,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.OrderWhatsAppOpenButton,
                )
            },
        )
    }
}
