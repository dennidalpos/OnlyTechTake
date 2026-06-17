package com.onlytechtake.repairapp.features.dashboard.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.nextActionLabel
import com.onlytechtake.repairapp.features.dashboard.common.*
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailWorkspace
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.input.ImeAction
import com.onlytechtake.repairapp.features.workspace.OrderStatusStepper
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass

@Composable
fun OrdersPanel(
    viewModel: OrdersViewModel,
    onOrderClick: (String) -> Unit,
    onNewOrder: () -> Unit,
    onOpenBackupSettings: () -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    if (isWide) {
        var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                OrdersListContent(
                    viewModel = viewModel,
                    onOrderClick = { selectedOrderId = it },
                    onNewOrder = onNewOrder,
                    onOpenBackupSettings = onOpenBackupSettings
                )
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
                        ) { /* Handle dirty state if needed */ },
                    )
                    OrderDetailWorkspace(
                        viewModel = detailViewModel,
                    ) { selectedOrderId = null }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.orders_select_detail_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    } else {
        OrdersListContent(viewModel, onOrderClick, onNewOrder, onOpenBackupSettings)
    }
}

@Composable
private fun OrdersListContent(
    viewModel: OrdersViewModel,
    onOrderClick: (String) -> Unit,
    onNewOrder: () -> Unit,
    onOpenBackupSettings: () -> Unit,
) {
    val orderFilter by viewModel.orderFilter.collectAsState()
    val orderListItems by viewModel.orderListItems.collectAsState()
    val backupReminderSettings by viewModel.backupReminderSettings.collectAsState()
    val showBackupReminder by viewModel.isBackupReminderDue.collectAsState()
    val context = LocalContext.current
    var showFilterDialog by rememberSaveable { mutableStateOf(value = false) }

    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportCompleteBackup)
    }

    AppScreenContainer(scrollable = false) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(stringResource(R.string.orders_title))
            Row(horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                AppButton(
                    text = stringResource(R.string.home_new_order),
                    onClick = onNewOrder,
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    leadingIcon = Icons.Default.Add
                )
                AppButton(
                    text = stringResource(R.string.orders_filter_title),
                    onClick = { showFilterDialog = true },
                    variant = if (orderFilter.hasActiveFilter()) AppButtonVariant.Info else AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    leadingIcon = Icons.Default.FilterList,
                    testTag = RepairAppTestTags.OrdersFilterButton
                )
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.cardGap))

        if (showBackupReminder) {
            BackupReminderCard(
                settings = backupReminderSettings,
                onRunBackup = { exportZipLauncher.launch("onlytechtake-backup.zip") },
                onOpenSettings = onOpenBackupSettings
            )
            Spacer(Modifier.height(RepairAppSpacing.cardGap))
        }

        AppTextField(
            value = orderFilter.query,
            onValueChange = { viewModel.setFilter(orderFilter.copy(query = it)) },
            label = stringResource(R.string.orders_search_label),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (orderFilter.query.isNotEmpty()) {
                {
                    AppIconButton(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.content_description_clear),
                        onClick = { viewModel.setFilter(orderFilter.copy(query = "")) }
                    )
                }
            } else null,
            imeAction = ImeAction.Search,
            singleLine = true
        )

        if (orderFilter.hasActiveFilter()) {
            Spacer(Modifier.height(4.dp))
            ActiveFilterInfo(orderFilter)
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
            contentPadding = PaddingValues(vertical = RepairAppSpacing.compactGap)
        ) {
            if (orderListItems.isEmpty()) {
                item {
                    Text(stringResource(R.string.orders_empty_filter), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(orderListItems, key = { it.order.id }) { item ->
                    val order = item.order
                    StandardCard(
                        modifier = Modifier.testTag(RepairAppTestTags.orderCard(order.id)),
                        onClick = { onOrderClick(order.id) },
                    ) {
                        Column(
                            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${order.orderNumber} - ${order.title}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = item.customer?.displayName(context).orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    formatUiDateTime(order.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            
                            OrderStatusStepper(status = order.status, compact = true)
                            
                            Text(
                                text = stringResource(R.string.orders_next_action_label, nextActionLabel(order)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        AppDialog(
            onDismissRequest = { showFilterDialog = false },
            title = stringResource(R.string.orders_filter_title),
            content = {
                OrderFilterBar(
                    filter = orderFilter,
                    onFilter = viewModel::setFilter,
                )
            },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.confirm),
                    onClick = { showFilterDialog = false },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact
                )
            },
        )
    }
}

@Composable
private fun ActiveFilterInfo(filter: OrderFilter) {
    val context = LocalContext.current
    val filterText = listOfNotNull(
        stringResource(R.string.orders_filter_mine).lowercase().takeIf { filter.showMineOnly },
        filter.status?.label(context),
        filter.statuses.takeIf { it.isNotEmpty() }?.joinToString(" / ") { it.label(context) },
    ).joinToString(", ")
    
    if (filterText.isNotBlank()) {
        AppStatusChip(
            text = stringResource(R.string.orders_filter_active, filterText),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun OrderFilterBar(
    filter: OrderFilter,
    onFilter: (OrderFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        val allSelected = !filter.hasActiveFilter()
        
        FilterToggleButton(
            text = stringResource(R.string.orders_filter_all),
            selected = allSelected,
            onClick = { onFilter(OrderFilter()) }
        )
        
        FilterToggleButton(
            text = stringResource(R.string.orders_filter_mine),
            selected = filter.showMineOnly,
            onClick = { onFilter(filter.copy(showMineOnly = !filter.showMineOnly)) }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp).alpha(0.3f))
        
        QuickStatusGroupFilter(stringResource(R.string.orders_filter_open), setOf(OrderStatus.OPEN), filter, onFilter)
        QuickStatusGroupFilter(stringResource(R.string.orders_filter_progress), setOf(OrderStatus.IN_PROGRESS, OrderStatus.DIAGNOSIS), filter, onFilter)
        QuickStatusGroupFilter(stringResource(R.string.orders_filter_result), setOf(OrderStatus.RESULT, OrderStatus.REPAIRING), filter, onFilter)
        QuickStatusGroupFilter(stringResource(R.string.orders_filter_ready), setOf(OrderStatus.READY_FOR_PICKUP), filter, onFilter)
    }
}

@Composable
private fun FilterToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AppButton(
        text = text,
        onClick = onClick,
        variant = if (selected) AppButtonVariant.Primary else AppButtonVariant.Ghost,
        size = AppButtonSize.Compact,
        modifier = Modifier.fillMaxWidth(),
        selected = selected
    )
}

@Composable
private fun QuickStatusGroupFilter(
    label: String,
    statuses: Set<OrderStatus>,
    filter: OrderFilter,
    onFilter: (OrderFilter) -> Unit,
) {
    val isSelected = filter.statuses == statuses
    FilterToggleButton(
        text = label,
        selected = isSelected,
        onClick = {
            onFilter(
                filter.copy(
                    status = null,
                    statuses = statuses.takeUnless { isSelected } ?: emptySet(),
                ),
            )
        },
    )
}

private fun OrderFilter.hasActiveFilter(): Boolean =
    showMineOnly || (status != null) || statuses.isNotEmpty() || query.isNotBlank()
