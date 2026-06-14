package com.onlytechtake.repairapp.features.dashboard.orders


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailWorkspace
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.nextActionLabel
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.OrderStatusStepper
import com.onlytechtake.repairapp.features.workspace.label as attachmentLabel

@Composable
fun OrdersPanel(
    viewModel: OrdersViewModel,
    onOrderClick: (String) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    if (isWide) {
        var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                OrdersListContent(viewModel = viewModel) {
                    selectedOrderId = it
                }
            }
            VerticalDivider()
            Box(Modifier.weight(1.5f)) {
                if (selectedOrderId != null) {
                    val container = LocalAppContainer.current
                    val detailViewModel: OrderDetailViewModel = viewModel(
                        key = "order_detail_$selectedOrderId",
                        factory = OrderDetailViewModel.Factory(
                            orderId = selectedOrderId!!,
                            authRepository = container.authRepository,
                            orderRepository = container.orderWorkflowRepository,
                            orderMediaRepository = container.orderMediaRepository,
                            orderDocumentRepository = container.orderDocumentRepository,
                            templateRepository = container.templateRepository,
                            generateOrderStatusPdfUseCase = container.generateOrderStatusPdfUseCase,
                            shareDocumentIntentFactory = container.shareDocumentIntentFactory,
                            onSetFormDirty = { /* Handle dirty state if needed */ },
                        )
                    )
                    OrderDetailWorkspace(
                        viewModel = detailViewModel,
                        onBack = { selectedOrderId = null }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Seleziona una commessa per vedere i dettagli", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    } else {
        OrdersListContent(viewModel, onOrderClick)
    }
}

@Composable
private fun OrdersListContent(
    viewModel: OrdersViewModel,
    onOrderClick: (String) -> Unit,
) {
    val orderFilter by viewModel.orderFilter.collectAsState()
    val orderListItems by viewModel.orderListItems.collectAsState()
    val context = LocalContext.current
    var showFilterDialog by rememberSaveable { mutableStateOf(value = false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = DashboardSpacing.maxContentWidth)
                .imePadding(),
            contentPadding = PaddingValues(DashboardSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(stringResource(R.string.orders_title))
                    TouchButton(
                        text = stringResource(R.string.orders_filter_title),
                        tone = if (orderFilter.hasActiveFilter()) ButtonTone.INFO else ButtonTone.NEUTRAL,
                        onClick = { showFilterDialog = true }
                    )
                }
            }

            item {
                Field(
                    value = orderFilter.query,
                    onValueChange = { viewModel.setFilter(orderFilter.copy(query = it)) },
                    label = "Cerca per cliente, telefono, email o commessa...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (orderFilter.hasActiveFilter()) {
                item {
                    ActiveFilterInfo(orderFilter)
                }
            }

            if (orderListItems.isEmpty()) {
                item {
                    Text(stringResource(R.string.orders_empty_filter), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(orderListItems, key = { it.order.id }) { item ->
                    val order = item.order
                    StandardCard(
                        modifier = Modifier
                            .testTag(RepairAppTestTags.orderCard(order.id)),
                        onClick = {
                            onOrderClick(order.id)
                        },
                    ) {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    "${order.orderNumber} - ${order.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    formatUiDateTime(order.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OrderStatusStepper(status = order.status, compact = true)
                            Text(
                                text = stringResource(
                                    R.string.orders_customer_label,
                                    item.customer?.displayName(context).orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(R.string.orders_next_action_label, nextActionLabel(order)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text(stringResource(R.string.orders_filter_title)) },
            text = {
                OrderFilterBar(
                    filter = orderFilter,
                    onFilter = viewModel::setFilter,
                )
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }
}

@Composable
private fun ActiveFilterInfo(filter: OrderFilter) {
    val context = LocalContext.current
    Text(
        text = stringResource(
            R.string.orders_filter_active,
            listOfNotNull(
                stringResource(R.string.orders_filter_mine).lowercase().takeIf { filter.showMineOnly },
                filter.status?.label(context),
                filter.statuses.takeIf { it.isNotEmpty() }?.joinToString(" / ") { it.label(context) },
            ).joinToString(", ")
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun OrderFilterBar(
    filter: OrderFilter,
    onFilter: (OrderFilter) -> Unit,
) {
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
    showMineOnly || (status != null) || statuses.isNotEmpty() || query.isNotBlank()
