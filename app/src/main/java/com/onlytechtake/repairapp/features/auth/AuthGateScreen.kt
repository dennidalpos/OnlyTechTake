package com.onlytechtake.repairapp.features.auth

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.features.dashboard.DashboardRoute

@Composable
fun AuthGateRoute() {
    val container = LocalAppContainer.current
    val viewModel: AuthGateViewModel = viewModel(
        factory = AuthGateViewModel.Factory(container.authRepository),
    )
    val authState by viewModel.authState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    LaunchedEffect(authState.lastUsername) {
        if (formState.username.isBlank() && authState.lastUsername.isNotBlank()) {
            viewModel.updateUsername(authState.lastUsername)
        }
    }

    when {
        authState.currentUser != null -> DashboardRoute(onLogout = viewModel::logout)
        !authState.configured -> FirstSetupScreen(
            formState = formState,
            onDisplayNameChange = viewModel::updateDisplayName,
            onUsernameChange = viewModel::updateUsername,
            onPasswordChange = viewModel::updatePassword,
            onConfirmPasswordChange = viewModel::updateConfirmPassword,
            onSubmit = viewModel::createFirstAdmin,
        )
        else -> LoginScreen(
            formState = formState,
            onUsernameChange = viewModel::updateUsername,
            onPasswordChange = viewModel::updatePassword,
            onSubmit = viewModel::login,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstSetupScreen(
    formState: AuthFormState,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Configurazione admin") })
        },
    ) { paddingValues ->
        AuthForm(
            title = "Primo avvio",
            subtitle = "Crea l'account admin locale per proteggere l'app.",
            formState = formState,
            paddingValues = paddingValues,
            submitLabel = "Crea admin",
            onDisplayNameChange = onDisplayNameChange,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onConfirmPasswordChange = onConfirmPasswordChange,
            onSubmit = onSubmit,
            showDisplayName = true,
            showConfirmPassword = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(
    formState: AuthFormState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "OnlyTechTake") })
        },
    ) { paddingValues ->
        AuthForm(
            title = "Login",
            subtitle = "Accedi con un utente admin o operatore.",
            formState = formState,
            paddingValues = paddingValues,
            submitLabel = "Accedi",
            onDisplayNameChange = {},
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onConfirmPasswordChange = {},
            onSubmit = onSubmit,
            showDisplayName = false,
            showConfirmPassword = false,
        )
    }
}

@Composable
private fun AuthForm(
    title: String,
    subtitle: String,
    formState: AuthFormState,
    paddingValues: PaddingValues,
    submitLabel: String,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    showDisplayName: Boolean,
    showConfirmPassword: Boolean,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        val compactHeight = maxHeight < 420.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = if (compactHeight) 8.dp else 16.dp)
                .widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) 8.dp else 12.dp),
        ) {
            Text(
                text = title,
                style = if (compactHeight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)

            if (showDisplayName) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text(text = "Nome admin") },
                    singleLine = true,
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formState.username,
                onValueChange = onUsernameChange,
                label = { Text(text = "Username") },
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formState.password,
                onValueChange = onPasswordChange,
                label = { Text(text = "Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )

            if (showConfirmPassword) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text(text = "Conferma password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }

            formState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.loading,
                onClick = onSubmit,
            ) {
                if (formState.loading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(text = "Operazione in corso")
                    }
                } else {
                    Text(text = submitLabel)
                }
            }
        }
    }
}
