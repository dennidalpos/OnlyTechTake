package com.onlytechtake.repairapp.features.workspace

import android.content.Context
import androidx.annotation.StringRes
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AuthenticatedActionRunner(
    val context: Context,
    private val authRepository: AuthRepository,
    private val currentUser: StateFlow<AuthenticatedUser?>,
    private val scope: CoroutineScope,
    private val showMessage: (String, Boolean) -> Unit,
    private val setLoading: (Boolean) -> Unit = {},
    private val diagnosticLogger: com.onlytechtake.repairapp.core.error.DiagnosticLogger? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun run(
        @StringRes successMessageRes: Int? = null,
        successMessage: String? = null,
        onSuccess: () -> Unit = {},
        block: suspend (AuthenticatedUser) -> Result<*>,
    ) {
        scope.launch {
            val actor = currentUser.value
            if (actor == null) {
                authRepository.logout()
                showMessage(context.getString(R.string.error_session_expired), true)
                return@launch
            }

            setLoading(true)
            authRepository.touchSession()
            val result =
                withContext(ioDispatcher) {
                    block(actor)
                }
            result.fold(
                onSuccess = { result ->
                    setLoading(false)
                    val message =
                        (result as? String)
                            ?: successMessage
                            ?: successMessageRes?.let { context.getString(it) }
                    message?.let { showMessage(it, false) }
                    onSuccess()
                },
            ) { error ->
                setLoading(false)
                diagnosticLogger?.logError("AuthenticatedActionRunner", error.message ?: "Action failed", error)
                showMessage(error.message ?: context.getString(R.string.error_unknown), true)
            }
        }
    }
}
