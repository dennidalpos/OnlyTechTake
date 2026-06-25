package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDivider
import com.onlytechtake.repairapp.core.design.components.AppMessageBanner
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
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
    onResetComplete: () -> Unit = {},
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val backupReminderSettings by viewModel.backupReminderSettings.collectAsState()
    val securitySettings by viewModel.securitySettings.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isBackupReminderDue by viewModel.isBackupReminderDue.collectAsState()
    var backupIntervalDays by rememberSaveable { mutableStateOf(backupReminderSettings.intervalDays.toString()) }
    var backupPassword by rememberSaveable { mutableStateOf(securitySettings.backupEncryptionPassword) }

    val timestamp =
        remember {
            val date =
                java.text
                    .SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault())
                    .format(java.util.Date())
            "${com.onlytechtake.repairapp.BuildConfig.FLAVOR}_$date"
        }

    val exportZipLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let(viewModel::exportCompleteBackup)
        }
    val restoreZipLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.restoreCompleteBackup(it, onBackupRestored) }
        }
    val exportOrdersXlsxLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
            uri?.let(viewModel::exportOrdersXlsx)
        }
    val exportDatabaseLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-sqlite3")) { uri ->
            uri?.let(viewModel::exportDatabase)
        }

    var ordersResetConfirmation by rememberSaveable { mutableStateOf("") }
    var resetConfirmation by rememberSaveable { mutableStateOf("") }

    // Pre-fetch strings for callbacks
    val confirmExportZipTitle = stringResource(R.string.config_backup_confirm_export_zip_title)
    val confirmExportZipText = stringResource(R.string.config_backup_confirm_export_zip_text)
    val confirmRestoreZipTitle = stringResource(R.string.config_backup_confirm_restore_zip_title)
    val confirmRestoreZipText = stringResource(R.string.config_backup_confirm_restore_zip_text)
    val confirmExportXlsxTitle = stringResource(R.string.config_backup_confirm_export_xlsx_title)
    val confirmExportXlsxText = stringResource(R.string.config_backup_confirm_export_xlsx_text)
    val confirmSendLogsTitle = stringResource(R.string.config_backup_logs_send_confirm_title)
    val confirmSendLogsText = stringResource(R.string.config_backup_logs_send_confirm_text)
    val confirmClearLogsTitle = stringResource(R.string.config_backup_logs_clear_confirm_title)
    val confirmClearLogsText = stringResource(R.string.config_backup_logs_clear_confirm_text)

    val confirmExportDatabaseTitle = stringResource(R.string.admin_confirm_export_database_title)
    val confirmExportDatabaseText = stringResource(R.string.admin_confirm_export_database_text)
    val confirmDeleteAllOrdersTitle = stringResource(R.string.admin_confirm_delete_all_orders_title)
    val confirmDeleteAllOrdersText = stringResource(R.string.admin_confirm_delete_all_orders_text)
    val confirmTotalResetTitle = stringResource(R.string.admin_confirm_total_reset_title)
    val confirmTotalResetText = stringResource(R.string.admin_confirm_total_reset_text)

    AppScreenContainer {
        if (isBackupReminderDue) {
            AppMessageBanner(
                message = stringResource(R.string.home_backup_reminder_title),
                type = BannerType.Warning,
                modifier = Modifier.padding(bottom = RepairAppSpacing.cardGap),
            )
        }

        if (isProcessing) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(RepairAppSpacing.compactGap),
                )
            }
        }

        SectionTitle(
            text = stringResource(R.string.config_backup_title),
            tooltip = stringResource(R.string.config_backup_tooltip),
        )

        if (!isAdmin) {
            AppMessageBanner(
                message = stringResource(R.string.admin_only_section),
                type = BannerType.Warning,
            )
        } else {
            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.config_backup_reminder_enabled),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Switch(
                            checked = backupReminderSettings.enabled,
                            onCheckedChange = viewModel::setBackupReminderEnabled,
                            modifier =
                                Modifier
                                    .padding(start = RepairAppSpacing.compactGap)
                                    .testTag(RepairAppTestTags.ConfigBackupReminderSwitch),
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
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    Text(
                        text = stringResource(R.string.config_backup_security_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.config_backup_encrypt_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
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
                            visualTransformation =
                                androidx.compose.ui.text.input
                                    .PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                        )
                    }

                    AppMessageBanner(
                        message =
                            if (securitySettings.isBackupEncryptionEnabled) {
                                stringResource(R.string.config_backup_encrypted_success)
                            } else {
                                stringResource(
                                    R.string.config_backup_unencrypted_warning,
                                )
                            },
                        type = if (securitySettings.isBackupEncryptionEnabled) BannerType.Success else BannerType.Warning,
                    )
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.cardGap))

            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    AppButton(
                        text = stringResource(R.string.backup_run_full_zip),
                        size = AppButtonSize.Default,
                        variant = AppButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = RepairAppTestTags.ConfigBackupRunZipButton,
                        onClick = {
                            onRequestConfirm(confirmExportZipTitle, confirmExportZipText) {
                                exportZipLauncher.launch("onlytechtake_backup_$timestamp.zip")
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
                                restoreZipLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/octet-stream",
                                        "*/*",
                                    ),
                                )
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

            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    AppButton(
                        text = stringResource(R.string.config_backup_export_xlsx),
                        size = AppButtonSize.Default,
                        variant = AppButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onRequestConfirm(confirmExportXlsxTitle, confirmExportXlsxText) {
                                exportOrdersXlsxLauncher.launch("onlytechtake_commesse_$timestamp.xlsx")
                            }
                        },
                    )

                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.admin_export_database),
                        variant = AppButtonVariant.Secondary,
                        size = AppButtonSize.Compact,
                        onClick = {
                            onRequestConfirm(confirmExportDatabaseTitle, confirmExportDatabaseText) {
                                exportDatabaseLauncher.launch("onlytechtake_backup_$timestamp.db")
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.sectionGap))

            SectionTitle(
                text = stringResource(R.string.admin_total_reset),
                tooltip = stringResource(R.string.admin_total_reset_tooltip),
            )

            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    Text(
                        text = stringResource(R.string.admin_delete_all_orders),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.admin_delete_all_orders_tooltip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppTextField(
                        value = ordersResetConfirmation,
                        onValueChange = { ordersResetConfirmation = it },
                        label =
                            stringResource(
                                R.string.admin_delete_all_orders_field_short,
                                stringResource(R.string.admin_delete_all_orders_confirmation_value),
                            ),
                    )
                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.admin_delete_all_orders),
                        enabled = ordersResetConfirmation == stringResource(R.string.admin_delete_all_orders_confirmation_value),
                        variant = AppButtonVariant.Warning,
                        leadingIcon = Icons.Default.Delete,
                        testTag = RepairAppTestTags.AdminDeleteAllOrdersButton,
                        onClick = {
                            onRequestConfirm(
                                confirmDeleteAllOrdersTitle,
                                confirmDeleteAllOrdersText,
                            ) {
                                viewModel.resetOrders(ordersResetConfirmation) {
                                    ordersResetConfirmation = ""
                                }
                            }
                        },
                    )

                    Spacer(Modifier.height(RepairAppSpacing.cardGap))

                    AppDivider()

                    Text(
                        text = stringResource(R.string.admin_total_reset),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.admin_total_reset_tooltip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppTextField(
                        resetConfirmation,
                        { resetConfirmation = it },
                        stringResource(
                            R.string.admin_total_reset_field,
                            stringResource(R.string.admin_total_reset_confirmation_value),
                        ),
                    )
                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.admin_total_reset),
                        enabled = resetConfirmation == stringResource(R.string.admin_total_reset_confirmation_value),
                        variant = AppButtonVariant.Danger,
                        leadingIcon = Icons.Default.DeleteForever,
                        testTag = RepairAppTestTags.AdminTotalResetButton,
                        onClick = {
                            onRequestConfirm(confirmTotalResetTitle, confirmTotalResetText) {
                                viewModel.resetAllData(resetConfirmation, onResetComplete)
                                resetConfirmation = ""
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.sectionGap))

            SectionTitle(
                text = stringResource(R.string.config_backup_logs_title),
                tooltip = stringResource(R.string.config_backup_logs_tooltip),
            )

            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    val logsSize =
                        storageStats?.logsBytes?.let {
                            com.onlytechtake.repairapp.core.utils
                                .formatFileSize(it)
                        } ?: "0 B"

                    Text(
                        text = stringResource(R.string.config_backup_logs_size_label, logsSize),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        AppButton(
                            text = stringResource(R.string.config_backup_logs_send_whatsapp),
                            variant = AppButtonVariant.WhatsApp,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onRequestConfirm(
                                    confirmSendLogsTitle,
                                    confirmSendLogsText,
                                ) {
                                    viewModel.shareDiagnosticLogs()
                                }
                            },
                        )
                        AppButton(
                            text = stringResource(R.string.config_backup_logs_clear),
                            variant = AppButtonVariant.Warning,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onRequestConfirm(
                                    confirmClearLogsTitle,
                                    confirmClearLogsText,
                                ) {
                                    viewModel.clearDiagnosticLogs()
                                }
                            },
                        )
                    }

                    AppDivider()

                    Text(
                        text = stringResource(R.string.storage_deep_maintenance),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        AppButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.storage_cleanup_cache),
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Compact,
                            onClick = viewModel::cleanupCache,
                        )
                        AppButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.storage_cleanup_orphans),
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Compact,
                            onClick = viewModel::cleanupOrphanedFiles,
                        )
                    }
                }
            }
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
    val resources = LocalResources.current
    val status =
        when {
            settings.lastBackupStatus == BackupReminderStatus.FAILED ->
                stringResource(R.string.backup_status_failed)

            settings.lastSuccessfulBackupAtMillis == null ->
                stringResource(R.string.backup_status_never)

            else -> {
                val days =
                    BackupReminderPolicy.daysSinceLastSuccessfulBackup(
                        settings,
                        System.currentTimeMillis(),
                    ) ?: 0L
                resources.getQuantityString(
                    R.plurals.backup_status_days_ago,
                    days.toInt(),
                    days,
                )
            }
        }
    val interval =
        resources.getQuantityString(
            R.plurals.config_backup_reminder_interval,
            settings.intervalDays,
            settings.intervalDays,
        )
    val lastSuccessful =
        settings.lastSuccessfulBackupAtMillis
            ?.let { formatUiDateTime(it) }
            ?: stringResource(R.string.config_backup_last_success_none)
    val fileName =
        settings.lastBackupFileName
            ?: stringResource(R.string.config_backup_last_file_none)
    return stringResource(
        R.string.config_backup_reminder_info,
        status,
        lastSuccessful,
        interval,
        fileName,
    )
}
