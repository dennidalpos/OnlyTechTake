package com.onlytechtake.repairapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.features.auth.AuthGateViewModel
import com.onlytechtake.repairapp.features.auth.components.*
import kotlinx.serialization.Serializable

@Serializable sealed interface AuthRoute {
    @Serializable data object Loading : AuthRoute

    @Serializable data object Setup : AuthRoute

    @Serializable data object Login : AuthRoute

    @Serializable data object Wizard : AuthRoute

    @Serializable data object PasswordChange : AuthRoute

    @Serializable data object Recovery : AuthRoute

    @Serializable data object Error : AuthRoute
}

@Composable
fun AuthNavHost(
    onAuthenticated: () -> Unit,
    onAppDataRestored: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    disableAutoSeed: Boolean = false,
) {
    val navController = rememberNavController()
    val viewModel: AuthGateViewModel = hiltViewModel()
    val authState by viewModel.authState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val ownerInfo by viewModel.ownerInfo.collectAsState()

    // Handle Showcase auto-login/auto-seed if enabled
    LaunchedEffect(authState.initialized, authState.configured, authState.currentUser) {
        if (BuildConfig.IS_SHOWCASE_MODE_ENABLED && !disableAutoSeed && authState.initialized) {
            if (!authState.configured) {
                viewModel.seedShowcaseData { onAppDataRestored() }
            } else if (authState.currentUser == null) {
                viewModel.loginShowcaseAdmin()
            }
        }
    }

    LaunchedEffect(authState, formState, ownerInfo) {
        when {
            authState.initializationError != null -> {
                navController.navigate(AuthRoute.Error) { popUpTo(0) { inclusive = true } }
            }

            !authState.initialized || viewModel.isSeeding.value -> {
                navController.navigate(AuthRoute.Loading) { popUpTo(0) { inclusive = true } }
            }

            authState.currentUser != null && !ownerInfo.isComplete() -> {
                navController.navigate(AuthRoute.Wizard) { popUpTo(0) { inclusive = true } }
            }

            authState.currentUser != null -> {
                onAuthenticated()
            }

            formState.passwordChangeRequired -> {
                navController.navigate(AuthRoute.PasswordChange) { popUpTo(0) { inclusive = true } }
            }

            formState.recoveryMode -> {
                navController.navigate(AuthRoute.Recovery) { popUpTo(0) { inclusive = true } }
            }

            !authState.configured -> {
                navController.navigate(AuthRoute.Setup) { popUpTo(0) { inclusive = true } }
            }

            else -> {
                navController.navigate(AuthRoute.Login) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthRoute.Loading,
        modifier = modifier,
    ) {
        composable<AuthRoute.Loading> {
            AuthLoadingContent(text = stringResource(R.string.auth_loading_initializing))
        }
        composable<AuthRoute.Setup> {
            SetupContent(
                formState = formState,
                onDisplayNameChange = viewModel::updateDisplayName,
                onUsernameChange = viewModel::updateUsername,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::createFirstAdmin,
                onRestore = { uri, password -> viewModel.restoreBackup(uri, password, onAppDataRestored) },
                onBack = onExit,
            )
        }
        composable<AuthRoute.Login> {
            LoginContent(
                formState = formState,
                onUsernameChange = viewModel::updateUsername,
                onPasswordChange = viewModel::updatePassword,
                onSubmit = viewModel::login,
                onDemoLogin = viewModel::loginDemo,
                onRecovery = viewModel::startRecovery,
                onBack = onExit,
            )
        }
        composable<AuthRoute.Wizard> {
            WizardContent(
                ownerInfo = ownerInfo,
                onSubmit = viewModel::updateOwnerInfo,
            )
        }
        composable<AuthRoute.PasswordChange> {
            MandatoryPasswordChangeContent(
                formState = formState,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::changePassword,
                onCancel = viewModel::cancelPasswordChange,
            )
        }
        composable<AuthRoute.Recovery> {
            RecoveryContent(
                formState = formState,
                onRecoveryCodeChange = viewModel::updateRecoveryCode,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onSubmit = viewModel::recoverAdmin,
                onCancel = viewModel::cancelRecovery,
            )
        }
        composable<AuthRoute.Error> {
            AuthErrorContent(
                error = authState.initializationError ?: stringResource(R.string.error_unknown_short),
                onRetry = onAppDataRestored,
            )
        }
    }
}
