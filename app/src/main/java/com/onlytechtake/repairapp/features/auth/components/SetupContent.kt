package com.onlytechtake.repairapp.features.auth.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.PasswordVisibilityButton
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.features.auth.AuthFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupContent(
    formState: AuthFormState,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRestore: (Uri, String?) -> Unit,
    onBack: () -> Unit,
) {
    var showRestoreDialog by remember { mutableStateOf(value = false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(value = false) }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                restoreUri = it
                showRestoreDialog = true
            }
        }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.auth_setup_top),
                navigationIcon = {
                    AppIconButton(
                        onClick = onBack,
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                },
                actions = {
                    AppIconButton(
                        onClick = { restoreLauncher.launch("application/zip") },
                        imageVector = Icons.Default.Backup,
                        contentDescription = stringResource(R.string.config_hub_backup_title),
                    )
                },
            )
        },
    ) { paddingValues ->
        AuthForm(
            title = stringResource(R.string.auth_setup_title),
            subtitle = stringResource(R.string.auth_setup_subtitle),
            formState = formState,
            paddingValues = paddingValues,
            submitLabel = stringResource(R.string.auth_create_admin),
            onDisplayNameChange = onDisplayNameChange,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onConfirmPasswordChange = onConfirmPasswordChange,
            onSubmit = onSubmit,
            showDisplayName = true,
            showConfirmPassword = true,
        )
    }

    if (showRestoreDialog) {
        AppDialog(
            onDismissRequest = {
                showRestoreDialog = false
                restoreUri = null
                restorePassword = ""
            },
            title = stringResource(R.string.action_restore_confirm_title),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)) {
                    Text(
                        text = stringResource(R.string.action_restore_confirm_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AppTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = stringResource(R.string.auth_password_backup),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        trailingIcon = {
                            PasswordVisibilityButton(
                                visible = passwordVisible,
                                onVisibleChange = { passwordVisible = it },
                            )
                        },
                    )
                }
            },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.action_confirm),
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    onClick = {
                        restoreUri?.let { uri ->
                            onRestore(
                                uri,
                                restorePassword.takeIf { it.isNotBlank() },
                            )
                        }
                        showRestoreDialog = false
                        restoreUri = null
                        restorePassword = ""
                    },
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = {
                        showRestoreDialog = false
                        restoreUri = null
                        restorePassword = ""
                    },
                )
            },
        )
    }
}
