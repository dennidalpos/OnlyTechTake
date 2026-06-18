package com.onlytechtake.repairapp.features.dashboard.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.PasswordVisibilityButton
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.repairColors
import com.onlytechtake.repairapp.features.dashboard.common.*

@Composable
fun AdminPanel(
    viewModel: AdminViewModel,
    isAdmin: Boolean,
    onResetComplete: () -> Unit,
) {
    val users by viewModel.users.collectAsState()
    val recoveryCode by viewModel.recoveryCode.collectAsState()
    val isBackupReminderDue by viewModel.isBackupReminderDue.collectAsState()
    val message by viewModel.message.collectAsState()

    val exportDatabaseLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-sqlite3")) { uri ->
        uri?.let(viewModel::exportDatabase)
    }
    val exportOrdersXlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let(viewModel::exportOrdersXlsx)
    }

    val confirmCreateTitle = stringResource(R.string.admin_confirm_create_title)
    val confirmCreateText = stringResource(R.string.admin_confirm_create_text)
    val labelActivate = stringResource(R.string.admin_user_activate)
    val labelDeactivate = stringResource(R.string.admin_user_deactivate)
    val labelDelete = stringResource(R.string.content_description_delete)
    val confirmDeleteTitle = stringResource(R.string.admin_confirm_delete_user_title)
    val confirmDeleteTextTemplate = stringResource(R.string.admin_confirm_delete_user_text)
    val confirmResetTitle = stringResource(R.string.admin_confirm_reset_title)
    val confirmResetTextTemplate = stringResource(R.string.admin_confirm_reset_text)
    val confirmTotalResetTitle = stringResource(R.string.admin_confirm_total_reset_title)
    val confirmTotalResetText = stringResource(R.string.admin_confirm_total_reset_text)
    val confirmDeleteAllOrdersTitle = stringResource(R.string.admin_confirm_delete_all_orders_title)
    val confirmDeleteAllOrdersText = stringResource(R.string.admin_confirm_delete_all_orders_text)
    val confirmExportDatabaseTitle = stringResource(R.string.admin_confirm_export_database_title)
    val confirmExportDatabaseText = stringResource(R.string.admin_confirm_export_database_text)
    val confirmExportXlsxTitle = stringResource(R.string.config_backup_confirm_export_xlsx_title)
    val confirmExportXlsxText = stringResource(R.string.config_backup_confirm_export_xlsx_text)

    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    
    var resetPasswords by remember { mutableStateOf(emptyMap<String, String>()) }
    
    var resetConfirmation by rememberSaveable { mutableStateOf("") }
    var ordersResetConfirmation by rememberSaveable { mutableStateOf("") }
    
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    if (!isAdmin) {
        AppScreenContainer {
            AppMessageBanner(message = stringResource(R.string.admin_only_section), type = BannerType.Warning)
        }
        return
    }

    AppScreenContainer {
        message?.let {
            DismissibleAppMessageBanner(
                message = it.text,
                type = if (it.isError) BannerType.Error else BannerType.Info,
                onDismiss = viewModel::clearMessage,
            )
        }

        SectionTitle(stringResource(R.string.admin_access_security))
        
        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
            ) {
                Text(stringResource(R.string.admin_create_operator), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AppTextField(displayName, { displayName = it }, stringResource(R.string.customer_field_first_name), required = true)
                AppTextField(username, { username = it }, stringResource(R.string.auth_username), required = true)
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.admin_field_temp_password),
                    required = true,
                    singleLine = true,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        PasswordVisibilityButton(
                            visible = passwordVisible,
                            onVisibleChange = { passwordVisible = it },
                            modifier = Modifier.focusProperties { canFocus = false },
                        )
                    },
                )
                AppButton(
                    text = stringResource(R.string.admin_create_operator),
                    enabled = displayName.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    size = AppButtonSize.Default,
                    variant = AppButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = RepairAppTestTags.AdminCreateOperatorButton,
                    onClick = {
                        requestConfirm(confirmCreateTitle, confirmCreateText) {
                            viewModel.createOperator(displayName, username, password.toCharArray())
                            displayName = ""
                            username = ""
                            password = ""
                            passwordVisible = false
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.cardGap))

        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
            ) {
                SectionTitle(
                    text = stringResource(R.string.admin_recovery_kit),
                    tooltip = stringResource(R.string.admin_recovery_tooltip),
                )
                
                if (recoveryCode == null) {
                    AppButton(
                        text = stringResource(R.string.admin_recovery_generate),
                        variant = AppButtonVariant.Secondary,
                        size = AppButtonSize.Compact,
                        onClick = { viewModel.generateRecoveryKit() }
                    )
                } else {
                    AppMessageBanner(
                        message = stringResource(R.string.admin_recovery_offline_warning),
                        type = BannerType.Warning
                    )
                    StandardCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                        Column(
                            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
                        ) {
                            Text(stringResource(R.string.admin_recovery_master_code), style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = recoveryCode!!,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            AppButton(
                                text = stringResource(R.string.admin_recovery_hide),
                                variant = AppButtonVariant.Ghost,
                                size = AppButtonSize.Compact,
                                onClick = { viewModel.clearRecoveryCode() }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))
        SectionTitle(stringResource(R.string.admin_users_title))
        
        if (users.isEmpty()) {
            AppEmptyState(
                title = stringResource(R.string.admin_no_users_title),
                body = stringResource(R.string.admin_no_users_body),
            )
        } else {
            users.forEach { user ->
                var resetPasswordVisible by rememberSaveable(user.id) { mutableStateOf(false) }
                var currentPassword by rememberSaveable(user.id) { mutableStateOf("") }
                var currentPasswordVisible by rememberSaveable(user.id) { mutableStateOf(false) }

                StandardCard(modifier = Modifier.padding(bottom = RepairAppSpacing.cardGap)) {
                    Column(
                        modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(user.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AppStatusBadge(
                                text = user.role.name,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        if (user.role != UserRole.ADMIN) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = RepairAppSpacing.tinyGap),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (user.active) stringResource(R.string.admin_user_active) else stringResource(R.string.admin_user_inactive),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (user.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = if (user.active) "L'operatore può accedere al sistema" else "L'operatore non può effettuare il login",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = user.active,
                                    onCheckedChange = { checked ->
                                        requestConfirm(
                                            title = if (checked) "$labelActivate?" else "$labelDeactivate?",
                                            text = if (checked) {
                                                "Vuoi attivare l'operatore ${user.displayName}? Potrà riprendere a gestire le commesse."
                                            } else {
                                                "Vuoi disattivare l'operatore ${user.displayName}? Non potrà più accedere all'app finché non verrà riattivato."
                                            }
                                        ) {
                                            viewModel.setUserActive(user.id, checked)
                                        }
                                    }
                                )
                                AppIconButton(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = labelDelete,
                                    onClick = {
                                        requestConfirm(
                                            title = "$labelDelete?",
                                            text = confirmDeleteTextTemplate.format(user.displayName)
                                        ) {
                                            viewModel.deleteUser(user.id)
                                        }
                                    },
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            
                            AppDivider(modifier = Modifier.alpha(0.3f))
                            Text(stringResource(R.string.admin_permissions_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            
                            val permissionGroups = listOf(
                                PermissionUIItem(UserPermission.CREATE_ORDERS, stringResource(R.string.admin_permission_create_orders)),
                                PermissionUIItem(UserPermission.EDIT_OWN_ORDERS, stringResource(R.string.admin_permission_edit_own_orders)),
                                PermissionUIItem(UserPermission.EDIT_ALL_ORDERS, stringResource(R.string.admin_permission_edit_all_orders)),
                                PermissionUIItem(UserPermission.CLOSE_ORDERS, stringResource(R.string.admin_permission_close_orders)),
                                PermissionUIItem(UserPermission.MANAGE_CUSTOMERS, stringResource(R.string.admin_permission_manage_customers)),
                                PermissionUIItem(UserPermission.MANAGE_CATEGORIES, stringResource(R.string.admin_permission_manage_categories)),
                                PermissionUIItem(UserPermission.MANAGE_TEMPLATES, stringResource(R.string.admin_permission_manage_templates)),
                                PermissionUIItem(UserPermission.MANAGE_PAYMENTS, stringResource(R.string.admin_permission_manage_payments)),
                                PermissionUIItem(UserPermission.EXPORT_DATA, stringResource(R.string.admin_permission_export_data)),
                                PermissionUIItem(UserPermission.MANAGE_STORAGE, stringResource(R.string.admin_permission_manage_storage)),
                            )

                            permissionGroups.forEach { item ->
                                val hasPermission = user.permissions.contains(item.permission)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = hasPermission,
                                            role = Role.Switch,
                                            onValueChange = { checked ->
                                                val newPermissions = if (checked) {
                                                    user.permissions + item.permission
                                                } else {
                                                    user.permissions - item.permission
                                                }
                                                viewModel.setUserPermissions(user.id, newPermissions)
                                            },
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.label, style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = hasPermission,
                                        onCheckedChange = null,
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }
                            }
                            AppDivider(modifier = Modifier.alpha(0.3f))
                        }

                        if (user.role == UserRole.ADMIN) {
                            AppTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                label = stringResource(R.string.admin_current_password),
                                singleLine = true,
                                keyboardType = KeyboardType.Password,
                                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    PasswordVisibilityButton(
                                        visible = currentPasswordVisible,
                                        onVisibleChange = { currentPasswordVisible = it },
                                        modifier = Modifier.focusProperties { canFocus = false },
                                    )
                                },
                            )
                        }

                        AppTextField(
                            value = resetPasswords[user.id].orEmpty(),
                            onValueChange = { value ->
                                resetPasswords += (user.id to value)
                            },
                            label = if (user.role == UserRole.ADMIN) {
                                stringResource(R.string.admin_new_password)
                            } else {
                                stringResource(R.string.admin_field_temp_password)
                            },
                            singleLine = true,
                            keyboardType = KeyboardType.Password,
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                PasswordVisibilityButton(
                                    visible = resetPasswordVisible,
                                    onVisibleChange = { resetPasswordVisible = it },
                                    modifier = Modifier.focusProperties { canFocus = false },
                                )
                            },
                        )
                        
                        AppButton(
                            text = if (user.role == UserRole.ADMIN) {
                                stringResource(R.string.admin_change_password)
                            } else {
                                stringResource(R.string.admin_reset_password)
                            },
                            enabled = if (user.role == UserRole.ADMIN) {
                                currentPassword.isNotBlank() && resetPasswords[user.id]?.isNotBlank() == true
                            } else {
                                resetPasswords[user.id]?.isNotBlank() == true
                            },
                            leadingIcon = Icons.Default.Key,
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Compact,
                            modifier = Modifier.align(Alignment.End),
                            onClick = {
                                val resetPassword = resetPasswords[user.id].orEmpty()
                                if (user.role == UserRole.ADMIN) {
                                    viewModel.changePassword(user.username, currentPassword.toCharArray(), resetPassword.toCharArray())
                                    currentPassword = ""
                                    resetPasswords -= user.id
                                } else {
                                    requestConfirm(confirmResetTitle, confirmResetTextTemplate.format(user.displayName)) {
                                        viewModel.resetPassword(user.id, resetPassword.toCharArray())
                                        resetPasswords -= user.id
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        if (isBackupReminderDue) {
            AppMessageBanner(
                message = "${stringResource(R.string.admin_backup_overdue_title)}: ${stringResource(R.string.admin_backup_overdue_text)}",
                type = BannerType.Error
            )
            Spacer(Modifier.height(RepairAppSpacing.cardGap))
        }

        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
            ) {
                SectionTitle(
                    text = stringResource(R.string.config_backup_orders_export_title),
                    tooltip = stringResource(R.string.config_backup_orders_tooltip),
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.admin_export_database),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = {
                        requestConfirm(confirmExportDatabaseTitle, confirmExportDatabaseText) {
                            exportDatabaseLauncher.launch("repairapp-export.db")
                        }
                    }
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.config_backup_export_xlsx),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = {
                        requestConfirm(confirmExportXlsxTitle, confirmExportXlsxText) {
                            exportOrdersXlsxLauncher.launch("repairapp-commesse.xlsx")
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.cardGap))

        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
            ) {
                SectionTitle(
                    text = stringResource(R.string.admin_delete_all_orders),
                    tooltip = stringResource(R.string.admin_delete_all_orders_tooltip),
                )
                AppTextField(
                    value = ordersResetConfirmation,
                    onValueChange = { ordersResetConfirmation = it },
                    label = stringResource(R.string.admin_delete_all_orders_field_short)
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.admin_delete_all_orders),
                    enabled = ordersResetConfirmation == "CANCELLA TUTTO",
                    variant = AppButtonVariant.Warning,
                    leadingIcon = Icons.Default.Delete,
                    testTag = RepairAppTestTags.AdminDeleteAllOrdersButton,
                    onClick = {
                        requestConfirm(
                            confirmDeleteAllOrdersTitle,
                            confirmDeleteAllOrdersText,
                        ) {
                            viewModel.resetOrders(ordersResetConfirmation) {
                                ordersResetConfirmation = ""
                            }
                        }
                    }
                )
            }
        }
        
        Spacer(Modifier.height(RepairAppSpacing.cardGap))

        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
            ) {
                SectionTitle(
                    text = stringResource(R.string.admin_total_reset),
                    tooltip = stringResource(R.string.admin_total_reset_tooltip),
                )
                AppTextField(resetConfirmation, { resetConfirmation = it }, stringResource(R.string.admin_total_reset_field))
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.admin_total_reset),
                    enabled = resetConfirmation == "RESET",
                    variant = AppButtonVariant.Danger,
                    leadingIcon = Icons.Default.DeleteForever,
                    testTag = RepairAppTestTags.AdminTotalResetButton,
                    onClick = {
                        requestConfirm(confirmTotalResetTitle, confirmTotalResetText) {
                            viewModel.resetAllData(resetConfirmation, onResetComplete)
                            resetConfirmation = ""
                        }
                    },
                )
            }
        }
    }

    confirmTitle?.let { title ->
        AppDialog(
            onDismissRequest = {
                confirmTitle = null
                confirmAction = null
            },
            title = title,
            text = confirmText,
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        confirmAction?.invoke()
                        confirmTitle = null
                        confirmAction = null
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = {
                        confirmTitle = null
                        confirmAction = null
                    },
                )
            },
        )
    }
}

private data class PermissionUIItem(
    val permission: UserPermission,
    val label: String
)
