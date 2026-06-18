package com.onlytechtake.repairapp.features.dashboard.common

import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.backup.BackupReminderStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppIconSizes
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.StandardCardVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionTitle(text: String, tooltip: String? = null) {
    Row(
        modifier = Modifier.padding(bottom = RepairAppSpacing.compactGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (tooltip != null) {
            val scope = rememberCoroutineScope()
            val tooltipState = rememberTooltipState(isPersistent = true)
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above
                ),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.clickable {
                            scope.launch { tooltipState.dismiss() }
                        }
                    ) {
                        Text(tooltip)
                    }
                },
                state = tooltipState
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.content_description_info),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(RepairAppIconSizes.small)
                        .clickable {
                            scope.launch { tooltipState.show() }
                        }
                )
            }
        }
    }
}

@Composable
fun AppEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StandardCard(variant = StandardCardVariant.Filled) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(RepairAppSpacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(RepairAppIconSizes.medium),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                AppButton(
                    text = actionLabel,
                    onClick = onAction,
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap),
    ) {
        content()
    }
}

@Composable
fun WizardNavigationBar(
    onBack: (() -> Unit)? = null,
    nextLabel: String = stringResource(R.string.nav_next),
    nextEnabled: Boolean = true,
    nextTestTag: String? = null,
    onNext: () -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RepairAppSpacing.cardGap),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap),
        ) {
            if (onBack != null) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.nav_back),
                    variant = AppButtonVariant.Ghost,
                    size = AppButtonSize.Default,
                    onClick = onBack,
                )
            }
            AppButton(
                modifier = Modifier.weight(if (onBack != null) 1.5f else 1f),
                text = nextLabel,
                enabled = nextEnabled,
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Default,
                testTag = nextTestTag,
                onClick = onNext,
            )
        }
        if (onCancel != null) {
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.nav_cancel_and_close),
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Compact,
                onClick = onCancel
            )
        }
    }
}

@Composable
fun OperatorText(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RepairAppShapes.compactButton)
            .padding(RepairAppSpacing.compactGap),
        text = text,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
fun CompactExpandableRow(
    title: String,
    subtitle: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(value = false) }
    val expandedState = if (expanded) stringResource(R.string.state_expanded) else stringResource(R.string.state_collapsed)

    StandardCard(
        modifier = modifier
            .semantics {
                stateDescription = expandedState
            },
        onClick = { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.compactGap),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank() && expanded) {
                OperatorText(detail)
            }
        }
    }
}

@Composable
fun CustomerTypeToggle(
    isBusiness: Boolean,
    onBusinessChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RepairAppShapes.pill)
            .padding(RepairAppSpacing.tinyGap),
        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.tinyGap)
    ) {
        val privateSelected = !isBusiness
        val businessSelected = isBusiness

        AppButton(
            text = stringResource(R.string.customer_type_private),
            onClick = { onBusinessChange(false) },
            modifier = Modifier.weight(1f),
            variant = if (privateSelected) AppButtonVariant.Primary else AppButtonVariant.Ghost,
            size = AppButtonSize.Compact,
            selected = privateSelected,
            testTag = RepairAppTestTags.CustomerPrivateToggle,
        )
        AppButton(
            text = stringResource(R.string.customer_type_business),
            onClick = { onBusinessChange(true) },
            modifier = Modifier.weight(1f),
            variant = if (businessSelected) AppButtonVariant.Primary else AppButtonVariant.Ghost,
            size = AppButtonSize.Compact,
            selected = businessSelected,
            testTag = RepairAppTestTags.CustomerBusinessToggle,
        )
    }
}

@Composable
fun NoteFieldWithDialog(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.customer_field_notes),
    testTag: String? = null,
) {
    var showDialog by rememberSaveable { mutableStateOf(value = false) }
    var tempNote by rememberSaveable { mutableStateOf(note) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        if (note.isNotBlank()) {
            OperatorText(note)
        }
        AppButton(
            text = if (note.isBlank()) label else "$label (${stringResource(R.string.customer_edit).lowercase()})",
            variant = AppButtonVariant.Secondary,
            size = AppButtonSize.Compact,
            testTag = testTag,
            onClick = {
                tempNote = note
                showDialog = true
            },
        )
    }

    if (showDialog) {
        AppDialog(
            onDismissRequest = { showDialog = false },
            title = label,
            content = {
                AppTextField(
                    value = tempNote,
                    onValueChange = { tempNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = label,
                )
            },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        onNoteChange(tempNote)
                        showDialog = false
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = { showDialog = false },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact
                )
            },
        )
    }
}

@Composable
fun BackupReminderCard(
    settings: BackupReminderSettings,
    onRunBackup: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    StandardCard(highlight = true) {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_backup_reminder_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                AppIconButton(
            onClick = onOpenSettings,
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.home_backup_reminder_settings),
            modifier = Modifier.size(RepairAppIconSizes.standard),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
            }
            Text(
                text = backupStatusLabel(settings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.backup_run_full_zip),
                onClick = onRunBackup,
                size = AppButtonSize.FullWidth,
            )
        }
    }
}

@Composable
fun backupStatusLabel(settings: BackupReminderSettings): String =
    when {
        settings.lastBackupStatus == BackupReminderStatus.FAILED ->
            stringResource(R.string.backup_status_failed)
        settings.lastSuccessfulBackupAtMillis == null ->
            stringResource(R.string.backup_status_never)
        else -> {
            val days = BackupReminderPolicy.daysSinceLastSuccessfulBackup(settings, System.currentTimeMillis()) ?: 0L
            LocalResources.current.getQuantityString(
                R.plurals.backup_status_days_ago,
                days.toInt(),
                days,
            )
        }
    }
