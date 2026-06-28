package com.onlytechtake.repairapp.features.dashboard.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    var showEncryptionModal by remember { mutableStateOf(false) }
    var tempPassword by rememberSaveable { mutableStateOf(securitySettings.backupEncryptionPassword) }
    var tempConfirmPassword by rememberSaveable { mutableStateOf(securitySettings.backupEncryptionPassword) }

    var showRestorePasswordModal by remember { mutableStateOf(false) }
    var restorePassword by rememberSaveable { mutableStateOf("") }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

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
            uri?.let {
                pendingRestoreUri = it
                restorePassword = ""
                showRestorePasswordModal = true
            }
        }
    val exportOrdersXlsxLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
            uri?.let(viewModel::exportOrdersXlsx)
        }

    val treeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                viewModel.setBackupUri(it)
            }
        }

    var showTimePicker by remember { mutableStateOf(false) }
    var isBackupRunning by remember { mutableStateOf(false) }

    var ordersResetConfirmation by rememberSaveable { mutableStateOf("") }
    var resetConfirmation by rememberSaveable { mutableStateOf("") }
    var isTotalResetExpanded by rememberSaveable { mutableStateOf(false) }

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

    val confirmDeleteAllOrdersTitle = stringResource(R.string.admin_confirm_delete_all_orders_title)
    val confirmDeleteAllOrdersText = stringResource(R.string.admin_confirm_delete_all_orders_text)
    val confirmTotalResetTitle = stringResource(R.string.admin_confirm_total_reset_title)
    val confirmTotalResetText = stringResource(R.string.admin_confirm_total_reset_text)

    val confirmAutoBackupTitle = stringResource(R.string.config_backup_auto_confirm_title)
    val confirmAutoBackupText = stringResource(R.string.config_backup_auto_confirm_text)

    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    AppScreenContainer {
        if (isBackupRunning) {
            androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                StandardCard {
                    Column(
                        modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                    ) {
                        Text(
                            text = stringResource(R.string.config_backup_auto_running),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.config_backup_auto_running_detail),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (isBackupReminderDue && !backupReminderSettings.isAutomatic) {
            AppMessageBanner(
                message = stringResource(R.string.home_backup_reminder_title),
                type = BannerType.Warning,
                modifier = Modifier.padding(bottom = RepairAppSpacing.cardGap),
            )
        }

        if (isProcessing) {
            Box(
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
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(
                        text = stringResource(R.string.config_backup_mode_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        RadioButton(
                            selected = !backupReminderSettings.isAutomatic,
                            onClick = { viewModel.setAutomaticBackupEnabled(false) },
                        )
                        Text(
                            text = stringResource(R.string.config_backup_mode_manual),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        RadioButton(
                            selected = backupReminderSettings.isAutomatic,
                            onClick = { viewModel.setAutomaticBackupEnabled(true) },
                        )
                        Text(
                            text = stringResource(R.string.config_backup_mode_auto),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.cardGap))

            if (!backupReminderSettings.isAutomatic) {
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

                        if (!backupReminderSettings.enabled) {
                            AppMessageBanner(
                                message = stringResource(R.string.config_backup_reminder_disabled_warning),
                                type = BannerType.Warning,
                            )
                        }
                    }
                }
            } else {
                StandardCard {
                    Column(
                        modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                    ) {
                        Text(
                            text = stringResource(R.string.config_backup_auto_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Text(
                            text = stringResource(R.string.config_backup_auto_destination),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        val displayPath =
                            backupReminderSettings.backupUri?.let {
                                try {
                                    val uri = it.toUri()
                                    uri.path?.substringAfterLast(":") ?: it
                                } catch (_: Exception) {
                                    it
                                }
                            } ?: stringResource(R.string.config_backup_auto_not_set)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = stringResource(R.string.config_backup_auto_info, displayPath),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = if (backupReminderSettings.backupUri == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                            AppButton(
                                text = stringResource(R.string.config_backup_auto_select_destination),
                                variant = AppButtonVariant.Secondary,
                                size = AppButtonSize.Compact,
                                onClick = { treeLauncher.launch(null) },
                            )
                        }

                        AppDivider()

                        Text(
                            text = stringResource(R.string.config_backup_auto_schedule),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text =
                                    stringResource(R.string.config_backup_auto_time) +
                                        stringResource(R.string.label_separator) +
                                        String.format(
                                            locale,
                                            stringResource(R.string.time_format_24h),
                                            backupReminderSettings.scheduledHour,
                                            backupReminderSettings.scheduledMinute,
                                        ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            AppButton(
                                text = stringResource(R.string.image_edit_rotate),
                                variant = AppButtonVariant.Secondary,
                                size = AppButtonSize.Compact,
                                leadingIcon = Icons.Default.CalendarMonth,
                                onClick = { showTimePicker = true },
                            )
                        }

                        if (showTimePicker) {
                            val timePickerState =
                                rememberTimePickerState(
                                    initialHour = backupReminderSettings.scheduledHour,
                                    initialMinute = backupReminderSettings.scheduledMinute,
                                    is24Hour = true,
                                )
                            AppDialog(
                                onDismissRequest = { showTimePicker = false },
                                title = stringResource(R.string.config_backup_auto_time),
                                confirmButton = {
                                    AppButton(
                                        text = stringResource(R.string.action_confirm),
                                        onClick = {
                                            viewModel.setBackupScheduledTime(
                                                timePickerState.hour,
                                                timePickerState.minute,
                                            )
                                            showTimePicker = false
                                        },
                                    )
                                },
                                dismissButton = {
                                    AppButton(
                                        text = stringResource(R.string.customer_cancel),
                                        variant = AppButtonVariant.Ghost,
                                        onClick = { showTimePicker = false },
                                    )
                                },
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TimePicker(state = timePickerState)
                                }
                            }
                        }

                        AppDivider()

                        AppButton(
                            text = stringResource(R.string.backup_run_full_zip),
                            variant = AppButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = Icons.Default.Backup,
                            enabled = backupReminderSettings.backupUri != null,
                            onClick = {
                                onRequestConfirm(confirmAutoBackupTitle, confirmAutoBackupText) {
                                    isBackupRunning = true
                                    viewModel.runBackupNow {
                                        isBackupRunning = false
                                    }
                                }
                            },
                        )
                    }
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

                    val isEncrypted = securitySettings.isBackupEncryptionEnabled && securitySettings.backupEncryptionPassword.isNotBlank()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isEncrypted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.config_backup_encrypt_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Switch(
                                    checked = securitySettings.isBackupEncryptionEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled && securitySettings.backupEncryptionPassword.isBlank()) {
                                            tempPassword = ""
                                            tempConfirmPassword = ""
                                            showEncryptionModal = true
                                        } else {
                                            viewModel.setBackupEncryptionEnabled(enabled)
                                        }
                                    },
                                )
                            }
                            Text(
                                text = if (isEncrypted) stringResource(R.string.config_backup_status_encrypted) else stringResource(R.string.config_backup_status_unencrypted),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEncrypted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (securitySettings.isBackupEncryptionEnabled) {
                        AppButton(
                            text = if (securitySettings.backupEncryptionPassword.isNotBlank()) stringResource(R.string.config_backup_password_change) else stringResource(R.string.config_backup_password_set),
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Compact,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                tempPassword = securitySettings.backupEncryptionPassword
                                tempConfirmPassword = securitySettings.backupEncryptionPassword
                                showEncryptionModal = true
                            },
                        )
                    }

                    if (!isEncrypted) {
                        AppMessageBanner(
                            message = stringResource(R.string.config_backup_unencrypted_warning),
                            type = BannerType.Warning,
                        )
                    }
                }
            }

            if (showEncryptionModal) {
                AppDialog(
                    onDismissRequest = {
                        if (securitySettings.backupEncryptionPassword.isBlank()) {
                            viewModel.setBackupEncryptionEnabled(false)
                        }
                        showEncryptionModal = false
                    },
                    title = stringResource(R.string.config_backup_password_modal_title),
                    confirmButton = {
                        AppButton(
                            text = stringResource(R.string.action_confirm),
                            enabled = tempPassword.isNotBlank() && tempPassword == tempConfirmPassword,
                            onClick = {
                                viewModel.setBackupEncryptionPassword(tempPassword)
                                viewModel.setBackupEncryptionEnabled(true)
                                showEncryptionModal = false
                            },
                        )
                    },
                    dismissButton = {
                        AppButton(
                            text = stringResource(R.string.customer_cancel),
                            variant = AppButtonVariant.Ghost,
                            onClick = {
                                if (securitySettings.backupEncryptionPassword.isBlank()) {
                                    viewModel.setBackupEncryptionEnabled(false)
                                }
                                showEncryptionModal = false
                            },
                        )
                    },
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                    ) {
                        Text(
                            text = stringResource(R.string.config_backup_password_modal_text),
                            style = MaterialTheme.typography.bodySmall,
                        )

                        AppTextField(
                            value = tempPassword,
                            onValueChange = { tempPassword = it },
                            label = stringResource(R.string.config_backup_password_label),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                            required = true,
                        )

                        AppTextField(
                            value = tempConfirmPassword,
                            onValueChange = { tempConfirmPassword = it },
                            label = stringResource(R.string.auth_confirm_password),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                            required = true,
                            showError = tempPassword != tempConfirmPassword,
                            errorText = stringResource(R.string.config_backup_password_confirm_error),
                        )

                        if (securitySettings.backupEncryptionPassword.isNotBlank()) {
                            AppButton(
                                text = stringResource(R.string.config_backup_password_remove),
                                variant = AppButtonVariant.Ghost,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    viewModel.setBackupEncryptionPassword("")
                                    viewModel.setBackupEncryptionEnabled(false)
                                    showEncryptionModal = false
                                },
                            )
                        }
                    }
                }
            }

            if (showRestorePasswordModal) {
                AppDialog(
                    onDismissRequest = { showRestorePasswordModal = false },
                    title = stringResource(R.string.config_backup_restore_password_title),
                    confirmButton = {
                        AppButton(
                            text = stringResource(R.string.config_backup_restore_zip),
                            onClick = {
                                pendingRestoreUri?.let { uri ->
                                    onRequestConfirm(confirmRestoreZipTitle, confirmRestoreZipText) {
                                        viewModel.restoreCompleteBackup(uri, onBackupRestored, restorePassword)
                                    }
                                }
                                showRestorePasswordModal = false
                            },
                        )
                    },
                    dismissButton = {
                        AppButton(
                            text = stringResource(R.string.customer_cancel),
                            variant = AppButtonVariant.Ghost,
                            onClick = { showRestorePasswordModal = false },
                        )
                    },
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                    ) {
                        Text(
                            text = stringResource(R.string.config_backup_restore_password_text),
                            style = MaterialTheme.typography.bodySmall,
                        )

                        AppTextField(
                            value = restorePassword,
                            onValueChange = { restorePassword = it },
                            label = stringResource(R.string.config_backup_password_label),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                            testTag = RepairAppTestTags.ConfigBackupRestorePasswordField,
                        )

                        Text(
                            text = stringResource(R.string.config_backup_unencrypted_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                            restoreZipLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/octet-stream",
                                    "*/*",
                                ),
                            )
                        },
                    )

                    AppDivider()

                    AppButton(
                        text = stringResource(R.string.config_backup_export_xlsx),
                        size = AppButtonSize.Compact,
                        variant = AppButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onRequestConfirm(confirmExportXlsxTitle, confirmExportXlsxText) {
                                exportOrdersXlsxLauncher.launch("onlytechtake_commesse_$timestamp.xlsx")
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.admin_total_reset),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Switch(
                            checked = isTotalResetExpanded,
                            onCheckedChange = { isTotalResetExpanded = it },
                        )
                    }

                    if (isTotalResetExpanded) {
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
                        } ?: stringResource(R.string.storage_size_zero)

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

                    SectionTitle(
                        text = stringResource(R.string.storage_deep_maintenance),
                        tooltip = stringResource(R.string.storage_tooltip),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.storage_cleanup_cache),
                                variant = AppButtonVariant.Secondary,
                                size = AppButtonSize.Compact,
                                onClick = viewModel::cleanupCache,
                            )
                            Text(
                                text = stringResource(R.string.storage_cleanup_cache_tooltip),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            AppButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.storage_cleanup_orphans),
                                variant = AppButtonVariant.Secondary,
                                size = AppButtonSize.Compact,
                                onClick = viewModel::cleanupOrphanedFiles,
                            )
                            Text(
                                text = stringResource(R.string.storage_cleanup_orphans_tooltip),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
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
            settings.lastBackupStatus == BackupReminderStatus.FAILED -> {
                stringResource(R.string.backup_status_failed)
            }

            settings.lastSuccessfulBackupAtMillis == null -> {
                stringResource(R.string.backup_status_never)
            }

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

@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    name = "Dark Mode",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun BackupSettingsPreview() {
    MaterialTheme {
        BackupSettings(
            viewModel =
                androidx.lifecycle.viewmodel.compose
                    .viewModel(),
            isAdmin = true,
            onBackupRestored = {},
            onRequestConfirm = { _, _, _ -> },
            onBack = {},
        )
    }
}
