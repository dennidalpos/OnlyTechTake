package com.onlytechtake.repairapp.features.dashboard.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppIconSizes
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.AppStatusBadge
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.AppVerticalDivider
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.utils.labelRes
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderFilterMode
import com.onlytechtake.repairapp.domain.workspace.nextActionLabel
import com.onlytechtake.repairapp.features.dashboard.common.AppEmptyState
import com.onlytechtake.repairapp.features.dashboard.common.BackupReminderCard
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailViewModel
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailWorkspace
import com.onlytechtake.repairapp.features.workspace.OrderStatusStepper
import com.onlytechtake.repairapp.features.workspace.SharedWorkspaceViewModel
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass
import kotlinx.coroutines.launch

@Composable
fun OrdersPanel(
    viewModel: OrdersViewModel,
    sharedViewModel: SharedWorkspaceViewModel,
    onOrderClick: (String) -> Unit,
    onNewOrder: () -> Unit,
    onOpenBackupSettings: () -> Unit,
    onSetFormDirty: (Boolean) -> Unit = {},
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val resetSignal by sharedViewModel.resetSignal.collectAsState()
    androidx.compose.runtime.LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            viewModel.setFilter(OrderFilter())
        }
    }

    if (isWide) {
        var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                OrdersListContent(
                    viewModel = viewModel,
                    onOrderClick = { selectedOrderId = it },
                    onNewOrder = onNewOrder,
                    onOpenBackupSettings = onOpenBackupSettings,
                )
            }
            AppVerticalDivider()
            Box(Modifier.weight(1.5f)) {
                if (selectedOrderId != null) {
                    val container = LocalAppContainer.current
                    val detailViewModel: OrderDetailViewModel =
                        viewModel(
                            key = "order_detail_$selectedOrderId",
                            factory =
                                OrderDetailViewModel.Factory(
                                    context = container.context,
                                    orderId = selectedOrderId!!,
                                    authRepository = container.authRepository,
                                    orderRepository = container.orderWorkflowRepository,
                                    orderMediaRepository = container.orderMediaRepository,
                                    orderDocumentRepository = container.orderDocumentRepository,
                                    templateRepository = container.templateRepository,
                                    generateOrderStatusPdfUseCase = container.generateOrderStatusPdfUseCase,
                                    shareDocumentIntentFactory = container.shareDocumentIntentFactory,
                                    diagnosticLogger = container.diagnosticLogger,
                                    onSetFormDirty = onSetFormDirty,
                                ),
                        )
                    OrderDetailWorkspace(
                        viewModel = detailViewModel,
                    ) { selectedOrderId = null }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppEmptyState(
                            title = stringResource(R.string.orders_select_detail_title),
                            body = stringResource(R.string.orders_select_detail_hint),
                        )
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
    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }
    val scope = rememberCoroutineScope()

    val exportZipLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let(viewModel::exportCompleteBackup)
        }

    AppScreenContainer(scrollable = false) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(stringResource(R.string.orders_title))
            AppButton(
                text = stringResource(R.string.orders_filter_title),
                onClick = { showFilterDialog = true },
                variant = if (orderFilter.hasActiveFilter()) AppButtonVariant.Info else AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                leadingIcon = Icons.Default.FilterList,
                testTag = RepairAppTestTags.OrdersFilterButton,
            )
        }

        Spacer(Modifier.height(RepairAppSpacing.cardGap))

        if (showBackupReminder) {
            BackupReminderCard(
                settings = backupReminderSettings,
                onRunBackup = { exportZipLauncher.launch("onlytechtake-backup.zip") },
                onOpenSettings = onOpenBackupSettings,
            )
            Spacer(Modifier.height(RepairAppSpacing.cardGap))
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            AppTextField(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                value = orderFilter.query,
                onValueChange = { viewModel.setFilter(orderFilter.copy(query = it)) },
                label = stringResource(R.string.label_search_customer),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon =
                    if (orderFilter.query.isNotEmpty()) {
                        {
                            AppIconButton(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.content_description_clear),
                                onClick = { viewModel.setFilter(orderFilter.copy(query = "")) },
                            )
                        }
                    } else {
                        null
                    },
                imeAction = ImeAction.Search,
                singleLine = true,
            )
            Surface(
                onClick = onNewOrder,
                color = MaterialTheme.colorScheme.primary,
                shape = RepairAppShapes.button,
                modifier =
                    Modifier
                        .height(56.dp)
                        .width(56.dp)
                        .testTag(RepairAppTestTags.HomeNewOrderButton),
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.home_new_order),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(RepairAppIconSizes.medium),
                    )
                }
            }
        }

        if (orderFilter.hasActiveFilter()) {
            Spacer(Modifier.height(RepairAppSpacing.tinyGap))
            ActiveFilterInfo(orderFilter)
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        Box(modifier = Modifier.weight(1f)) {
            val listItems = orderListItems
            if (listItems == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.onlytechtake.repairapp.core.design.LoadingBrandIndicator(
                        text = stringResource(R.string.order_loading_detail),
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag("orders_list")
                            .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                    contentPadding =
                        PaddingValues(
                            top = RepairAppSpacing.compactGap,
                            bottom = RepairAppSpacing.bottomContentPadding,
                        ),
                ) {
                    if (listItems.isEmpty()) {
                        item {
                            AppEmptyState(
                                title = stringResource(R.string.orders_empty_title),
                                body = stringResource(R.string.orders_empty_body),
                            )
                        }
                    } else {
                        items(listItems, key = { it.order.id }) { item ->
                            val order = item.order
                            val customerName =
                                item.customer?.displayName(context)
                                    ?: stringResource(R.string.orders_customer_unavailable)
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
                                                text =
                                                    stringResource(
                                                        R.string.orders_order_reference,
                                                        order.orderNumber,
                                                        order.title,
                                                    ),
                                                modifier =
                                                    Modifier.testTag(
                                                        RepairAppTestTags.orderCardOrderReference(
                                                            order.id,
                                                        ),
                                                    ),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                softWrap = false,
                                            )
                                            Text(
                                                text =
                                                    stringResource(
                                                        R.string.orders_customer_reference,
                                                        customerName,
                                                    ),
                                                modifier =
                                                    Modifier.testTag(
                                                        RepairAppTestTags.orderCardCustomerReference(
                                                            order.id,
                                                        ),
                                                    ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                softWrap = false,
                                            )
                                        }
                                        Text(
                                            text = formatUiDateTime(order.updatedAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(start = RepairAppSpacing.compactGap),
                                        )
                                    }

                                    OrderStatusStepper(
                                        status = order.status,
                                        compact = true,
                                        modifier =
                                            Modifier.testTag(
                                                RepairAppTestTags.orderCardStatusStepper(
                                                    order.id,
                                                ),
                                            ),
                                    ) { step ->
                                        RepairAppTestTags.orderCardStatusStep(
                                            order.id,
                                            step,
                                        )
                                    }

                                    Text(
                                        text =
                                            stringResource(
                                                R.string.orders_next_action_label,
                                                nextActionLabel(LocalContext.current, order),
                                            ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showScrollToTop) {
                AppButton(
                    text = stringResource(R.string.customer_scroll_to_top),
                    leadingIcon = Icons.Default.KeyboardArrowUp,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = RepairAppSpacing.screenVertical)
                            .testTag(RepairAppTestTags.ScrollToTopButton),
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
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
                    text = stringResource(R.string.action_confirm),
                    onClick = { showFilterDialog = false },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

@Composable
private fun ActiveFilterInfo(filter: OrderFilter) {
    val modeText =
        when (filter.mode) {
            OrderFilterMode.ALL -> {
                null
            }

            OrderFilterMode.MINE -> {
                stringResource(R.string.orders_filter_mine)
            }

            OrderFilterMode.OPEN_UNASSIGNED -> {
                stringResource(
                    R.string.orders_filter_open_unassigned,
                )
            }

            OrderFilterMode.CLOSED -> {
                stringResource(R.string.orders_filter_closed_discarded)
            }

            OrderFilterMode.IN_PROGRESS -> {
                stringResource(
                    R.string.orders_filter_in_progress_assigned,
                )
            }
        }

    val filterText =
        listOfNotNull(
            modeText?.lowercase(),
            filter.status?.let { stringResource(it.labelRes) },
            filter.statuses
                .takeIf { it.isNotEmpty() }
                ?.let { statuses ->
                    buildString {
                        statuses.forEachIndexed { index, it ->
                            if (index > 0) append(" / ")
                            append(stringResource(it.labelRes))
                        }
                    }
                },
        ).joinToString(", ")

    if (filterText.isNotBlank()) {
        AppStatusBadge(
            text = stringResource(R.string.orders_filter_active, filterText),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun OrderFilterBar(
    filter: OrderFilter,
    onFilter: (OrderFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        FilterToggleButton(
            text = stringResource(R.string.orders_filter_all),
            selected = filter.mode == OrderFilterMode.ALL,
        ) { onFilter(OrderFilter(mode = OrderFilterMode.ALL)) }

        FilterToggleButton(
            text = stringResource(R.string.orders_filter_mine),
            selected = filter.mode == OrderFilterMode.MINE,
        ) { onFilter(filter.copy(mode = OrderFilterMode.MINE)) }

        FilterToggleButton(
            text = stringResource(R.string.orders_filter_open_unassigned),
            selected = filter.mode == OrderFilterMode.OPEN_UNASSIGNED,
        ) { onFilter(filter.copy(mode = OrderFilterMode.OPEN_UNASSIGNED)) }

        FilterToggleButton(
            text = stringResource(R.string.orders_filter_in_progress_assigned),
            selected = filter.mode == OrderFilterMode.IN_PROGRESS,
        ) { onFilter(filter.copy(mode = OrderFilterMode.IN_PROGRESS)) }

        FilterToggleButton(
            text = stringResource(R.string.orders_filter_closed_discarded),
            selected = filter.mode == OrderFilterMode.CLOSED,
        ) { onFilter(filter.copy(mode = OrderFilterMode.CLOSED)) }
    }
}

@Composable
private fun FilterToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AppButton(
        text = text,
        onClick = onClick,
        variant = if (selected) AppButtonVariant.Primary else AppButtonVariant.Ghost,
        size = AppButtonSize.Compact,
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
    )
}

private fun OrderFilter.hasActiveFilter(): Boolean =
    (mode != OrderFilterMode.ALL) || query.isNotBlank()
