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
fun MandatoryPasswordChangeContent(
    formState: AuthFormState,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.auth_password_change_top),
                navigationIcon = {
                    AppIconButton(
                        onClick = onCancel,
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.customer_cancel),
                    )
                },
            )
        },
    ) { paddingValues ->
        AuthForm(
            title = stringResource(R.string.auth_security_title),
            subtitle = stringResource(R.string.auth_security_subtitle),
            formState = formState,
            paddingValues = paddingValues,
            submitLabel = stringResource(R.string.auth_update_password),
            onDisplayNameChange = {},
            onUsernameChange = {},
            onPasswordChange = onPasswordChange,
            onConfirmPasswordChange = onConfirmPasswordChange,
            onSubmit = onSubmit,
            showDisplayName = false,
            showConfirmPassword = true,
            showUsername = false,
        )
    }
}
