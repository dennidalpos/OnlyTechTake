package com.onlytechtake.repairapp.features.dashboard.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.PasswordVisibilityButton
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDivider
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppMessageBanner
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.AppStatusBadge
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.features.dashboard.common.AppEmptyState
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.settings.SettingsViewModel

@Composable
fun AdminPanel(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit = {},
) {
    val users by viewModel.users.collectAsState()
    val recoveryCode by viewModel.recoveryCode.collectAsState()
    val message by viewModel.message.collectAsState()

    val confirmCreateTitle = stringResource(R.string.admin_confirm_create_title)
    val confirmCreateText = stringResource(R.string.admin_confirm_create_text)
    val labelActivate = stringResource(R.string.admin_user_activate)
    val labelDeactivate = stringResource(R.string.admin_user_deactivate)
    val labelDelete = stringResource(R.string.content_description_delete)
    val confirmDeleteTextTemplate = stringResource(R.string.admin_confirm_delete_user_text)
    val confirmResetTitle = stringResource(R.string.admin_confirm_reset_title)
    val confirmResetTextTemplate = stringResource(R.string.admin_confirm_reset_text)

    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(value = false) }

    var resetPasswords by remember { mutableStateOf(emptyMap<String, String>()) }

    if (!isAdmin) {
        AppScreenContainer {
            AppMessageBanner(
                message = stringResource(R.string.admin_only_section),
                type = BannerType.Warning,
            )
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
                Text(
                    stringResource(R.string.admin_create_operator),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AppTextField(
                    displayName,
                    { displayName = it },
                    stringResource(R.string.customer_field_first_name),
                    required = true,
                    modifier = Modifier.testTag(RepairAppTestTags.AdminDisplayNameField),
                )
                AppTextField(
                    username,
                    { username = it },
                    stringResource(R.string.auth_username),
                    required = true,
                    modifier = Modifier.testTag(RepairAppTestTags.AdminUsernameField),
                )
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.admin_field_temp_password),
                    required = true,
                    singleLine = true,
                    keyboardType = KeyboardType.Password,
                    modifier = Modifier.testTag(RepairAppTestTags.AdminPasswordField),
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
                        onRequestConfirm(confirmCreateTitle, confirmCreateText) {
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
                        onClick = { viewModel.generateRecoveryKit() },
                    )
                } else {
                    AppMessageBanner(
                        message = stringResource(R.string.admin_recovery_offline_warning),
                        type = BannerType.Warning,
                    )
                    StandardCard(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f,
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                        ) {
                            Text(
                                stringResource(R.string.admin_recovery_master_code),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                text = recoveryCode!!,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            AppButton(
                                text = stringResource(R.string.admin_recovery_hide),
                                variant = AppButtonVariant.Ghost,
                                size = AppButtonSize.Compact,
                                onClick = { viewModel.clearRecoveryCode() },
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    user.username,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            AppStatusBadge(
                                text = user.role.name,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }

                        if (user.role != UserRole.ADMIN) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = RepairAppSpacing.tinyGap),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text =
                                            if (user.active) {
                                                stringResource(R.string.admin_user_active)
                                            } else {
                                                stringResource(
                                                    R.string.admin_user_inactive,
                                                )
                                            },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (user.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = if (user.active) "L'operatore può accedere al sistema" else "L'operatore non può effettuare il login",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = user.active,
                                    onCheckedChange = { checked ->
                                        onRequestConfirm(
                                            if (checked) "$labelActivate?" else "$labelDeactivate?",
                                            if (checked) {
                                                "Vuoi attivare l'operatore ${user.displayName}? Potrà riprendere a gestire le commesse."
                                            } else {
                                                "Vuoi disattivare l'operatore ${user.displayName}? Non potrà più accedere all'app finché non verrà riattivato."
                                            },
                                        ) {
                                            viewModel.setUserActive(user.id, checked)
                                        }
                                    },
                                )
                                AppIconButton(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = labelDelete,
                                    onClick = {
                                        onRequestConfirm(
                                            "$labelDelete?",
                                            confirmDeleteTextTemplate.format(user.displayName),
                                        ) {
                                            viewModel.deleteUser(user.id)
                                        }
                                    },
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }

                            AppDivider(modifier = Modifier.alpha(0.3f))
                            Text(
                                stringResource(R.string.admin_permissions_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )

                            val permissionGroups =
                                listOf(
                                    PermissionUIItem(
                                        UserPermission.CREATE_ORDERS,
                                        stringResource(R.string.admin_permission_create_orders),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.EDIT_OWN_ORDERS,
                                        stringResource(R.string.admin_permission_edit_own_orders),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.EDIT_ALL_ORDERS,
                                        stringResource(R.string.admin_permission_edit_all_orders),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.CLOSE_ORDERS,
                                        stringResource(R.string.admin_permission_close_orders),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.MANAGE_CUSTOMERS,
                                        stringResource(R.string.admin_permission_manage_customers),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.MANAGE_CATEGORIES,
                                        stringResource(R.string.admin_permission_manage_categories),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.MANAGE_TEMPLATES,
                                        stringResource(R.string.admin_permission_manage_templates),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.MANAGE_PAYMENTS,
                                        stringResource(R.string.admin_permission_manage_payments),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.EXPORT_DATA,
                                        stringResource(R.string.admin_permission_export_data),
                                    ),
                                    PermissionUIItem(
                                        UserPermission.MANAGE_STORAGE,
                                        stringResource(R.string.admin_permission_manage_storage),
                                    ),
                                )

                            permissionGroups.forEach { item ->
                                val hasPermission = user.permissions.contains(item.permission)
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .toggleable(
                                                value = hasPermission,
                                                role = Role.Switch,
                                            ) { checked ->
                                                val newPermissions =
                                                    if (checked) {
                                                        user.permissions + item.permission
                                                    } else {
                                                        user.permissions - item.permission
                                                    }
                                                viewModel.setUserPermissions(
                                                    user.id,
                                                    newPermissions,
                                                )
                                            },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(item.label, style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = hasPermission,
                                        onCheckedChange = null,
                                        modifier = Modifier.scale(0.7f),
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
                            label =
                                if (user.role == UserRole.ADMIN) {
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
                            text =
                                if (user.role == UserRole.ADMIN) {
                                    stringResource(R.string.admin_change_password)
                                } else {
                                    stringResource(R.string.admin_reset_password)
                                },
                            enabled =
                                if (user.role == UserRole.ADMIN) {
                                    (currentPassword.isNotBlank() && resetPasswords[user.id]?.isNotBlank() == true)
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
                                    viewModel.changePassword(
                                        user.username,
                                        currentPassword.toCharArray(),
                                        resetPassword.toCharArray(),
                                    )
                                    currentPassword = ""
                                    resetPasswords -= user.id
                                } else {
                                    onRequestConfirm(
                                        confirmResetTitle,
                                        confirmResetTextTemplate.format(user.displayName),
                                    ) {
                                        viewModel.resetPassword(
                                            user.id,
                                            resetPassword.toCharArray(),
                                        )
                                        resetPasswords -= user.id
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        AppButton(
            text = stringResource(R.string.admin_close),
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Default,
            modifier = Modifier.fillMaxWidth(),
            onClick = onBack,
        )
    }
}

private data class PermissionUIItem(
    val permission: UserPermission,
    val label: String,
)
