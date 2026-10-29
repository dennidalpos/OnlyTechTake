package com.onlytechtake.repairapp.features.workspace

import android.content.Context
import androidx.annotation.StringRes
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.di.IoDispatcher
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticatedActionRunner
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        private val authRepository: AuthRepository,
        private val workspaceManager: WorkspaceManager,
        private val diagnosticLogger: DiagnosticLogger,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        fun run(
            scope: CoroutineScope,
            @StringRes successMessageRes: Int? = null,
            successMessage: String? = null,
            autoDismissMillis: Long? = null,
            setLoading: (Boolean) -> Unit = {},
            onSuccess: () -> Unit = {},
            block: suspend (AuthenticatedUser) -> Result<*>,
        ) {
            scope.launch {
                val actor = authRepository.currentUser.value
                if (actor == null) {
                    authRepository.logout()
                    workspaceManager.showMessage(context.getString(R.string.error_session_expired), true, null)
                    return@launch
                }

                setLoading(true)
                authRepository.touchSession()
                val result =
                    withContext(ioDispatcher) {
                        runCatching { block(actor) }.getOrElse { Result.failure<Any?>(it) }
                    }
                setLoading(false)
                result.fold(
                    onSuccess = { res ->
                        val message =
                            (res as? String)
                                ?: successMessage
                                ?: successMessageRes?.let { context.getString(it) }
                        message?.let { workspaceManager.showMessage(it, false, autoDismissMillis) }
                        onSuccess()
                    },
                ) { error ->
                    diagnosticLogger.logError(
                        "AuthenticatedActionRunner",
                        error.message ?: "Action failed",
                        error,
                    )
                    workspaceManager.showMessage(
                        error.message ?: context.getString(R.string.error_unknown),
                        true,
                        autoDismissMillis,
                    )
                }
            }
        }
    }
