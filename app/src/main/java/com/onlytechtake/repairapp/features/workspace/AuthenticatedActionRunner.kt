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
    private val showSuccess: (String) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val showMessage: (String, Boolean) -> Unit,
) {
    fun run(
        successMessage: String = "Operazione completata",
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
                    showSuccess(result as? String ?: successMessage)
                    onSuccess()
                },
                onFailure = showError,
            )
        }
    }

    fun runCustom(block: suspend (AuthenticatedUser) -> Unit) {
        scope.launch {
            val actor = currentUser.value
            if (actor == null) {
                authRepository.logout()
                showMessage(SESSION_EXPIRED_MESSAGE, true)
                return@launch
            }

            authRepository.touchSession()
            block(actor)
        }
    }

    private companion object {
        const val SESSION_EXPIRED_MESSAGE = "Sessione scaduta. Accedi di nuovo."
    }
}
