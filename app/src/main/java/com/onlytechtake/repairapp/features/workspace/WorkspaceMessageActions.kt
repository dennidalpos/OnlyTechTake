package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import com.onlytechtake.repairapp.domain.workspace.statusTemplateCode
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
                        if (it.destination == WorkspaceDestination.ORDER_DETAIL && it.selectedOrderId == orderId) {
                            it.copy(
                                preparedMessage = prepared,
                                preparedMessageBody = prepared.body,
                                message = null,
                            )
                        } else {
                            it
                        }
                    }
                },
                onFailure = showError,
            )
        }
    }

    fun updatePreparedMessageBody(body: String) {
        mutableUiState.update { it.copy(preparedMessageBody = body) }
    }

    fun clearPreparedMessage() {
        mutableUiState.update(WorkspaceUiState::withoutPreparedMessage)
    }

    fun recordPreparedMessage() = withPrepared(
        clearOnSuccess = false,
        successMessage = "WhatsApp preparato",
    ) { actor, prepared, body ->
        templateRepository.recordPrepared(actor, prepared, body)
    }

    fun confirmSentMessage() = withPrepared(
        clearOnSuccess = true,
        successMessage = "WhatsApp registrato",
    ) { actor, prepared, body ->
        templateRepository.confirmSent(actor, prepared, body)
    }

    private fun withPrepared(
        clearOnSuccess: Boolean,
        successMessage: String,
        block: suspend (AuthenticatedUser, PreparedMessage, String) -> Result<Unit>,
    ) {
        authenticatedActionRunner.runCustom { actor ->
            val prepared = mutableUiState.value.preparedMessage
                ?: return@runCustom showMessage("Prepara prima il messaggio", true)
            block(actor, prepared, mutableUiState.value.preparedMessageBody).fold(
                onSuccess = {
                    if (clearOnSuccess) clearPreparedMessage()
                    showSuccess(successMessage)
                },
                onFailure = showError,
            )
        }
    }
}
