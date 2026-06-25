package com.onlytechtake.repairapp.features.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.design.LoadingBrandIndicator
import com.onlytechtake.repairapp.core.design.PasswordVisibilityButton
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppErrorText
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.AppTopBar
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.features.dashboard.DashboardRoute

@Composable
fun AuthGateRoute(
    appInstanceKey: Int,
    onAppDataRestored: () -> Unit,
) {
    var showcaseSeedRequested by rememberSaveable(appInstanceKey) { mutableStateOf(false) }
    val container = LocalAppContainer.current
    val viewModel: AuthGateViewModel =
        viewModel(
            key = "auth_gate_$appInstanceKey",
            factory = AuthGateViewModel.Factory(
                container.context,
                container.authRepository,
                container.dataSeeder
            ),
        )
    val authState by viewModel.authState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState.lastUsername) {
        if (formState.username.isBlank() && authState.lastUsername.isNotBlank()) {
            viewModel.updateUsername(authState.lastUsername)
        }
    }

    if (BuildConfig.IS_SHOWCASE_MODE_ENABLED) {
        if (authState.initialized && !authState.configured) {
            val appCtx = context.applicationContext
            LaunchedEffect(onAppDataRestored, viewModel) {
                if (!showcaseSeedRequested) {
                    showcaseSeedRequested = true
                    viewModel.seedShowcaseData(appCtx) {
                        onAppDataRestored()
                    }
                }
            }
        }
    }

    if (BuildConfig.IS_SHOWCASE_MODE_ENABLED) {
        if (authState.initialized && authState.configured && (authState.currentUser == null)) {
            LaunchedEffect(Unit) {
                viewModel.loginShowcaseAdmin()
            }
        }
    }

    val isShowcasePreparing =
        if (BuildConfig.IS_SHOWCASE_MODE_ENABLED) !authState.configured else false
    when {
        authState.initializationError != null ->
            AuthErrorScreen(
                error = authState.initializationError!!,
                onRetry = onAppDataRestored,
            )

        !authState.initialized || isShowcasePreparing || formState.seeding ->
            AuthLoadingScreen(
                text =
                    when {
                        formState.seeding -> stringResource(R.string.auth_showcase_initializing)
                        isShowcasePreparing ->
                            stringResource(
                                R.string.auth_showcase_preparing,
                            )

                        else -> stringResource(R.string.auth_system_ready)
                    },
            )

        authState.currentUser != null ->
            DashboardRoute(
                appInstanceKey = appInstanceKey,
                onLogout = viewModel::logout,
                onAppDataRestored = onAppDataRestored,
            )

        formState.passwordChangeRequired ->
            MandatoryPasswordChangeScreen(
                formState = formState,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::changePassword,
                onCancel = viewModel::cancelPasswordChange,
            )

        formState.recoveryMode ->
            AdminRecoveryScreen(
                formState = formState,
                onRecoveryCodeChange = viewModel::updateRecoveryCode,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::recoverAdmin,
                onCancel = viewModel::cancelRecovery,
            )

        !authState.configured ->
            FirstSetupScreen(
                formState = formState,
                onDisplayNameChange = viewModel::updateDisplayName,
                onUsernameChange = viewModel::updateUsername,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::createFirstAdmin,
            ) { (context as? Activity)?.finish() }

        else ->
            LoginScreen(
                formState = formState,
                onUsernameChange = viewModel::updateUsername,
                onPasswordChange = viewModel::updatePassword,
                onSubmit = viewModel::login,
                onDemoLogin = viewModel::loginDemo,
                onRecovery = viewModel::startRecovery,
            ) { (context as? Activity)?.finish() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MandatoryPasswordChangeScreen(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminRecoveryScreen(
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
                        contentDescription = stringResource(R.string.content_description_back),
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

@Composable
private fun AuthLoadingScreen(text: String) {
    Scaffold { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            LoadingBrandIndicator(
                text = text,
                contentDescription = text,
            )
        }
    }
}

@Composable
private fun AuthErrorScreen(
    error: String,
    onRetry: () -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(RepairAppSpacing.screenHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Errore di Inizializzazione",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(RepairAppSpacing.compactGap))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(RepairAppSpacing.sectionGap))
            AppButton(
                text = "Riprova",
                onClick = onRetry,
                variant = AppButtonVariant.Primary,
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
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.auth_setup_top),
                navigationIcon = {
                    AppIconButton(
                        onClick = onBack,
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_description_back),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(
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

@Composable
private fun AuthForm(
    title: String,
    subtitle: String?,
    formState: AuthFormState,
    paddingValues: PaddingValues,
    submitLabel: String,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRecoveryCodeChange: (String) -> Unit = {},
    onSubmit: () -> Unit,
    onDemoLogin: (() -> Unit)? = null,
    onRecovery: (() -> Unit)? = null,
    showDisplayName: Boolean,
    showConfirmPassword: Boolean,
    showUsername: Boolean = true,
    showRecoveryCode: Boolean = false,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(value = false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(value = false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nextAction = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val submitAction = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSubmit()
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compactHeight = maxHeight < 420.dp

        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .widthIn(max = RepairAppSpacing.maxContentWidthMedium)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(
                        horizontal = RepairAppSpacing.screenHorizontal,
                        vertical = if (compactHeight) RepairAppSpacing.compactGap else RepairAppSpacing.screenHorizontal,
                    ),
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) RepairAppSpacing.compactGap else RepairAppSpacing.fieldGap),
        ) {
            Text(
                text = title,
                style = if (compactHeight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showDisplayName) {
                AppTextField(
                    value = formState.displayName,
                    onValueChange = onDisplayNameChange,
                    label = stringResource(R.string.auth_admin_name),
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    keyboardActions = nextAction,
                )
            }

            if (showUsername) {
                AppTextField(
                    value = formState.username,
                    onValueChange = onUsernameChange,
                    label = stringResource(R.string.auth_username),
                    singleLine = true,
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                    keyboardActions = nextAction,
                    testTag = RepairAppTestTags.AuthUsernameField,
                )
            }

            if (showRecoveryCode) {
                AppTextField(
                    value = formState.recoveryCode,
                    onValueChange = onRecoveryCodeChange,
                    label = stringResource(R.string.auth_recovery_code),
                    singleLine = true,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next,
                    keyboardActions = nextAction,
                )
            }

            AppTextField(
                value = formState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_password),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                imeAction = if (showConfirmPassword) ImeAction.Next else ImeAction.Done,
                keyboardActions =
                    KeyboardActions(
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
                        modifier = Modifier.focusProperties { canFocus = false },
                    )
                },
                testTag = RepairAppTestTags.AuthPasswordField,
            )

            if (showConfirmPassword) {
                AppTextField(
                    value = formState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = stringResource(R.string.auth_confirm_password),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { submitAction() }),
                    trailingIcon = {
                        PasswordVisibilityButton(
                            visible = confirmPasswordVisible,
                            onVisibleChange = { confirmPasswordVisible = it },
                            modifier = Modifier.focusProperties { canFocus = false },
                        )
                    },
                )
            }

            formState.errorMessage?.let { errorMessage ->
                AppErrorText(
                    text = errorMessage,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            AppButton(
                text = submitLabel,
                onClick = submitAction,
                enabled = !formState.loading,
                loading = formState.loading,
                modifier = Modifier.fillMaxWidth(),
                testTag = RepairAppTestTags.AuthSubmitButton,
            )

            onRecovery?.let {
                AppButton(
                    text = stringResource(R.string.auth_recovery_action),
                    onClick = it,
                    enabled = !formState.loading,
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if ((onDemoLogin != null) && DemoMode.canEnable()) {
                AppButton(
                    text = stringResource(R.string.auth_demo_action),
                    onClick = onDemoLogin,
                    enabled = !formState.loading,
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
