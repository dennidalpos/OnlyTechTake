package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
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
            items(TemplateAnchor.entries) { anchor ->
                val assignedCode = anchorMappings[anchor]
                val assignedTemplate = templates.find { it.code == assignedCode }

                StandardCard(
                    onClick = { selectingAnchorForTemplate = anchor },
                    highlight = assignedTemplate?.active == true,
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
                            Text(anchor.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                anchor.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Spacer(modifier = Modifier.height(RepairAppSpacing.compactGap))
                            Text(
                                text =
                                    stringResource(
                                        R.string.config_template_assigned_template,
                                        assignedTemplate?.title ?: stringResource(
                                            R.string.config_template_default_label,
                                            anchor.defaultCode,
                                        ),
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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

    selectingAnchorForTemplate?.let { anchor ->
        AnchorSelectionDialog(
            anchor = anchor,
            templates = templates.filter { it.active },
            currentMapping = anchorMappings[anchor],
            onDismiss = { selectingAnchorForTemplate = null },
            onSelect = { templateCode ->
                viewModel.setTemplateForAnchor(anchor, templateCode)
                selectingAnchorForTemplate = null
            },
        )
    }
}
