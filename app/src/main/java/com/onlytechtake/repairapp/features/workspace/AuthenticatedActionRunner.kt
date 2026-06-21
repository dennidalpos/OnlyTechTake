package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class AuthenticatedActionRunner(
    private val authRepository: AuthRepository,
    private val currentUser: StateFlow<AuthenticatedUser?>,
    private val scope: CoroutineScope,
    private val showMessage: (String, Boolean) -> Unit,
) {
    fun run(
        successMessage: String? = null,
        onSuccess: () -> Unit = {},
        block: suspend (AuthenticatedUser) -> Result<*>,
    ) {
        scope.launch {
            val actor = currentUser.value
            if (actor == null) {
                authRepository.logout()
                showMessage(SESSION_EXPIRED_MESSAGE, true)
                return@launch
            }

            authRepository.touchSession()
            block(actor).fold(
                onSuccess = { result ->
                    ((result as? String) ?: successMessage)?.let { showMessage(it, false) }
                    onSuccess()
                },
            ) { error ->
                showMessage(error.message ?: "Errore", true)
            }
        }
    }

    private companion object {
        const val SESSION_EXPIRED_MESSAGE = "Sessione scaduta. Accedi di nuovo."
    }
}
