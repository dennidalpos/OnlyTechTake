package com.onlytechtake.repairapp.features.workspace

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
)

enum class WorkspaceDestination {
    HOME,
    CUSTOMERS,
    ORDERS,
    NEW_ORDER,
    ORDER_DETAIL,
    CONFIG,
    ADMIN,
}

fun CustomerType.label(): String =
    when (this) {
        CustomerType.PRIVATE -> "Privato"
        CustomerType.BUSINESS -> "Azienda"
    }
