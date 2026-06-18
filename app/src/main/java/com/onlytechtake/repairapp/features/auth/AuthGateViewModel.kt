package com.onlytechtake.repairapp.features.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.demo.ShowcaseDataSeeder
import com.onlytechtake.repairapp.domain.auth.AuthPolicy
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthState
import com.onlytechtake.repairapp.domain.auth.LoginResult
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
    private val authRepository: AuthRepository,
) : ViewModel() {
    val authState: StateFlow<AuthState> =
        authRepository
            .observeAuthState()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AuthState(),
            )

    private val mutableFormState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = mutableFormState

    fun seedShowcaseData(
        context: Context,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            if (mutableFormState.value.seeding) return@launch

            mutableFormState.update { it.copy(seeding = true, loading = true, errorMessage = null) }
            runCatching {
                ShowcaseDataSeeder.seedShowcaseData(context)
            }.onSuccess {
                onDone()
            }.onFailure { e ->
                android.util.Log.e("AuthGateViewModel", "Seed failed!", e)
                mutableFormState.update {
                    it.copy(
                        seeding = false,
                        loading = false,
                        errorMessage = "Errore durante l'inizializzazione: ${e.message}",
                    )
                }
            }
        }
    }

    fun loginShowcaseAdmin() {
        if (mutableFormState.value.loading) return
        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            authRepository.login("admin", ShowcaseDataSeeder.DEFAULT_PASSWORD.toCharArray())
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
                    it.copy(loading = false, errorMessage = "Creazione admin non riuscita")
                }
            }
        }
    }

    fun login() {
        val form = formState.value
        if (form.username.isBlank() || form.password.isBlank()) {
            setError("Inserisci username e password")
            return
        }

        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            when (val result = authRepository.login(form.username, form.password.toCharArray())) {
                is LoginResult.Success -> {
                    // Success will trigger AuthState change, no need to clear form here
                }
                is LoginResult.PasswordChangeRequired ->
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
                LoginResult.InactiveUser ->
                    mutableFormState.update {
                        it.copy(loading = false, errorMessage = "Utente disattivato")
                    }
                LoginResult.InvalidCredentials ->
                    mutableFormState.update {
                        it.copy(loading = false, errorMessage = "Credenziali non valide")
                    }
                LoginResult.LockedOut ->
                    mutableFormState.update {
                        it.copy(loading = false, errorMessage = "Troppi tentativi: riprova piu' tardi")
                    }
            }
        }
    }

    fun loginDemo() {
        viewModelScope.launch {
            mutableFormState.update { it.copy(loading = true, errorMessage = null) }
            authRepository.loginDemo()
        }
    }

    fun startRecovery() {
        updateForm { copy(recoveryMode = true, errorMessage = null, password = "", confirmPassword = "", recoveryCode = "") }
    }

    fun cancelRecovery() {
        updateForm { copy(recoveryMode = false, errorMessage = null, password = "", confirmPassword = "") }
    }

    fun recoverAdmin() {
        val form = formState.value
        if (form.recoveryCode.isBlank()) {
            setError("Inserisci il codice di recupero")
            return
        }
        if (form.password.length < AuthPolicy.MIN_PASSWORD_LENGTH) {
            setError("La password deve avere almeno 8 caratteri")
            return
        }
        if (form.password != form.confirmPassword) {
            setError("Le password non coincidono")
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
                            errorMessage = "Password resettata con successo. Effettua il login.",
                            password = "",
                            confirmPassword = "",
                            recoveryCode = "",
                        )
                    }
                }.onFailure { e ->
                    mutableFormState.update { it.copy(loading = false, errorMessage = e.message ?: "Errore durante il recupero") }
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
            setError("La password deve avere almeno 8 caratteri")
            return
        }
        if (form.password != form.confirmPassword) {
            setError("Le password non coincidono")
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
                    // After password change, we can try to login again automatically
                    // or just go back to login screen.
                    // Let's try to login automatically.
                    val loginResult = authRepository.login(form.username, form.password.toCharArray())
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
                                errorMessage = "Password cambiata, effettua il login",
                                password = "",
                                confirmPassword = "",
                                currentPassword = "",
                            )
                        }
                    }
                }.onFailure { e ->
                    mutableFormState.update {
                        it.copy(loading = false, errorMessage = e.message ?: "Errore durante il cambio password")
                    }
                }
        }
    }

    private fun validateSetup(form: AuthFormState): String? =
        when {
            form.displayName.isBlank() -> "Inserisci il nome admin"
            form.username.isBlank() -> "Inserisci lo username admin"
            form.password.length < AuthPolicy.MIN_PASSWORD_LENGTH -> "La password deve avere almeno 8 caratteri"
            form.password != form.confirmPassword -> "Le password non coincidono"
            else -> null
        }

    private fun updateForm(update: AuthFormState.() -> AuthFormState) {
        mutableFormState.update(update)
    }

    private fun setError(message: String) {
        mutableFormState.update { it.copy(errorMessage = message) }
    }

    class Factory(
        private val authRepository: AuthRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthGateViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return AuthGateViewModel(authRepository) as T
        }
    }
}
