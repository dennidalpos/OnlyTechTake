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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.formatUiDateTime
import androidx.compose.ui.platform.LocalContext
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.features.workspace.nextActionLabel
import com.onlytechtake.repairapp.features.workspace.OrderStatusStepper
import com.onlytechtake.repairapp.features.workspace.label as attachmentLabel

@Composable
fun OrdersPanel(viewModel: WorkspaceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val orderListItems by viewModel.orderListItems.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle(stringResource(R.string.orders_title))
        }

        item {
            OrderFilterBar(
                filter = uiState.orderFilter,
                onFilter = viewModel::setFilter,
            )
        }

        if (orderListItems.isEmpty()) {
            item {
                Text(stringResource(R.string.orders_empty_filter), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(orderListItems) { item ->
                val order = item.order
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(RepairAppTestTags.orderCard(order.id))
                        .clickable(
                            onClickLabel = "Apri ${order.orderNumber}",
                            role = Role.Button,
                            onClick = { viewModel.selectOrder(order.id) },
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                    ) {
                        Text("${order.orderNumber} - ${order.title}", style = MaterialTheme.typography.titleMedium)
                        OrderStatusStepper(status = order.status, compact = true)
                        Text(
                            stringResource(
                                R.string.orders_customer_label,
                                item.customer?.displayName(context).orEmpty()
                            )
                        )
                        Text(stringResource(R.string.orders_status_label, order.status.label(context)))
                        Text(stringResource(R.string.orders_next_action_label, nextActionLabel(order)))
                        Text(stringResource(R.string.orders_updated_label, formatUiDateTime(order.updatedAt)))
                        Text(stringResource(R.string.orders_media_label, item.attachments.attachmentLabel()))
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
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
        ActionRow {
            val allSelected = !filter.hasActiveFilter()
            TouchButton(
                text = stringResource(R.string.orders_filter_all),
                tone = if (allSelected) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                selected = allSelected,
                stateDescription = if (allSelected) stringResource(R.string.state_selected) else stringResource(R.string.state_not_selected),
                onClick = { onFilter(OrderFilter()) },
            )
            TouchButton(
                text = stringResource(R.string.orders_filter_mine),
                tone = if (filter.showMineOnly) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                selected = filter.showMineOnly,
                stateDescription = if (filter.showMineOnly) stringResource(R.string.state_selected) else stringResource(R.string.state_not_selected),
                onClick = { onFilter(filter.copy(showMineOnly = !filter.showMineOnly)) },
            )
            QuickStatusGroupFilter(stringResource(R.string.orders_filter_open), setOf(OrderStatus.OPEN), filter, onFilter)
            QuickStatusGroupFilter(stringResource(R.string.orders_filter_progress), setOf(OrderStatus.IN_PROGRESS, OrderStatus.DIAGNOSIS), filter, onFilter)
            QuickStatusGroupFilter(stringResource(R.string.orders_filter_result), setOf(OrderStatus.RESULT, OrderStatus.REPAIRING), filter, onFilter)
            QuickStatusGroupFilter(stringResource(R.string.orders_filter_ready), setOf(OrderStatus.READY_FOR_PICKUP), filter, onFilter)
        }
        if (filter.showMineOnly || filter.status != null || filter.statuses.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
            ) {
                Text(
                    text = stringResource(
                        R.string.orders_filter_active,
                        listOfNotNull(
                            stringResource(R.string.orders_filter_mine).lowercase().takeIf { filter.showMineOnly },
                            filter.status?.label(context),
                            filter.statuses.takeIf { it.isNotEmpty() }?.joinToString(" / ") { it.label(context) },
                        ).joinToString(", ")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun QuickStatusGroupFilter(
    label: String,
    statuses: Set<OrderStatus>,
    filter: OrderFilter,
    onFilter: (OrderFilter) -> Unit,
) {
    TouchButton(
        text = label,
        tone = if (filter.statuses == statuses) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
        selected = filter.statuses == statuses,
        stateDescription = if (filter.statuses == statuses) stringResource(R.string.state_selected) else stringResource(R.string.state_not_selected),
        onClick = {
            onFilter(
                filter.copy(
                    status = null,
                    statuses = statuses.takeUnless { filter.statuses == statuses } ?: emptySet(),
                ),
            )
        },
    )
}

private fun OrderFilter.hasActiveFilter(): Boolean =
    showMineOnly || status != null || statuses.isNotEmpty()
