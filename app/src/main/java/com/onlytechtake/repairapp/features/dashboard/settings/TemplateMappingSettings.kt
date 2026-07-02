package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.AppBadge
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun ColumnScope.TemplateMappingSettings(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val templates by viewModel.templates.collectAsState()
    val anchorMappings by viewModel.anchorMappings.collectAsState()
    var selectingAnchorForTemplate by remember { mutableStateOf<TemplateAnchor?>(null) }
    var expandedAnchor by remember { mutableStateOf<TemplateAnchor?>(null) }

    val resetTitle = stringResource(R.string.config_template_reset_confirm_title)
    val resetText = stringResource(R.string.config_template_reset_confirm_text)

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .widthIn(max = RepairAppSpacing.maxContentWidthMedium),
        contentPadding = PaddingValues(RepairAppSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
    ) {
        item {
            SectionTitle(
                text = stringResource(R.string.config_hub_template_mapping_title),
                tooltip = stringResource(R.string.config_template_association_tooltip),
            )
        }

        if (isAdmin) {
            items(TemplateAnchor.entries, key = { it.settingKey }) { anchor ->
                val assignedCode = anchorMappings[anchor]
                val isDefault = assignedCode == anchor.defaultCode
                val assignedTemplate = templates.find { it.code == assignedCode }

                StandardCard(
                    onClick = { selectingAnchorForTemplate = anchor },
                    highlight = !isDefault,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(RepairAppSpacing.cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AnchorStepHeader(anchor)
                            Spacer(modifier = Modifier.height(RepairAppSpacing.compactGap))
                            Text(
                                text = stringResource(anchor.labelRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(anchor.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )

                            Spacer(modifier = Modifier.height(RepairAppSpacing.cardGap))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusIcon =
                                    when {
                                        assignedTemplate == null -> Icons.Default.Error
                                        isDefault -> Icons.Default.RadioButtonUnchecked
                                        else -> Icons.Default.Link
                                    }
                                val statusColor =
                                    when {
                                        assignedTemplate == null -> MaterialTheme.colorScheme.error
                                        isDefault -> MaterialTheme.colorScheme.outline
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                val statusText =
                                    when {
                                        assignedTemplate == null -> stringResource(R.string.config_template_status_missing)
                                        isDefault -> stringResource(R.string.config_template_status_default)
                                        else -> stringResource(R.string.config_template_status_associated)
                                    }
                                val badgeContainerColor =
                                    when {
                                        assignedTemplate == null -> MaterialTheme.colorScheme.errorContainer
                                        isDefault -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                val badgeContentColor =
                                    when {
                                        assignedTemplate == null -> MaterialTheme.colorScheme.onErrorContainer
                                        isDefault -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }

                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(RepairAppSpacing.compactGap))
                                AppBadge(
                                    text = statusText,
                                    containerColor = badgeContainerColor,
                                    contentColor = badgeContentColor,
                                )
                                Spacer(modifier = Modifier.width(RepairAppSpacing.compactGap))
                                Text(
                                    text =
                                        assignedTemplate?.title ?: assignedCode
                                        ?: stringResource(R.string.config_template_status_not_configured),
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (assignedTemplate == null) {
                                            MaterialTheme.colorScheme.error
                                        } else if (isDefault) {
                                            MaterialTheme.colorScheme.outline
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (assignedTemplate != null) {
                                    IconButton(
                                        onClick = {
                                            expandedAnchor =
                                                if (expandedAnchor == anchor) null else anchor
                                        },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (expandedAnchor == anchor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = expandedAnchor == anchor,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                assignedTemplate?.let { template ->
                                    Column(
                                        modifier =
                                            Modifier
                                                .padding(top = RepairAppSpacing.compactGap)
                                                .fillMaxWidth(),
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = RepairAppSpacing.compactGap),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                        Text(
                                            text = template.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isDefault) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = RepairAppSpacing.screenHorizontal))
            }

            item {
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.config_template_reset_defaults),
                    variant = AppButtonVariant.Secondary,
                    onClick = {
                        onRequestConfirm(resetTitle, resetText) {
                            viewModel.resetTemplatesToDefaults()
                        }
                    },
                )
            }
        }

        item {
            AppButton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = RepairAppSpacing.screenHorizontal),
                text = stringResource(R.string.config_template_close),
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                onClick = onBack,
            )
        }
    }

    val activeTemplates = remember(templates) { templates.filter { it.active } }

    selectingAnchorForTemplate?.let { anchor ->
        AnchorSelectionDialog(
            anchor = anchor,
            templates = activeTemplates,
            currentMapping = anchorMappings[anchor],
            onDismiss = { selectingAnchorForTemplate = null },
        ) { templateCode ->
            viewModel.setTemplateForAnchor(anchor, templateCode)
            selectingAnchorForTemplate = null
        }
    }
}
