package com.onlytechtake.repairapp.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthPolicy
import com.onlytechtake.repairapp.domain.auth.AuthState
import com.onlytechtake.repairapp.domain.auth.LoginResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

data class AuthFormState(
    val displayName: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = null,
    val loading: Boolean = false,
)

class AuthGateViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState(),
        )

    private val mutableFormState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = mutableFormState

    fun updateDisplayName(value: String) = updateForm { copy(displayName = value, errorMessage = null) }
    fun updateUsername(value: String) = updateForm { copy(username = value, errorMessage = null) }
    fun updatePassword(value: String) = updateForm { copy(password = value, errorMessage = null) }
    fun updateConfirmPassword(value: String) = updateForm { copy(confirmPassword = value, errorMessage = null) }

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
                LoginResult.InactiveUser -> mutableFormState.update {
                    it.copy(loading = false, errorMessage = "Utente disattivato")
                }
                LoginResult.InvalidCredentials -> mutableFormState.update {
                    it.copy(loading = false, errorMessage = "Credenziali non valide")
                }
                LoginResult.LockedOut -> mutableFormState.update {
                    it.copy(loading = false, errorMessage = "Troppi tentativi: riprova piu' tardi")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
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
