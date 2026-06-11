package com.onlytechtake.repairapp.features.workspace

import android.content.Intent
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage

data class WorkspaceMessage(
    val text: String,
    val isError: Boolean,
)

data class WorkspaceUiState(
    val message: WorkspaceMessage? = null,
    val destination: WorkspaceDestination = WorkspaceDestination.HOME,
    val selectedOrderId: String? = null,
    val orderFilter: OrderFilter = OrderFilter(),
    val customerSearchQuery: String = "",
    val preparedMessage: PreparedMessage? = null,
    val preparedMessageBody: String = "",
    val isGeneratingStatusPdf: Boolean = false,
    val statusPdfError: String? = null,
    val showPdfSentConfirmation: Boolean = false,
    val pendingShareIntent: Intent? = null,
    val lastGeneratedDocumentId: String? = null,
)

fun WorkspaceUiState.withoutPreparedMessage(): WorkspaceUiState =
    copy(preparedMessage = null, preparedMessageBody = "")

enum class WorkspaceDestination {
    HOME,
    CUSTOMERS,
    ORDERS,
    NEW_ORDER,
    ORDER_DETAIL,
    CONFIG,
    ADMIN,
}

enum class ButtonTone {
    PRIMARY,
    NEUTRAL,
    SUCCESS,
    INFO,
    WARNING,
    DANGER,
}
