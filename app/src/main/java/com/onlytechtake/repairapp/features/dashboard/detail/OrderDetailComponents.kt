package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.repairColors
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import com.onlytechtake.repairapp.domain.workspace.availableOrderStatusActions
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.CompactExpandableRow

@Composable
fun OrderHeader(detail: OrderDetail, context: Context) {
    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = detail.order.orderNumber,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        AppStatusChip(
                            text = detail.order.status.label(context),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = detail.order.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = detail.customer?.displayName(context) ?: stringResource(R.string.orders_customer_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = detail.order.categoryPathSnapshot ?: detail.category?.name ?: stringResource(R.string.orders_category_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (detail.isPaid) {
                    AppBadge(
                        text = stringResource(R.string.order_paid_badge),
                        containerColor = MaterialTheme.repairColors.successContainer,
                        contentColor = MaterialTheme.repairColors.onSuccessContainer,
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp).alpha(0.5f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoMetadata(label = "Aggiornata", value = formatUiDateTime(detail.order.updatedAt))
                detail.order.assignedToUserId?.let { InfoMetadata(label = "Operatore", value = it) }
            }
        }
    }
}

@Composable
private fun InfoMetadata(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OrderActionCard(
    status: OrderStatus,
    isPaid: Boolean,
    whatsAppAnchor: TemplateAnchor,
    onActionClick: (OrderStatusAction) -> Unit,
    onTogglePayment: () -> Unit,
    onSendWhatsApp: (TemplateAnchor) -> Unit,
) {
    AppActionPanel(title = stringResource(R.string.order_actions_title)) {
        Text(stringResource(R.string.order_actions_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        ActionRow {
            availableOrderStatusActions(status).forEach { action ->
                AppButton(
                    text = action.label,
                    onClick = { onActionClick(action) },
                    variant = if (action == OrderStatusAction.RECORD_RESULT) AppButtonVariant.Primary else AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.alpha(0.3f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Checkbox, onClick = onTogglePayment),
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isPaid, onCheckedChange = null)
            Text(stringResource(R.string.order_paid_button), style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider(modifier = Modifier.alpha(0.3f))

        AppButton(
            text = stringResource(R.string.order_whatsapp_send),
            variant = AppButtonVariant.WhatsApp,
            onClick = { onSendWhatsApp(whatsAppAnchor) },
            size = AppButtonSize.Compact,
            leadingIcon = Icons.AutoMirrored.Filled.Message,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AppActionPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    StandardCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)
        ) {
            content()
        }
    }
}

@Composable
fun QuoteDecisionPanel(
    quoteDecision: QuoteDecision?,
    quoteRejectNote: String,
    onQuoteDecisionChange: (QuoteDecision) -> Unit,
    onQuoteRejectNoteChange: (String) -> Unit,
    onConfirm: (CustomerFeedbackType, String, ClosureReason?) -> Unit,
) {
    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Text(stringResource(R.string.order_quote_outcome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Row(horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap)) {
                QuoteDecisionButton(
                    text = stringResource(R.string.order_quote_accepted),
                    selected = quoteDecision == QuoteDecision.ACCEPTED,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuoteDecisionChange(QuoteDecision.ACCEPTED) }
                )

                QuoteDecisionButton(
                    text = stringResource(R.string.order_quote_rejected),
                    selected = quoteDecision == QuoteDecision.REJECTED,
                    modifier = Modifier.weight(1f),
                    onClick = { onQuoteDecisionChange(QuoteDecision.REJECTED) }
                )
            }

            if (quoteDecision == QuoteDecision.REJECTED) {
                AppTextField(
                    value = quoteRejectNote,
                    onValueChange = onQuoteRejectNoteChange,
                    label = stringResource(R.string.order_quote_reject_reason),
                    showOptional = true,
                )
            }

            if (quoteDecision != null) {
                AppButton(
                    text = stringResource(R.string.confirm),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (quoteDecision == QuoteDecision.ACCEPTED) {
                            onConfirm(CustomerFeedbackType.ACCEPT, "Preventivo accettato", null)
                        } else {
                            onConfirm(CustomerFeedbackType.REJECT, quoteRejectNote, ClosureReason.CUSTOMER_DECLINED)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun QuoteDecisionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppButton(
        text = text,
        onClick = onClick,
        variant = if (selected) AppButtonVariant.Primary else AppButtonVariant.Ghost,
        size = AppButtonSize.Compact,
        modifier = modifier,
        selected = selected
    )
}

@Composable
fun QuoteDecisionRow(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onSelect: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
            .selectable(selected = checked, onClick = onSelect, role = Role.RadioButton)
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                RepairAppShapes.compactButton,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = checked, onClick = null)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun InternalNotesPanel(
    notes: String?,
    onAddNoteClick: () -> Unit,
) {
    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            Text(stringResource(R.string.order_internal_notes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            notes?.takeIf { it.isNotBlank() }?.let {
                OperatorText(it)
            }
            AppButton(
                text = stringResource(R.string.order_save_note),
                variant = AppButtonVariant.Ghost,
                onClick = onAddNoteClick,
                size = AppButtonSize.Compact,
                modifier = Modifier.align(Alignment.End)
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
) {
    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.order_wa_preview_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                AppIconButton(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.order_wa_close_preview), onClick = onClose)
            }
            
            AppMessageBanner(
                message = "Inviando a: ${prepared.phone}",
                type = BannerType.Info
            )
            
            AppTextField(
                value = body,
                onValueChange = onBodyChange,
                label = stringResource(R.string.order_wa_message_label),
                modifier = Modifier.fillMaxWidth(),
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap)
            ) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.order_wa_open),
                    variant = AppButtonVariant.WhatsApp,
                    onClick = onOpenWhatsApp,
                    size = AppButtonSize.Compact,
                )
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.order_wa_sent),
                    variant = AppButtonVariant.Success,
                    onClick = onConfirmSent,
                    size = AppButtonSize.Compact,
                )
            }
        }
    }
}

@Composable
fun OrderDocumentSection(
    isGeneratingPdf: Boolean,
    pdfError: String?,
    documents: List<OrderDocumentEntity>,
    hasSignatures: Boolean,
    onGeneratePdf: (Boolean) -> Unit,
    onShareDocument: (String) -> Unit,
    onDeleteDocument: (String) -> Unit,
    requestConfirm: (String, String, () -> Unit) -> Unit,
) {
    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Text(stringResource(R.string.order_customer_documents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            AppButton(
                text = stringResource(R.string.order_generate_status_pdf),
                variant = AppButtonVariant.Secondary,
                enabled = !isGeneratingPdf,
                size = AppButtonSize.Compact,
                onClick = {
                    if (!hasSignatures) {
                        requestConfirm(
                            "Firma mancante",
                            "Il documento non conterra' la firma del cliente. Proseguire?"
                        ) {
                            onGeneratePdf(false)
                        }
                    } else {
                        onGeneratePdf(true)
                    }
                },
            )
            
            if (isGeneratingPdf) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            pdfError?.let { error ->
                AppMessageBanner(message = "Errore: $error", type = BannerType.Error)
            }

            if (documents.isNotEmpty()) {
                Text(stringResource(R.string.order_document_history), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                documents.forEach { doc ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).clickable { onShareDocument(doc.id) }) {
                            Text(doc.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = stringResource(R.string.order_document_generated, formatUiDateTime(doc.createdAt)) + (if (doc.sharedAt != null) " (${stringResource(R.string.order_wa_sent)})" else ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Row {
                            AppIconButton(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.order_share),
                                onClick = { onShareDocument(doc.id) },
                                tint = MaterialTheme.colorScheme.primary
                            )
                            AppIconButton(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.content_description_delete),
                                tint = MaterialTheme.colorScheme.error,
                                onClick = {
                                    requestConfirm(
                                        "Eliminare documento?",
                                        "Il file ${doc.fileName} verra' rimosso permanentemente."
                                    ) {
                                        onDeleteDocument(doc.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderTimelineSection(
    timeline: List<TimelineEventEntity>,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        SectionTitle(
            text = stringResource(R.string.order_timeline),
            tooltip = "Cronologia di tutti gli eventi e cambiamenti di stato relativi a questa commessa."
        )
        timeline.forEachIndexed { index, event ->
            val eventNumber = timeline.size - index
            CompactExpandableRow(
                title = "$eventNumber. ${event.title}",
                subtitle = formatUiDateTime(event.createdAt),
                detail = event.description.orEmpty(),
            )
        }
    }
}
