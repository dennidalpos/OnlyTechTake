package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.AppBadge
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.domain.models.Template

@Composable
fun AnchorStepHeader(anchor: TemplateAnchor) {
    val stepLabel =
        if (anchor.stepNumber == 0) {
            stringResource(R.string.config_template_legal_step)
        } else {
            stringResource(R.string.config_template_step_prefix, anchor.stepNumber)
        }
    Row(
        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBadge(
            text = stepLabel,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(anchor.stepNameRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AnchorSelectionDialog(
    anchor: TemplateAnchor,
    templates: List<Template>,
    currentMapping: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(RepairAppSpacing.screenHorizontal)
                        .fillMaxWidth(),
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.config_template_associate_with,
                            stringResource(anchor.labelRes),
                        ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(RepairAppSpacing.cardGap))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    item {
                        StandardCard(
                            onClick = { onSelect(anchor.defaultCode) },
                            highlight = (currentMapping == null || currentMapping == anchor.defaultCode),
                        ) {
                            Row(
                                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsBackupRestore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                                Spacer(modifier = Modifier.width(RepairAppSpacing.compactGap))
                                Text(
                                    text =
                                        stringResource(
                                            R.string.config_template_default_label,
                                            anchor.defaultCode,
                                        ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    items(templates) { template ->
                        StandardCard(
                            onClick = { onSelect(template.code) },
                            highlight = currentMapping == template.code,
                        ) {
                            Column(modifier = Modifier.padding(RepairAppSpacing.cardPadding)) {
                                Text(template.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    template.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(RepairAppSpacing.cardGap))
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = onDismiss,
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
