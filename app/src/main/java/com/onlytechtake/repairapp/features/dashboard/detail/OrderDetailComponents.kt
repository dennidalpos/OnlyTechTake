package com.onlytechtake.repairapp.features.dashboard.detail

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.design.repairColors
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import com.onlytechtake.repairapp.domain.workspace.availableOrderStatusActions
import com.onlytechtake.repairapp.domain.workspace.requiresConfirmation
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.CompactExpandableRow
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.QuoteDecision
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.workspace.OrderStatusStepper

@Composable
fun OrderHeader(
    detail: OrderDetail,
    context: Context,
) {
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                        Text(
                            text = detail.order.orderNumber,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        AppStatusBadge(
                            text = detail.order.status.label(context),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag(RepairAppTestTags.OrderStatusLabel),
                        )
                    }
                    Text(
                        text = stringResource(R.string.orders_order_reference, detail.order.orderNumber, detail.order.title),
                        modifier = Modifier.testTag(RepairAppTestTags.OrderDetailOrderReference),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.orders_customer_reference,
                                detail.customer?.displayName(context) ?: stringResource(R.string.orders_customer_unavailable),
                            ),
                        modifier = Modifier.testTag(RepairAppTestTags.OrderDetailCustomerReference),
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

            AppDivider(modifier = Modifier.padding(vertical = RepairAppSpacing.tinyGap).alpha(0.5f))

            OrderStatusStepper(
                status = detail.order.status,
                modifier = Modifier.testTag(RepairAppTestTags.OrderDetailStatusStepper),
                stepTestTag = RepairAppTestTags::orderDetailStatusStep,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
            ) {
                InfoMetadata(label = stringResource(R.string.order_updated_label), value = formatUiDateTime(detail.order.updatedAt))
                detail.order.assignedToUserId?.let { InfoMetadata(label = stringResource(R.string.order_operator_label), value = it) }
            }
        }
    }
}

@Composable
private fun InfoMetadata(
    label: String,
    value: String,
) {
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
    val actions = availableOrderStatusActions(status)
    val primaryAction = actions.firstOrNull { !it.requiresConfirmation() } ?: actions.firstOrNull()
    val secondaryActions = actions.filterNot { it == primaryAction }

    AppActionPanel(
        title = stringResource(R.string.order_actions_title),
        modifier = Modifier.testTag(RepairAppTestTags.OrderActionSection),
    ) {
        Text(stringResource(R.string.order_next_action_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (primaryAction != null) {
            AppButton(
                text = primaryAction.label,
                onClick = { onActionClick(primaryAction) },
                variant = if (primaryAction.requiresConfirmation()) AppButtonVariant.Warning else AppButtonVariant.Primary,
                size = AppButtonSize.FullWidth,
                testTag = RepairAppTestTags.statusAction(primaryAction.name),
            )
        }

        if (secondaryActions.isNotEmpty()) {
            Text(
                stringResource(R.string.order_secondary_actions_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ActionRow {
                secondaryActions.forEach { action ->
                    AppButton(
                        text = action.label,
                        onClick = { onActionClick(action) },
                        variant = if (action.requiresConfirmation()) AppButtonVariant.Warning else AppButtonVariant.Secondary,
                        size = AppButtonSize.Compact,
                        testTag = RepairAppTestTags.statusAction(action.name),
                    )
                }
            }
        }

        if (actions.isEmpty()) {
            Text(
                text = stringResource(R.string.action_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isPaid || status == OrderStatus.READY_FOR_PICKUP || status == OrderStatus.CLOSED) {
            AppDivider(modifier = Modifier.alpha(0.3f))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Checkbox, onClick = onTogglePayment),
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isPaid, onCheckedChange = null)
                Text(
                    text = stringResource(R.string.order_paid_button),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPaid) MaterialTheme.repairColors.success else Color.Unspecified,
                )
            }
        }

        AppDivider(modifier = Modifier.alpha(0.3f))

        AppButton(
            text = stringResource(R.string.order_whatsapp_send),
            variant = AppButtonVariant.WhatsApp,
            onClick = { onSendWhatsApp(whatsAppAnchor) },
            size = AppButtonSize.Compact,
            leadingIcon = Icons.AutoMirrored.Filled.Message,
            modifier = Modifier.fillMaxWidth().testTag(RepairAppTestTags.OrderWhatsAppButton),
        )
    }
}

@Composable
fun AppActionPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    StandardCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
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
                    testTag = RepairAppTestTags.QuoteAcceptedToggle,
                    onClick = { onQuoteDecisionChange(QuoteDecision.ACCEPTED) },
                )

                QuoteDecisionButton(
                    text = stringResource(R.string.order_quote_rejected),
                    selected = quoteDecision == QuoteDecision.REJECTED,
                    modifier = Modifier.weight(1f),
                    testTag = RepairAppTestTags.QuoteRejectedToggle,
                    onClick = { onQuoteDecisionChange(QuoteDecision.REJECTED) },
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
                    testTag = RepairAppTestTags.QuoteConfirmButton,
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
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    AppButton(
        text = text,
        onClick = onClick,
        variant = if (selected) AppButtonVariant.Primary else AppButtonVariant.Ghost,
        size = AppButtonSize.Compact,
        modifier = modifier,
        selected = selected,
        testTag = testTag,
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
        modifier =
            modifier
                .fillMaxWidth()
                .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
                .selectable(selected = checked, onClick = onSelect, role = Role.RadioButton)
                .background(
                    if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                    RepairAppShapes.compactButton,
                ).padding(horizontal = RepairAppSpacing.compactGap, vertical = RepairAppSpacing.compactGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
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
                modifier = Modifier.align(Alignment.End),
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
    StandardCard(modifier = Modifier.testTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)) {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.order_wa_preview_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AppIconButton(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.order_wa_close_preview),
                    onClick = onClose,
                    modifier = Modifier.testTag(RepairAppTestTags.OrderWhatsAppClosePreviewButton),
                )
            }

            AppMessageBanner(
                message = stringResource(R.string.order_wa_sending_to, prepared.phone),
                type = BannerType.Info,
            )

            AppTextField(
                value = body,
                onValueChange = onBodyChange,
                label = stringResource(R.string.order_wa_message_label),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap),
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
fun OrderTimelineSection(
    timeline: List<TimelineEventEntity>,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        SectionTitle(
            text = stringResource(R.string.order_timeline),
            tooltip = stringResource(R.string.order_timeline_tooltip),
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
