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
fun LoginContent(
    formState: AuthFormState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDemoLogin: () -> Unit,
    onRecovery: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                navigationIcon = {
                    AppIconButton(
                        onClick = onBack,
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.auth_close_app),
                    )
                },
            )
        },
    ) { paddingValues ->
        AuthForm(
            title = stringResource(R.string.auth_login_title),
            subtitle = null,
            formState = formState,
            paddingValues = paddingValues,
            submitLabel = stringResource(R.string.auth_login_submit),
            onDisplayNameChange = {},
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onConfirmPasswordChange = {},
            onSubmit = onSubmit,
            onDemoLogin = onDemoLogin,
            onRecovery = onRecovery,
            showDisplayName = false,
            showConfirmPassword = false,
        )
    }
}
