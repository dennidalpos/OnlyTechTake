package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.formatUiDateTime
import com.onlytechtake.repairapp.features.workspace.label
import com.onlytechtake.repairapp.features.workspace.nextActionLabel

@Composable
fun OrdersPanel(viewModel: WorkspaceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val orderListItems by viewModel.orderListItems.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionTitle("Commesse")
        }

        item {
            OrderFilterBar(
                filter = uiState.orderFilter,
                onFilter = viewModel::setFilter,
            )
        }

        if (orderListItems.isEmpty()) {
            item {
                Text("Nessuna commessa per questo filtro.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(orderListItems) { item ->
                val order = item.order
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = "Apri ${order.orderNumber}",
                            role = Role.Button,
                            onClick = { viewModel.selectOrder(order.id) },
                        ),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${order.orderNumber} - ${order.title}", style = MaterialTheme.typography.titleMedium)
                        Text("Cliente: ${item.customer?.displayName().orEmpty().ifBlank { "Cliente non disponibile" }}")
                        Text("Stato: ${order.status.label()}")
                        Text("Prossima azione: ${nextActionLabel(order.status)}")
                        Text("Aggiornata: ${formatUiDateTime(order.updatedAt)}")
                        Text("Media e firme: ${item.attachments.label()}")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderFilterBar(
    filter: OrderFilter,
    onFilter: (OrderFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionRow {
            TouchButton(
                text = "Tutte",
                tone = if (!filter.hasActiveFilter()) ButtonTone.SUCCESS else ButtonTone.DANGER,
                onClick = { onFilter(OrderFilter()) },
            )
            TouchButton(
                text = "Mie",
                tone = if (filter.showMineOnly) ButtonTone.SUCCESS else ButtonTone.DANGER,
                onClick = { onFilter(filter.copy(showMineOnly = !filter.showMineOnly)) },
            )
            QuickStatusFilter("Nuove", OrderStatus.NEW, filter, onFilter)
            QuickStatusFilter("Diagnosi", OrderStatus.DIAGNOSIS, filter, onFilter)
            QuickStatusFilter("Attesa cliente", OrderStatus.WAITING_CUSTOMER, filter, onFilter)
            QuickStatusFilter("Pronte", OrderStatus.READY_FOR_PICKUP, filter, onFilter)
        }
        if (filter.showMineOnly || filter.status != null || filter.statuses.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Filtro attivo: " + listOfNotNull(
                        "mie".takeIf { filter.showMineOnly },
                        filter.status?.label(),
                        filter.statuses.takeIf { it.isNotEmpty() }?.joinToString(" / ") { it.label() },
                    ).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun QuickStatusFilter(
    label: String,
    status: OrderStatus,
    filter: OrderFilter,
    onFilter: (OrderFilter) -> Unit,
) {
    TouchButton(
        text = label,
        tone = if (filter.status == status) ButtonTone.SUCCESS else ButtonTone.DANGER,
        onClick = {
            onFilter(
                filter.copy(
                    status = status.takeUnless { filter.status == status },
                    statuses = emptySet(),
                ),
            )
        },
    )
}

private fun OrderFilter.hasActiveFilter(): Boolean =
    showMineOnly || status != null || statuses.isNotEmpty()
