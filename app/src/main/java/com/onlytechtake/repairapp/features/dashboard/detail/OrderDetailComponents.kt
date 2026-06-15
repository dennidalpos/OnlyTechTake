package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import com.onlytechtake.repairapp.domain.workspace.availableOrderStatusActions
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.OrderStatusStepper
import com.onlytechtake.repairapp.features.workspace.attachmentCounts
import com.onlytechtake.repairapp.features.workspace.label as attachmentLabel
import com.onlytechtake.repairapp.features.workspace.tone

@Composable
fun OrderHeader(
    detail: OrderDetail,
    context: Context,
    modifier: Modifier = Modifier,
) {
    StandardCard(highlight = true, modifier = modifier) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            OrderStatusStepper(detail.order.status)
            Text(
                text = detail.order.status.label(context),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(RepairAppTestTags.OrderStatusLabel),
            )

            if (detail.isPaid) {
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
                text = "${detail.order.orderNumber} - ${detail.order.title}",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = stringResource(
                    R.string.orders_customer_label,
                    detail.customer?.displayName(context).orEmpty()
                ),
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Operatore: ${detail.assignedUser?.displayName.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatUiDateTime(detail.order.updatedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (detail.order.description?.isNotBlank() == true) {
                OperatorText(
                    text = stringResource(R.string.order_problem_label, detail.order.description),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = attachmentCounts(detail.media, detail.signatures).attachmentLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                detail.order.diagnosisOutcome?.let {
                    Text(
                        text = "• ${it.label(context)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            detail.order.closureReason?.let {
                Text(
                    text = stringResource(R.string.closure_reason_prefix, it.label(context)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun OrderActionCard(
    status: OrderStatus,
    isPaid: Boolean,
    onActionClick: (OrderStatusAction) -> Unit,
    onTogglePayment: (Boolean) -> Unit,
    onSendWhatsApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            SectionTitle("Azioni Commessa")
            
            ActionRow {
                val actions = availableOrderStatusActions(status)
                val templateCode = statusTemplateCode(status)

                actions.forEach { action ->
                    TouchButton(
                        text = action.label,
                        tone = action.tone(),
                        testTag = RepairAppTestTags.statusAction(action.name),
                        onClick = { onActionClick(action) }
                    )
                }

                if (status == OrderStatus.READY_FOR_PICKUP || status == OrderStatus.CLOSED) {
                    TouchButton(
                        text = if (isPaid) stringResource(R.string.order_not_paid_button) else stringResource(R.string.order_paid_button),
                        tone = if (isPaid) ButtonTone.WARNING else ButtonTone.SUCCESS,
                        onClick = { onTogglePayment(!isPaid) }
                    )
                }

                TouchButton(
                    text = stringResource(R.string.order_whatsapp_send),
                    tone = ButtonTone.BRAND_WHATSAPP,
                    onClick = { onSendWhatsApp(templateCode) }
                )
            }
        }
    }
}

@Composable
fun QuoteDecisionPanel(
    quoteDecision: QuoteDecision?,
    quoteRejectNote: String,
    onQuoteDecisionChange: (QuoteDecision?) -> Unit,
    onQuoteRejectNoteChange: (String) -> Unit,
    onConfirm: (CustomerFeedbackType, String, ClosureReason?) -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardCard(modifier = modifier) {
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
                onQuoteDecisionChange(toggleQuoteDecision(quoteDecision, QuoteDecision.ACCEPTED))
            }
            QuoteDecisionRow(
                text = "Preventivo rifiutato",
                checked = quoteDecision == QuoteDecision.REJECTED,
                testTag = RepairAppTestTags.QuoteRejectedToggle,
            ) {
                onQuoteDecisionChange(toggleQuoteDecision(quoteDecision, QuoteDecision.REJECTED))
            }
            if (quoteDecision == QuoteDecision.REJECTED) {
                Field(
                    value = quoteRejectNote,
                    onValueChange = onQuoteRejectNoteChange,
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
                            onConfirm(CustomerFeedbackType.ACCEPT, "Preventivo accettato", null)
                        }
                        QuoteDecision.REJECTED -> {
                            onConfirm(CustomerFeedbackType.REJECT, quoteRejectNote, ClosureReason.CUSTOMER_DECLINED)
                        }
                        null -> Unit
                    }
                },
            )
        }
    }
}

@Composable
fun WhatsAppPreviewPanel(
    prepared: PreparedMessage,
    body: String,
    onBodyChange: (String) -> Unit,
    onOpenWhatsApp: () -> Unit,
    onConfirmSent: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            SectionTitle(stringResource(R.string.order_wa_preview_title))
            Field(body, onBodyChange, stringResource(R.string.order_wa_message_label))
            ActionRow {
                TouchButton(
                    text = stringResource(R.string.order_wa_open),
                    tone = ButtonTone.BRAND_WHATSAPP,
                    onClick = onOpenWhatsApp
                )
                TouchButton(
                    text = stringResource(R.string.order_wa_sent),
                    tone = ButtonTone.SUCCESS,
                    onClick = onConfirmSent
                )
                TouchButton(
                    text = stringResource(R.string.order_wa_close_preview),
                    tone = ButtonTone.NEUTRAL,
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
fun InternalNotesPanel(
    notes: String?,
    onAddNoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            Text(stringResource(R.string.order_internal_notes), style = MaterialTheme.typography.titleMedium)
            notes?.takeIf { it.isNotBlank() }?.let {
                OperatorText(it)
            }
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.order_internal_notes),
                tone = ButtonTone.NEUTRAL,
                onClick = onAddNoteClick
            )
        }
    }
}

@Composable
fun QuoteDecisionRow(
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

private fun statusTemplateCode(status: OrderStatus): String =
    when (status) {
        OrderStatus.OPEN -> "whatsapp_accettazione"
        OrderStatus.DIAGNOSIS -> "whatsapp_in_diagnosi"
        OrderStatus.RESULT -> "whatsapp_preventivo_intervento"
        OrderStatus.REPAIRING -> "whatsapp_in_riparazione"
        OrderStatus.READY_FOR_PICKUP -> "whatsapp_pronto_ritiro"
        OrderStatus.CLOSED -> "whatsapp_consegnato"
        else -> "whatsapp_generic"
    }
