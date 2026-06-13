package com.onlytechtake.repairapp.features.dashboard.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.PasswordVisibilityButton
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.ButtonTone

@Composable
fun AdminPanel(
    viewModel: WorkspaceViewModel,
    isAdmin: Boolean,
    onResetComplete: () -> Unit,
) {
    val users by viewModel.users.collectAsState()
    val uriHandler = LocalUriHandler.current

    val confirmCreateTitle = stringResource(R.string.admin_confirm_create_title)
    val confirmCreateText = stringResource(R.string.admin_confirm_create_text)
    val confirmStatusTitle = stringResource(R.string.admin_confirm_status_title)
    val confirmStatusTextTemplate = stringResource(R.string.admin_confirm_status_text)
    val confirmResetTitle = stringResource(R.string.admin_confirm_reset_title)
    val confirmResetTextTemplate = stringResource(R.string.admin_confirm_reset_text)
    val confirmTotalResetTitle = stringResource(R.string.admin_confirm_total_reset_title)
    val confirmTotalResetText = stringResource(R.string.admin_confirm_total_reset_text)
    val confirmArchiveTitle = stringResource(R.string.admin_confirm_archive_title)
    val confirmArchiveText = stringResource(R.string.admin_confirm_archive_text)
    val confirmDeleteTitle = stringResource(R.string.admin_confirm_delete_title)
    val confirmDeleteText = stringResource(R.string.admin_confirm_delete_text)

    var displayName by rememberSaveable { mutableStateOf(value = "") }
    var username by rememberSaveable { mutableStateOf(value = "") }
    var password by rememberSaveable { mutableStateOf(value = "") }
    var passwordVisible by rememberSaveable { mutableStateOf(value = false) }
    
    // Use remember for maps to avoid parcelable issues with rememberSaveable
    var resetPasswords by remember { mutableStateOf(value = emptyMap<String, String>()) }
    
    var resetConfirmation by rememberSaveable { mutableStateOf(value = "") }
    var archiveConfirmation by rememberSaveable { mutableStateOf(value = "") }
    var deleteConfirmation by rememberSaveable { mutableStateOf(value = "") }
    
    var confirmTitle by remember { mutableStateOf<String?>(value = null) }
    var confirmText by remember { mutableStateOf(value = "") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(value = null) }

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    if (!isAdmin) {
        Text(modifier = Modifier.padding(DashboardSpacing.screenPadding), text = stringResource(R.string.admin_only_section))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle(stringResource(R.string.dashboard_admin))
            StandardCard(highlight = true) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text(stringResource(R.string.admin_create_operator), style = MaterialTheme.typography.titleMedium)
                    Field(displayName, { displayName = it }, stringResource(R.string.customer_field_first_name))
                    Field(username, { username = it }, "Username")
                    Field(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.admin_field_temp_password),
                        singleLine = true,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            PasswordVisibilityButton(
                                visible = passwordVisible,
                                onVisibleChange = { passwordVisible = it },
                                modifier = Modifier.focusProperties { canFocus = false }
                            )
                        },
                    )
                    TouchButton(
                        text = stringResource(R.string.admin_create_operator),
                        enabled = displayName.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                        onClick = {
                            requestConfirm(confirmCreateTitle, confirmCreateText) {
                                viewModel.createOperator(displayName, username, password)
                                displayName = ""
                                username = ""
                                password = ""
                                passwordVisible = false
                            }
                        },
                    )
                }
            }
        }
        item { SectionTitle(stringResource(R.string.admin_users_title)) }
        if (users.isEmpty()) {
            item {
                StandardCard {
                    Column(modifier = Modifier.padding(DashboardSpacing.cardPadding)) {
                        Text(stringResource(R.string.admin_no_users), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(users, key = { it.id }) { user ->
                var resetPasswordVisible by rememberSaveable(user.id) { mutableStateOf(value = false) }
                StandardCard {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Text("${user.displayName} - ${user.username} (${user.role.name})", style = MaterialTheme.typography.titleMedium)
                        Text(if (user.active) stringResource(R.string.admin_user_active) else stringResource(R.string.admin_user_inactive))
                        Field(
                            value = resetPasswords[user.id].orEmpty(),
                            onValueChange = { value ->
                                resetPasswords += (user.id to value)
                            },
                            label = stringResource(R.string.admin_field_temp_password),
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
                        ActionRow {
                            TouchButton(
                                text = if (user.active) stringResource(R.string.admin_user_deactivate) else stringResource(R.string.admin_user_activate),
                                tone = if (user.active) ButtonTone.DANGER else ButtonTone.SUCCESS,
                                onClick = {
                                    requestConfirm(confirmStatusTitle, confirmStatusTextTemplate.format(user.displayName)) {
                                        viewModel.setUserActive(user.id, !user.active)
                                    }
                                },
                            )
                            TouchButton(
                                text = stringResource(R.string.admin_reset_password),
                                enabled = resetPasswords[user.id]?.isNotBlank() == true,
                                tone = ButtonTone.NEUTRAL,
                                onClick = {
                                    val resetPassword = resetPasswords[user.id].orEmpty()
                                    requestConfirm(confirmResetTitle, confirmResetTextTemplate.format(user.displayName)) {
                                        viewModel.resetPassword(user.id, resetPassword)
                                        resetPasswords -= user.id
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            StandardCard {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text(stringResource(R.string.admin_total_reset), style = MaterialTheme.typography.titleMedium)
                    Field(resetConfirmation, { resetConfirmation = it }, "Digita RESET")
                    TouchButton(
                        text = stringResource(R.string.admin_total_reset),
                        enabled = resetConfirmation == "RESET",
                        tone = ButtonTone.DANGER,
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
        item {
            StandardCard {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text(stringResource(R.string.admin_cleanup_orders), style = MaterialTheme.typography.titleMedium)
                    Field(archiveConfirmation, { archiveConfirmation = it }, "Digita ARCHIVIA")
                    TouchButton(
                        text = stringResource(R.string.admin_archive_discarded),
                        enabled = archiveConfirmation == "ARCHIVIA",
                        tone = ButtonTone.WARNING,
                        onClick = {
                            requestConfirm(confirmArchiveTitle, confirmArchiveText) {
                                viewModel.archiveDiscardedClosedOrders()
                                archiveConfirmation = ""
                            }
                        },
                    )
                    Field(deleteConfirmation, { deleteConfirmation = it }, "Digita CANCELLA")
                    TouchButton(
                        text = stringResource(R.string.admin_delete_archived),
                        enabled = deleteConfirmation == "CANCELLA",
                        tone = ButtonTone.DANGER,
                        onClick = {
                            requestConfirm(confirmDeleteTitle, confirmDeleteText) {
                                viewModel.deleteArchivedOrders()
                                deleteConfirmation = ""
                            }
                        },
                    )
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.admin_author))
            StandardCard {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text("Danny Perondi", style = MaterialTheme.typography.titleMedium)
                    TouchButton(
                        text = "GitHub Repository",
                        onClick = { uriHandler.openUri("https://github.com/dennidalpos/OnlyTechTake") },
                        tone = ButtonTone.NEUTRAL,
                    )
                }
            }
        }
    }

    confirmTitle?.let { title ->
        AlertDialog(
            onDismissRequest = {
                confirmTitle = null
                confirmAction = null
            },
            title = { Text(title) },
            text = { Text(confirmText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction?.invoke()
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text(stringResource(R.string.customer_cancel))
                }
            },
        )
    }
}
