package com.onlytechtake.repairapp.features.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.demo.DataSeeder
import com.onlytechtake.repairapp.domain.auth.AuthPolicy
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthState
import com.onlytechtake.repairapp.domain.auth.LoginResult
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormState(
    val displayName: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val currentPassword: String = "",
    val recoveryCode: String = "",
    val recoveryMode: Boolean = false,
    val passwordChangeRequired: Boolean = false,
    val errorMessage: String? = null,
    val loading: Boolean = false,
    val seeding: Boolean = false,
)

class AuthGateViewModel(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val ownerPreferences: OwnerPreferences,
    private val backupRepository: BackupRepository,
    private val dataSeeder: DataSeeder,
) : ViewModel() {
    val authState: StateFlow<AuthState> =
        authRepository
            .observeAuthState()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AuthState(),
            )

    private val mutableFormState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = mutableFormState

    val ownerInfo =
        ownerPreferences.ownerInfo.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OwnerInfo(),
        )

    fun seedShowcaseData(
        context: Context,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            if (mutableFormState.value.seeding) return@launch

            mutableFormState.update { it.copy(seeding = true, loading = true, errorMessage = null) }
            runCatching {
                dataSeeder.seed(context)
            }.onSuccess {
                onDone()
            }.onFailure { e ->
                android.util.Log.e("AuthGateViewModel", "Seed failed!", e)
                mutableFormState.update {
                    it.copy(
                        seeding = false,
                        loading = false,
                        errorMessage =
                            context.getString(
                                R.string.error_init_failed,
                                e.message,
                            ),
                    )
                }
            }
        }
    }

    fun loginShowcaseAdmin() {
        if (mutableFormState.value.loading) return
        val password = dataSeeder.defaultPassword ?: return
        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            authRepository.login("admin", password.toCharArray())
            mutableFormState.update { it.copy(loading = false) }
        }
    }

    fun updateDisplayName(value: String) = updateForm { copy(displayName = value, errorMessage = null) }

    fun updateUsername(value: String) = updateForm { copy(username = value, errorMessage = null) }

    fun updatePassword(value: String) = updateForm { copy(password = value, errorMessage = null) }

    fun updateConfirmPassword(value: String) = updateForm { copy(confirmPassword = value, errorMessage = null) }

    fun updateRecoveryCode(value: String) = updateForm { copy(recoveryCode = value, errorMessage = null) }

    fun createFirstAdmin() {
        val form = formState.value
        val validationError = validateSetup(form)
        if (validationError != null) {
            setError(validationError)
            return
        }

        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            runCatching {
                authRepository.createFirstAdmin(
                    displayName = form.displayName,
                    username = form.username,
                    password = form.password.toCharArray(),
                )
            }.onSuccess {
                // We don't clear the form here to avoid a flicker before navigation
                // The AuthState update will trigger the transition to Dashboard
            }.onFailure {
                mutableFormState.update {
                    it.copy(
                        loading = false,
                        errorMessage = context.getString(R.string.error_admin_creation_failed),
                    )
                }
            }
        }
    }

    fun login() {
        val form = formState.value
        if (form.username.isBlank() || form.password.isBlank()) {
            setError(context.getString(R.string.error_enter_username_password))
            return
        }

        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            when (authRepository.login(form.username, form.password.toCharArray())) {
                is LoginResult.Success -> {
                    // Success will trigger AuthState change, no need to clear form here
                }

                is LoginResult.PasswordChangeRequired -> {
                    mutableFormState.update {
                        it.copy(
                            loading = false,
                            passwordChangeRequired = true,
                            errorMessage = null,
                            currentPassword = it.password,
                            password = "",
                            confirmPassword = "",
                        )
                    }
                }

                LoginResult.InactiveUser -> {
                    mutableFormState.update {
                        it.copy(
                            loading = false,
                            errorMessage = context.getString(R.string.error_user_deactivated),
                        )
                    }
                }

                LoginResult.InvalidCredentials -> {
                    mutableFormState.update {
                        it.copy(
                            loading = false,
                            errorMessage = context.getString(R.string.error_invalid_credentials),
                        )
                    }
                }

                LoginResult.LockedOut -> {
                    mutableFormState.update {
                        it.copy(
                            loading = false,
                            errorMessage = context.getString(R.string.error_too_many_attempts),
                        )
                    }
                }
            }
        }
    }

    fun loginDemo() {
        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            // Seed data if not already configured (first demo run)
            if (!authState.value.configured) {
                runCatching {
                    dataSeeder.seed(context)
                }.onFailure { e ->
                    android.util.Log.e("AuthGateViewModel", "Demo seed failed", e)
                }
            }
            authRepository.loginDemo()
        }
    }

    fun startRecovery() {
        updateForm {
            copy(
                recoveryMode = true,
                errorMessage = null,
                password = "",
                confirmPassword = "",
                recoveryCode = "",
            )
        }
    }

    fun cancelRecovery() {
        updateForm {
            copy(
                recoveryMode = false,
                errorMessage = null,
                password = "",
                confirmPassword = "",
            )
        }
    }

    fun recoverAdmin() {
        val form = formState.value
        if (form.recoveryCode.isBlank()) {
            setError(context.getString(R.string.error_enter_recovery_code))
            return
        }
        if (form.password.length < AuthPolicy.MIN_PASSWORD_LENGTH) {
            setError(context.getString(R.string.error_password_min_length))
            return
        }
        if (form.password != form.confirmPassword) {
            setError(context.getString(R.string.error_passwords_mismatch))
            return
        }

        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            authRepository
                .resetAdminWithRecoveryCode(form.recoveryCode, form.password.toCharArray())
                .onSuccess {
                    mutableFormState.update {
                        it.copy(
                            loading = false,
                            recoveryMode = false,
                            errorMessage = context.getString(R.string.success_password_reset),
                            password = "",
                            confirmPassword = "",
                            recoveryCode = "",
                        )
                    }
                }.onFailure { e ->
                    mutableFormState.update {
                        it.copy(
                            loading = false,
                            errorMessage =
                                e.message
                                    ?: context.getString(R.string.error_recovery_failed),
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            mutableFormState.update {
                it.copy(
                    password = "",
                    confirmPassword = "",
                    currentPassword = "",
                    passwordChangeRequired = false,
                    loading = false,
                    errorMessage = null,
                )
            }
        }
    }

    fun cancelPasswordChange() {
        mutableFormState.update {
            it.copy(
                passwordChangeRequired = false,
                password = "",
                confirmPassword = "",
                currentPassword = "",
                errorMessage = null,
            )
        }
    }

    fun changePassword() {
        val form = formState.value
        if (form.password.length < AuthPolicy.MIN_PASSWORD_LENGTH) {
            setError(context.getString(R.string.error_password_min_length))
            return
        }
        if (form.password != form.confirmPassword) {
            setError(context.getString(R.string.error_passwords_mismatch))
            return
        }

        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            authRepository
                .changePassword(
                    username = form.username,
                    currentPassword = form.currentPassword.toCharArray(),
                    newPassword = form.password.toCharArray(),
                ).onSuccess {
                    // After password change, we can try to log in again automatically
                    // or just go back to login screen.
                    // Let's try to log in automatically.
                    val loginResult =
                        authRepository.login(form.username, form.password.toCharArray())
                    if (loginResult is LoginResult.Success) {
                        mutableFormState.update {
                            it.copy(
                                loading = false,
                                passwordChangeRequired = false,
                                password = "",
                                confirmPassword = "",
                                currentPassword = "",
                            )
                        }
                    } else {
                        mutableFormState.update {
                            it.copy(
                                loading = false,
                                passwordChangeRequired = false,
                                errorMessage = context.getString(R.string.success_password_changed_login),
                                password = "",
                                confirmPassword = "",
                                currentPassword = "",
                            )
                        }
                    }
                }.onFailure { e ->
                    mutableFormState.update {
                        it.copy(
                            loading = false,
                            errorMessage =
                                e.message
                                    ?: context.getString(R.string.error_change_password_failed),
                        )
                    }
                }
        }
    }

    private fun validateSetup(form: AuthFormState): String? =
        when {
            form.displayName.isBlank() -> context.getString(R.string.error_enter_admin_name)
            form.username.isBlank() -> context.getString(R.string.error_enter_admin_username)
            form.password.length < AuthPolicy.MIN_PASSWORD_LENGTH -> context.getString(R.string.error_password_min_length)
            form.password != form.confirmPassword -> context.getString(R.string.error_passwords_mismatch)
            else -> null
        }

    private fun updateForm(update: AuthFormState.() -> AuthFormState) {
        mutableFormState.update(update)
    }

    private fun setError(message: String) {
        mutableFormState.update { it.copy(errorMessage = message) }
    }

    fun updateOwnerInfo(info: OwnerInfo) {
        viewModelScope.launch {
            ownerPreferences.updateOwnerInfo(info)
        }
    }

    fun restoreBackup(
        uri: Uri,
        password: String? = null,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            backupRepository
                .restoreInitialBackup(uri, password)
                .onSuccess {
                    onDone()
                }.onFailure { e ->
                    mutableFormState.update {
                        it.copy(
                            loading = false,
                            errorMessage =
                                e.message
                                    ?: context.getString(R.string.error_restore_failed),
                        )
                    }
                }
        }
    }

    class Factory(
        private val context: Context,
        private val authRepository: AuthRepository,
        private val ownerPreferences: OwnerPreferences,
        private val backupRepository: BackupRepository,
        private val dataSeeder: DataSeeder,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthGateViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return AuthGateViewModel(
                context,
                authRepository,
                ownerPreferences,
                backupRepository,
                dataSeeder,
            ) as T
        }
    }
}
