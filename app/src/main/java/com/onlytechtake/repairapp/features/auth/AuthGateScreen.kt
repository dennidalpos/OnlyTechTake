package com.onlytechtake.repairapp.features.auth

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.Preview
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
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppErrorText
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.AppTopBar
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo
import com.onlytechtake.repairapp.features.dashboard.DashboardRoute

@Composable
fun AuthGateRoute(
    appInstanceKey: Int,
    onAppDataRestored: () -> Unit,
    disableAutoSeed: Boolean = false,
) {
    var showcaseSeedRequested by remember(appInstanceKey) { mutableStateOf(value = false) }
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: AuthGateViewModel =
        viewModel(
            key = "auth_gate_$appInstanceKey",
            factory =
                AuthGateViewModel.Factory(
                    context,
                    container.authRepository,
                    container.ownerPreferences,
                    container.backupRepository,
                    container.dataSeeder,
                ),
        )
    val authState by viewModel.authState.collectAsState()
    val ownerInfo by viewModel.ownerInfo.collectAsState()
    val formState by viewModel.formState.collectAsState()

    LaunchedEffect(authState.lastUsername) {
        if (formState.username.isBlank() && authState.lastUsername.isNotBlank()) {
            viewModel.updateUsername(authState.lastUsername)
        }
    }

    if (BuildConfig.IS_SHOWCASE_MODE_ENABLED && !disableAutoSeed) {
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

    if (BuildConfig.IS_SHOWCASE_MODE_ENABLED && !disableAutoSeed) {
        if (authState.initialized && authState.configured && (authState.currentUser == null)) {
            LaunchedEffect(Unit) {
                viewModel.loginShowcaseAdmin()
            }
        }
    }

    val isShowcasePreparing =
        if (BuildConfig.IS_SHOWCASE_MODE_ENABLED) !authState.configured else false
    when {
        authState.initializationError != null -> {
            AuthErrorScreen(
                error = authState.initializationError!!,
                onRetry = onAppDataRestored,
            )
        }

        !authState.initialized || isShowcasePreparing || formState.seeding -> {
            val context = LocalContext.current
            AuthLoadingScreen(
                text =
                    when {
                        formState.seeding -> DemoMode.getShowcaseInitializingMessage(context)
                        isShowcasePreparing -> DemoMode.getShowcasePreparingMessage(context)
                        else -> stringResource(R.string.auth_system_ready)
                    },
            )
        }

        (authState.currentUser != null) && !ownerInfo.isComplete() -> {
            OwnerWizardScreen(
                ownerInfo = ownerInfo,
                onSubmit = viewModel::updateOwnerInfo,
            )
        }

        authState.currentUser != null -> {
            DashboardRoute(
                appInstanceKey = appInstanceKey,
                onLogout = viewModel::logout,
                onAppDataRestored = onAppDataRestored,
            )
        }

        formState.passwordChangeRequired -> {
            MandatoryPasswordChangeScreen(
                formState = formState,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::changePassword,
                onCancel = viewModel::cancelPasswordChange,
            )
        }

        formState.recoveryMode -> {
            AdminRecoveryScreen(
                formState = formState,
                onRecoveryCodeChange = viewModel::updateRecoveryCode,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::recoverAdmin,
                onCancel = viewModel::cancelRecovery,
            )
        }

        !authState.configured -> {
            FirstSetupScreen(
                formState = formState,
                onDisplayNameChange = viewModel::updateDisplayName,
                onUsernameChange = viewModel::updateUsername,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::createFirstAdmin,
                onRestore = { uri, password ->
                    viewModel.restoreBackup(uri, password, onAppDataRestored)
                },
            ) { (context as? Activity)?.finish() }
        }

        else -> {
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
                text = stringResource(R.string.error_init_title),
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
                text = stringResource(R.string.action_retry),
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
                    testTag = RepairAppTestTags.AuthDisplayNameField,
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
                    testTag = RepairAppTestTags.AuthRecoveryCodeField,
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
                    testTag = RepairAppTestTags.AuthConfirmPasswordField,
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
                    text = DemoMode.getDemoActionLabel(LocalContext.current),
                    onClick = onDemoLogin,
                    enabled = !formState.loading,
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerWizardScreen(
    ownerInfo: OwnerInfo,
    onSubmit: (OwnerInfo) -> Unit,
) {
    val context = LocalContext.current
    // Pre-fill with showcase data if empty and showcase mode is enabled
    val showcaseInfo =
        if (BuildConfig.IS_SHOWCASE_MODE_ENABLED) DemoMode.getShowcaseOwnerInfo(context) else null

    val initialName =
        if ((showcaseInfo != null) && ownerInfo.name.isBlank()) {
            showcaseInfo.name
        } else {
            ownerInfo.name
        }
    val initialVat =
        if ((showcaseInfo != null) && ownerInfo.vatNumber.isBlank()) {
            showcaseInfo.vatNumber
        } else {
            ownerInfo.vatNumber
        }
    val initialAddress =
        if ((showcaseInfo != null) && ownerInfo.address.isBlank()) {
            showcaseInfo.address
        } else {
            ownerInfo.address
        }
    val initialEmail =
        if ((showcaseInfo != null) && ownerInfo.email.isBlank()) {
            showcaseInfo.email
        } else {
            ownerInfo.email
        }
    val initialPhone =
        if ((showcaseInfo != null) && ownerInfo.phone.isBlank()) {
            showcaseInfo.phone
        } else {
            ownerInfo.phone
        }

    var name by rememberSaveable { mutableStateOf(initialName) }
    var vatNumber by rememberSaveable { mutableStateOf(initialVat) }
    var address by rememberSaveable { mutableStateOf(initialAddress) }
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.config_hub_owner_title),
            )
        },
    ) { paddingValues ->
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
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.owner_field_name),
                    required = true,
                    imeAction = ImeAction.Next,
                    testTag = RepairAppTestTags.OwnerFieldName,
                )
                AppTextField(
                    value = vatNumber,
                    onValueChange = { vatNumber = it },
                    label = stringResource(R.string.owner_field_vat),
                    required = true,
                    imeAction = ImeAction.Next,
                    testTag = RepairAppTestTags.OwnerFieldVat,
                )
                AppTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = stringResource(R.string.owner_field_address),
                    required = true,
                    imeAction = ImeAction.Next,
                    testTag = RepairAppTestTags.OwnerFieldAddress,
                )
                AppTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.owner_field_email),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    testTag = RepairAppTestTags.OwnerFieldEmail,
                )
                AppTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.owner_field_phone),
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    testTag = RepairAppTestTags.OwnerFieldPhone,
                )

                Spacer(Modifier.height(RepairAppSpacing.sectionGap))

                AppButton(
                    text = stringResource(R.string.action_confirm),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && vatNumber.isNotBlank() && address.isNotBlank(),
                    onClick = {
                        onSubmit(
                            OwnerInfo(
                                name = name,
                                vatNumber = vatNumber,
                                address = address,
                                email = email,
                                phone = phone,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Login Screen - Light")
@Preview(
    showBackground = true,
    name = "Login Screen - Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LoginScreenPreview() {
    com.onlytechtake.repairapp.core.design.RepairAppTheme {
        LoginScreen(
            formState = AuthFormState(username = "admin"),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onDemoLogin = {},
            onRecovery = {},
        ) {}
    }
}

@Preview(showBackground = true, name = "First Setup - Light")
@Composable
private fun FirstSetupPreview() {
    com.onlytechtake.repairapp.core.design.RepairAppTheme {
        FirstSetupScreen(
            formState = AuthFormState(),
            onDisplayNameChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onRestore = { _, _ -> },
        ) {}
    }
}

@Preview(showBackground = true, name = "Owner Wizard - Light")
@Composable
private fun OwnerWizardPreview() {
    com.onlytechtake.repairapp.core.design.RepairAppTheme {
        OwnerWizardScreen(
            ownerInfo = OwnerInfo(),
            onSubmit = {},
        )
    }
}

@Preview(showBackground = true, name = "Recovery Screen - Light")
@Composable
private fun AdminRecoveryPreview() {
    com.onlytechtake.repairapp.core.design.RepairAppTheme {
        AdminRecoveryScreen(
            formState = AuthFormState(recoveryMode = true),
            onRecoveryCodeChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onCancel = {},
        )
    }
}

@Preview(showBackground = true, name = "Auth Loading - Light")
@Composable
private fun AuthLoadingPreview() {
    com.onlytechtake.repairapp.core.design.RepairAppTheme {
        AuthLoadingScreen(text = "Initializing system...")
    }
}
