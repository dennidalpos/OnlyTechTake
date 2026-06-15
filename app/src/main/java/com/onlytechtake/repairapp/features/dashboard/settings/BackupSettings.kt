package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.data.local.entities.ExportDestination
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.backup.BackupReminderStatus
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone

@Composable
fun BackupSettings(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onBackupRestored: () -> Unit,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val exportDestination by viewModel.exportDestination.collectAsState()
    val isCloudAvailable by viewModel.isCloudAvailable.collectAsState()
    val backupReminderSettings by viewModel.backupReminderSettings.collectAsState()
    var backupIntervalDays by rememberSaveable { mutableStateOf(backupReminderSettings.intervalDays.toString()) }

    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportCompleteBackup)
    }
    val restoreZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restoreCompleteBackup(it, onBackupRestored) }
    }
    val exportOrdersXlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let(viewModel::exportOrdersXlsx)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = DashboardSpacing.maxContentWidth),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            StandardCard(highlight = true) {
                Column(modifier = Modifier.padding(DashboardSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                    SectionTitle(
                        text = stringResource(R.string.config_backup_title),
                        tooltip = "Gestisci la frequenza dei promemoria e la destinazione dei file esportati.",
                    )
                    if (!isAdmin) {
                        Text(stringResource(R.string.admin_only_section))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.config_backup_reminder_enabled))
                            Switch(
                                checked = backupReminderSettings.enabled,
                                onCheckedChange = viewModel::setBackupReminderEnabled,
                                modifier = Modifier.testTag(RepairAppTestTags.ConfigBackupReminderSwitch),
                            )
                        }
                        Field(
                            value = backupIntervalDays,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(3)
                                backupIntervalDays = digits
                                digits.toIntOrNull()?.let(viewModel::setBackupReminderIntervalDays)
                            },
                            label = stringResource(R.string.config_backup_interval_days),
                            singleLine = true,
                            keyboardType = KeyboardType.Number,
                            testTag = RepairAppTestTags.ConfigBackupIntervalField,
                        )
                        OperatorText(backupReminderInfo(backupReminderSettings))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = "⚠️",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.config_backup_unencrypted_warning),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(stringResource(R.string.config_backup_destination_label), style = MaterialTheme.typography.titleSmall)
                        ActionRow {
                            TouchButton(
                                text = stringResource(R.string.config_backup_destination_local),
                                tone = if (exportDestination == ExportDestination.LOCAL) ButtonTone.PRIMARY else ButtonTone.NEUTRAL,
                                onClick = { viewModel.setExportDestination(ExportDestination.LOCAL) },
                            )
                            TouchButton(
                                text = stringResource(R.string.config_backup_destination_cloud),
                                tone = if (exportDestination == ExportDestination.CLOUD) ButtonTone.PRIMARY else ButtonTone.NEUTRAL,
                                enabled = isCloudAvailable,
                                onClick = { viewModel.setExportDestination(ExportDestination.CLOUD) },
                            )
                        }
                        if (!isCloudAvailable) {
                            OperatorText("Nessun provider cloud (Drive, OneDrive, Dropbox) rilevato. L'esportazione avverra' solo in locale.")
                        }

                        ActionRow {
                            val destMsgLocal = stringResource(R.string.order_export_local)
                            val destMsgCloud = stringResource(R.string.order_export_cloud)
                            val confirmExportZipTitle = stringResource(R.string.config_backup_confirm_export_zip_title)
                            val confirmExportZipText = stringResource(R.string.config_backup_confirm_export_zip_text)
                            TouchButton(
                                text = stringResource(R.string.backup_run_full_zip),
                                testTag = RepairAppTestTags.ConfigBackupRunZipButton,
                                onClick = {
                                    val destMsg = if (exportDestination == ExportDestination.LOCAL) destMsgLocal else destMsgCloud
                                    onRequestConfirm(confirmExportZipTitle, confirmExportZipText.format(destMsg)) {
                                        exportZipLauncher.launch("onlytechtake-backup.zip")
                                    }
                                },
                            )
                            val confirmRestoreZipTitle = stringResource(R.string.config_backup_confirm_restore_zip_title)
                            val confirmRestoreZipText = stringResource(R.string.config_backup_confirm_restore_zip_text)
                            TouchButton(
                                text = stringResource(R.string.config_backup_restore_zip),
                                tone = ButtonTone.WARNING,
                                onClick = {
                                    onRequestConfirm(
                                        confirmRestoreZipTitle,
                                        confirmRestoreZipText,
                                    ) {
                                        restoreZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                    }
                                },
                            )
                        }

                        SectionTitle(
                            text = stringResource(R.string.config_backup_orders_export_title),
                            tooltip = "Esporta l'elenco di tutte le commesse in un file Excel leggibile da PC.",
                        )
                        ActionRow {
                            val destMsgLocal = stringResource(R.string.order_export_local)
                            val destMsgCloud = stringResource(R.string.order_export_cloud)
                            val confirmExportXlsxTitle = stringResource(R.string.config_backup_confirm_export_xlsx_title)
                            val confirmExportXlsxText = stringResource(R.string.config_backup_confirm_export_xlsx_text)
                            TouchButton(
                                text = stringResource(R.string.config_backup_export_xlsx),
                                onClick = {
                                    val destMsg = if (exportDestination == ExportDestination.LOCAL) destMsgLocal else destMsgCloud
                                    onRequestConfirm(confirmExportXlsxTitle, confirmExportXlsxText.format(destMsg)) {
                                        exportOrdersXlsxLauncher.launch("onlytechtake-commesse.xlsx")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            TouchButton(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                text = "Chiudi Setup backup",
                tone = ButtonTone.NEUTRAL,
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun backupReminderInfo(settings: BackupReminderSettings): String {
    val status = when {
        settings.lastBackupStatus == BackupReminderStatus.FAILED ->
            stringResource(R.string.backup_status_failed)
        settings.lastSuccessfulBackupAtMillis == null ->
            stringResource(R.string.backup_status_never)
        else -> {
            val days = BackupReminderPolicy.daysSinceLastSuccessfulBackup(settings, System.currentTimeMillis()) ?: 0L
            stringResource(R.string.backup_status_days_ago, days)
        }
    }
    val lastSuccessful = settings.lastSuccessfulBackupAtMillis
        ?.let { formatUiDateTime(it) }
        ?: stringResource(R.string.config_backup_last_success_none)
    val fileName = settings.lastBackupFileName
        ?: stringResource(R.string.config_backup_last_file_none)
    return stringResource(
        R.string.config_backup_reminder_info,
        status,
        lastSuccessful,
        settings.intervalDays,
        fileName,
    )
}
