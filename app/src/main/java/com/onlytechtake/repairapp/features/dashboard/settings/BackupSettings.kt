package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.data.local.entities.ExportDestination
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.backup.BackupReminderStatus
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

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
    val securitySettings by viewModel.securitySettings.collectAsState()
    var backupIntervalDays by rememberSaveable { mutableStateOf(backupReminderSettings.intervalDays.toString()) }
    var backupPassword by rememberSaveable { mutableStateOf(securitySettings.backupEncryptionPassword) }

    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportCompleteBackup)
    }
    val restoreZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restoreCompleteBackup(it, onBackupRestored) }
    }
    val exportOrdersXlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let(viewModel::exportOrdersXlsx)
    }

    // Pre-fetch strings for callbacks
    val destMsgLocal = stringResource(R.string.order_export_local)
    val destMsgCloud = stringResource(R.string.order_export_cloud)
    val confirmExportZipTitle = stringResource(R.string.config_backup_confirm_export_zip_title)
    val confirmExportZipText = stringResource(R.string.config_backup_confirm_export_zip_text)
    val confirmRestoreZipTitle = stringResource(R.string.config_backup_confirm_restore_zip_title)
    val confirmRestoreZipText = stringResource(R.string.config_backup_confirm_restore_zip_text)
    val confirmExportXlsxTitle = stringResource(R.string.config_backup_confirm_export_xlsx_title)
    val confirmExportXlsxText = stringResource(R.string.config_backup_confirm_export_xlsx_text)

    AppScreenContainer {
        SectionTitle(
            text = stringResource(R.string.config_backup_title),
            tooltip = stringResource(R.string.config_backup_tooltip),
        )

        if (!isAdmin) {
            AppMessageBanner(
                message = stringResource(R.string.admin_only_section),
                type = BannerType.Warning
            )
        } else {
            StandardCard {
                Column(modifier = Modifier.padding(RepairAppSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.config_backup_reminder_enabled), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = backupReminderSettings.enabled,
                            onCheckedChange = viewModel::setBackupReminderEnabled,
                            modifier = Modifier.testTag(RepairAppTestTags.ConfigBackupReminderSwitch),
                        )
                    }
                    AppTextField(
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
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.cardGap))

            StandardCard {
                Column(modifier = Modifier.padding(RepairAppSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)) {
                    Text(
                        text = stringResource(R.string.config_backup_security_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.config_backup_encrypt_label), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = securitySettings.isBackupEncryptionEnabled,
                            onCheckedChange = viewModel::setBackupEncryptionEnabled,
                        )
                    }
                    if (securitySettings.isBackupEncryptionEnabled) {
                        AppTextField(
                            value = backupPassword,
                            onValueChange = { 
                                backupPassword = it
                                viewModel.setBackupEncryptionPassword(it)
                            },
                            label = stringResource(R.string.config_backup_password_label),
                            required = true,
                            showError = backupPassword.isBlank(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                        )
                    }

                    AppMessageBanner(
                        message = if (securitySettings.isBackupEncryptionEnabled) stringResource(R.string.config_backup_encrypted_success) else stringResource(R.string.config_backup_unencrypted_warning),
                        type = if (securitySettings.isBackupEncryptionEnabled) BannerType.Success else BannerType.Warning
                    )
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.cardGap))

            StandardCard {
                Column(modifier = Modifier.padding(RepairAppSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)) {
                    Text(stringResource(R.string.config_backup_destination_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                        AppButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.config_backup_destination_local),
                            variant = if (exportDestination == ExportDestination.LOCAL) AppButtonVariant.Primary else AppButtonVariant.Ghost,
                            size = AppButtonSize.Compact,
                            onClick = { viewModel.setExportDestination(ExportDestination.LOCAL) },
                            selected = exportDestination == ExportDestination.LOCAL
                        )
                        AppButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.config_backup_destination_cloud),
                            variant = if (exportDestination == ExportDestination.CLOUD) AppButtonVariant.Primary else AppButtonVariant.Ghost,
                            size = AppButtonSize.Compact,
                            onClick = { viewModel.setExportDestination(ExportDestination.CLOUD) },
                            selected = exportDestination == ExportDestination.CLOUD
                        )
                    }
                    
                    if (!isCloudAvailable && exportDestination == ExportDestination.CLOUD) {
                        AppMessageBanner(message = stringResource(R.string.config_backup_provider_unavailable), type = BannerType.Info)
                    }

                    Spacer(Modifier.height(8.dp))
                    
                    AppButton(
                        text = stringResource(R.string.backup_run_full_zip),
                        size = AppButtonSize.Default,
                        variant = AppButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = RepairAppTestTags.ConfigBackupRunZipButton,
                        onClick = {
                            val destMsg = if (exportDestination == ExportDestination.LOCAL) destMsgLocal else destMsgCloud
                            onRequestConfirm(confirmExportZipTitle, confirmExportZipText.format(destMsg)) {
                                exportZipLauncher.launch("onlytechtake-backup.zip")
                            }
                        },
                    )
                    
                    AppButton(
                        text = stringResource(R.string.config_backup_restore_zip),
                        variant = AppButtonVariant.Warning,
                        size = AppButtonSize.Compact,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = RepairAppTestTags.BackupRestoreZipButton,
                        onClick = {
                            onRequestConfirm(confirmRestoreZipTitle, confirmRestoreZipText) {
                                restoreZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.sectionGap))

            SectionTitle(
                text = stringResource(R.string.config_backup_orders_export_title),
                tooltip = stringResource(R.string.config_backup_orders_tooltip),
            )
            
            AppButton(
                text = stringResource(R.string.config_backup_export_xlsx),
                size = AppButtonSize.Default,
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val destMsg = if (exportDestination == ExportDestination.LOCAL) destMsgLocal else destMsgCloud
                    onRequestConfirm(confirmExportXlsxTitle, confirmExportXlsxText.format(destMsg)) {
                        exportOrdersXlsxLauncher.launch("onlytechtake-commesse.xlsx")
                    }
                },
            )
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        AppButton(
            text = stringResource(R.string.config_backup_close),
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Default,
            modifier = Modifier.fillMaxWidth(),
            testTag = RepairAppTestTags.BackupCloseButton,
            onClick = onBack,
        )
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
