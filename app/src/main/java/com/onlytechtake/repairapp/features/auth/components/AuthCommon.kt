package com.onlytechtake.repairapp.features.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.design.PasswordVisibilityButton
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppErrorText
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.features.auth.AuthFormState

@Composable
fun AuthForm(
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
