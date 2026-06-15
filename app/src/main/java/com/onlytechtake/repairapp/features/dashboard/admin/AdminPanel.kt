package com.onlytechtake.repairapp.features.dashboard.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.PasswordVisibilityButton
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone

@Composable
fun AdminPanel(
    viewModel: AdminViewModel,
    isAdmin: Boolean,
    onResetComplete: () -> Unit,
) {
    val users by viewModel.users.collectAsState()
    val recoveryCode by viewModel.recoveryCode.collectAsState()
    val isBackupReminderDue by viewModel.isBackupReminderDue.collectAsState()

    val confirmCreateTitle = stringResource(R.string.admin_confirm_create_title)
    val confirmCreateText = stringResource(R.string.admin_confirm_create_text)
    val confirmStatusTitle = stringResource(R.string.admin_confirm_status_title)
    val confirmStatusTextTemplate = stringResource(R.string.admin_confirm_status_text)
    val confirmResetTitle = stringResource(R.string.admin_confirm_reset_title)
    val confirmResetTextTemplate = stringResource(R.string.admin_confirm_reset_text)
    val confirmTotalResetTitle = stringResource(R.string.admin_confirm_total_reset_title)
    val confirmTotalResetText = stringResource(R.string.admin_confirm_total_reset_text)
    val confirmDeleteAllOrdersTitle = stringResource(R.string.admin_confirm_delete_all_orders_title)

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
        Text(modifier = Modifier.padding(DashboardSpacing.screenPadding), text = stringResource(R.string.admin_only_section))
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = DashboardSpacing.maxContentWidth),
            contentPadding = PaddingValues(DashboardSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
        ) {
            item {
                SectionTitle("Accessi e Sicurezza")
                StandardCard(highlight = true) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Text("Crea Operatore", style = MaterialTheme.typography.titleMedium)
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
                                    modifier = Modifier.focusProperties { canFocus = false },
                                )
                            },
                        )
                        TouchButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Crea",
                            enabled = displayName.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
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
            }
            item {
                StandardCard(highlight = true) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        SectionTitle(
                            text = "Kit di Recupero Admin",
                            tooltip = "Il kit di recupero permette di resettare la password admin se dimenticata. Conservalo in un luogo sicuro fuori dal dispositivo."
                        )
                        
                        if (recoveryCode == null) {
                            TouchButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Genera Nuovo Kit di Recupero",
                                tone = ButtonTone.NEUTRAL,
                                onClick = { viewModel.generateRecoveryKit() }
                            )
                        } else {
                            StandardCard {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("CODICE MASTER:", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = recoveryCode!!,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Scrivi questo codice e conservalo offline. Scomparirà al riavvio della schermata.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    TouchButton(
                                        text = "Fatto, nascondi codice",
                                        onClick = { viewModel.clearRecoveryCode() }
                                    )
                                }
                            }
                        }
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
                    var resetPasswordVisible by rememberSaveable(user.id) { mutableStateOf(false) }
                    var currentPassword by rememberSaveable(user.id) { mutableStateOf("") }
                    var currentPasswordVisible by rememberSaveable(user.id) { mutableStateOf(false) }

                    StandardCard {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                        ) {
                            Text("${user.displayName} - ${user.username} (${user.role.name})", style = MaterialTheme.typography.titleMedium)
                            
                            if (user.role != UserRole.ADMIN) {
                                Text(if (user.active) stringResource(R.string.admin_user_active) else stringResource(R.string.admin_user_inactive))
                                HorizontalDivider()
                                Text("Aree e Permessi", style = MaterialTheme.typography.labelLarge)
                                
                                val permissionGroups = listOf(
                                    PermissionUIItem(UserPermission.CREATE_ORDERS, "Creazione commesse"),
                                    PermissionUIItem(UserPermission.EDIT_OWN_ORDERS, "Modifica proprie commesse"),
                                    PermissionUIItem(UserPermission.EDIT_ALL_ORDERS, "Modifica tutte le commesse"),
                                    PermissionUIItem(UserPermission.CLOSE_ORDERS, "Chiusura commesse"),
                                    PermissionUIItem(UserPermission.MANAGE_CUSTOMERS, "Gestione anagrafica clienti"),
                                    PermissionUIItem(UserPermission.MANAGE_CATEGORIES, "Gestione catalogo categorie"),
                                    PermissionUIItem(UserPermission.MANAGE_TEMPLATES, "Gestione template messaggi"),
                                    PermissionUIItem(UserPermission.MANAGE_PAYMENTS, "Gestione pagamenti"),
                                    PermissionUIItem(UserPermission.EXPORT_DATA, "Esportazione dati (Excel/PDF)"),
                                    PermissionUIItem(UserPermission.MANAGE_STORAGE, "Pulizia cache e statistiche"),
                                )

                                permissionGroups.forEach { item ->
                                    val hasPermission = user.permissions.contains(item.permission)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val newPermissions = if (hasPermission) user.permissions - item.permission else user.permissions + item.permission
                                            viewModel.setUserPermissions(user.id, newPermissions)
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.label, style = MaterialTheme.typography.bodySmall)
                                        Switch(
                                            checked = hasPermission,
                                            onCheckedChange = { checked ->
                                                val newPermissions = if (checked) user.permissions + item.permission else user.permissions - item.permission
                                                viewModel.setUserPermissions(user.id, newPermissions)
                                            },
                                            modifier = Modifier.scale(0.7f)
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }

                            if (user.role == UserRole.ADMIN) {
                                Field(
                                    value = currentPassword,
                                    onValueChange = { currentPassword = it },
                                    label = "Password Attuale",
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

                            Field(
                                value = resetPasswords[user.id].orEmpty(),
                                onValueChange = { value ->
                                    resetPasswords += (user.id to value)
                                },
                                label = if (user.role == UserRole.ADMIN) "Nuova Password" else stringResource(R.string.admin_field_temp_password),
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
                                if (user.role != UserRole.ADMIN) {
                                    TouchButton(
                                        text = if (user.active) stringResource(R.string.admin_user_deactivate) else stringResource(R.string.admin_user_activate),
                                        tone = if (user.active) ButtonTone.DANGER else ButtonTone.SUCCESS,
                                        onClick = {
                                            requestConfirm(confirmStatusTitle, confirmStatusTextTemplate.format(user.displayName)) {
                                                viewModel.setUserActive(user.id, !user.active)
                                            }
                                        },
                                    )
                                }
                                TouchButton(
                                    text = if (user.role == UserRole.ADMIN) "Cambia Password" else stringResource(R.string.admin_reset_password),
                                    enabled = if (user.role == UserRole.ADMIN) {
                                        currentPassword.isNotBlank() && resetPasswords[user.id]?.isNotBlank() == true
                                    } else {
                                        resetPasswords[user.id]?.isNotBlank() == true
                                    },
                                    tone = ButtonTone.NEUTRAL,
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
            }

            if (isBackupReminderDue) {
                item {
                    StandardCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Column(modifier = Modifier.padding(DashboardSpacing.cardPadding)) {
                            Text(
                                text = "ATTENZIONE: Nessun backup recente trovato!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Si consiglia vivamente di eseguire un backup completo prima di procedere con operazioni di cancellazione dati.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
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
                        SectionTitle(
                            text = stringResource(R.string.admin_delete_all_orders),
                            tooltip = "Tutte le commesse, timeline, messaggi, foto, firme e documenti verranno eliminati permanentemente."
                        )
                        Field(
                            value = ordersResetConfirmation,
                            onValueChange = { ordersResetConfirmation = it },
                            label = "Digita CANCELLA TUTTO"
                        )
                        TouchButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.admin_delete_all_orders),
                            enabled = ordersResetConfirmation == "CANCELLA TUTTO",
                            tone = ButtonTone.WARNING,
                            onClick = {
                                requestConfirm(
                                    confirmDeleteAllOrdersTitle,
                                    "Stai per eliminare tutte le commesse. Questa azione non è reversibile."
                                ) {
                                    viewModel.resetOrders(ordersResetConfirmation) {
                                        ordersResetConfirmation = ""
                                    }
                                }
                            }
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
                        SectionTitle(
                            text = stringResource(R.string.admin_total_reset),
                            tooltip = "Il reset totale rimuoverà account, clienti, configurazioni e tutte le commesse. L'app tornerà allo stato iniziale."
                        )
                        Field(resetConfirmation, { resetConfirmation = it }, "Digita RESET")
                        TouchButton(
                            modifier = Modifier.fillMaxWidth(),
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
                TouchButton(
                    text = stringResource(R.string.customer_cancel),
                    tone = ButtonTone.NEUTRAL,
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
