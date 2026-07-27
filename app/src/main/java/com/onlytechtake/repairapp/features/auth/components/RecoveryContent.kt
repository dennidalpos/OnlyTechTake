package com.onlytechtake.repairapp.features.auth.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppTopBar
import com.onlytechtake.repairapp.features.auth.AuthFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryContent(
    formState: AuthFormState,
    onRecoveryCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.auth_recovery_top),
                navigationIcon = {
                    AppIconButton(
                        onClick = onCancel,
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                },
            )
        },
    ) { paddingValues ->
        AuthForm(
            title = stringResource(R.string.auth_recovery_title),
            subtitle = stringResource(R.string.auth_recovery_subtitle),
            formState = formState,
            paddingValues = paddingValues,
            submitLabel = stringResource(R.string.auth_reset_admin_password),
            onDisplayNameChange = {},
            onUsernameChange = {},
            onPasswordChange = onPasswordChange,
            onConfirmPasswordChange = onConfirmPasswordChange,
            onRecoveryCodeChange = onRecoveryCodeChange,
            onSubmit = onSubmit,
            showDisplayName = false,
            showConfirmPassword = true,
            showUsername = false,
            showRecoveryCode = true,
        )
    }
}
