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
    private val setLoading: (Boolean) -> Unit = {},
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

            setLoading(true)
            authRepository.touchSession()
            block(actor).fold(
                onSuccess = { result ->
                    setLoading(false)
                    ((result as? String) ?: successMessage)?.let { showMessage(it, false) }
                    onSuccess()
                },
            ) { error ->
                setLoading(false)
                showMessage(error.message ?: "Errore", true)
            }
        }
    }

    private companion object {
        const val SESSION_EXPIRED_MESSAGE = "Sessione scaduta. Accedi di nuovo."
    }
}
