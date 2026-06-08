package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class WorkspaceMessageActions(
    private val authenticatedActionRunner: AuthenticatedActionRunner,
    private val mutableUiState: MutableStateFlow<WorkspaceUiState>,
    private val templateRepository: TemplateRepository,
    private val showSuccess: (String) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val showMessage: (String, Boolean) -> Unit,
) {
    fun prepareStatusMessage(orderId: String, status: OrderStatus) {
        prepareMessage(orderId, statusTemplateCode(status))
    }

    fun prepareMessage(orderId: String, templateCode: String) {
        authenticatedActionRunner.runCustom { actor ->
            templateRepository.prepareMessage(actor, orderId, templateCode).fold(
                onSuccess = { prepared ->
                    mutableUiState.update {
                        it.copy(
                            preparedMessage = prepared,
                            preparedMessageBody = prepared.body,
                            message = WorkspaceMessage("Anteprima WhatsApp pronta", false),
                        )
                    }
                },
                onFailure = showError,
            )
        }
    }

    fun updatePreparedMessageBody(body: String) {
        mutableUiState.update { it.copy(preparedMessageBody = body) }
    }

    fun recordPreparedMessage() = withPrepared { actor, prepared, body ->
        templateRepository.recordPrepared(actor, prepared, body)
    }

    fun confirmSentMessage() = withPrepared { actor, prepared, body ->
        templateRepository.confirmSent(actor, prepared, body)
    }

    private fun withPrepared(
        block: suspend (AuthenticatedUser, PreparedMessage, String) -> Result<Unit>,
    ) {
        authenticatedActionRunner.runCustom { actor ->
            val prepared = mutableUiState.value.preparedMessage
                ?: return@runCustom showMessage("Prepara prima il messaggio", true)
            block(actor, prepared, mutableUiState.value.preparedMessageBody).fold(
                onSuccess = {
                    mutableUiState.update { it.copy(preparedMessage = null, preparedMessageBody = "") }
                    showSuccess("WhatsApp registrato")
                },
                onFailure = showError,
            )
        }
    }
}
