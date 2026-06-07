package com.onlytechtake.repairapp.features.auth

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.PasswordVisibilityButton
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
        !authState.initialized -> AuthLoadingScreen()
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

@Composable
private fun AuthLoadingScreen() {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = "Caricamento autenticazione"
                },
            )
        }
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
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nextAction = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val submitAction = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSubmit()
    }

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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = nextAction,
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formState.username,
                onValueChange = onUsernameChange,
                label = { Text(text = "Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = nextAction,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formState.password,
                onValueChange = onPasswordChange,
                label = { Text(text = "Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (showConfirmPassword) ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = {
                        if (!showConfirmPassword) {
                            submitAction()
                        }
                    },
                ),
                trailingIcon = {
                    PasswordVisibilityButton(
                        visible = passwordVisible,
                        onVisibleChange = { passwordVisible = it },
                    )
                },
            )

            if (showConfirmPassword) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text(text = "Conferma password") },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitAction() }),
                    trailingIcon = {
                        PasswordVisibilityButton(
                            visible = confirmPasswordVisible,
                            onVisibleChange = { confirmPasswordVisible = it },
                        )
                    },
                )
            }

            formState.errorMessage?.let { errorMessage ->
                Text(
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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
