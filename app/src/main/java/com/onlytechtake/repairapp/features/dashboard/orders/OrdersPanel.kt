package com.onlytechtake.repairapp.features.dashboard.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailViewModel
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailWorkspace
import com.onlytechtake.repairapp.features.workspace.SharedWorkspaceViewModel
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass
import kotlinx.coroutines.launch

@Composable
fun OrdersPanel(
    viewModel: OrdersViewModel,
    sharedViewModel: SharedWorkspaceViewModel,
    onOrderClick: (String) -> Unit,
    onNewOrder: () -> Unit,
    onSetFormDirty: (Boolean) -> Unit = {},
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    val orderListItems by viewModel.orderListItems.collectAsState()

    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Force refresh when entering the panel if needed
    }

    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            OrderListContent(
                orders = orderListItems,
                onFilterClick = { showFilterDialog = true },
                onOrderClick = {
                    if (isExpanded) {
                        selectedOrderId = it
                    } else {
                        onOrderClick(it)
                    }
                },
                onNewOrder = onNewOrder,
            )
        }

        if (isExpanded) {
            AppVerticalDivider()
            Box(Modifier.weight(1.5f)) {
                if (selectedOrderId != null) {
                    key(selectedOrderId) {
                        val orderDetailViewModel: OrderDetailViewModel = hiltViewModel(key = selectedOrderId)
                        LaunchedEffect(selectedOrderId) {
                            orderDetailViewModel.setOrderId(selectedOrderId)
                        }
                        OrderDetailWorkspace(
                            viewModel = orderDetailViewModel,
                            sharedViewModel = sharedViewModel,
                            showCloseButton = false,
                            onBack = { selectedOrderId = null },
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.orders_select_detail_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.orders_select_detail_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        val filter by viewModel.orderFilter.collectAsState()
        OrderFilterDialog(
            currentFilter = filter,
            onDismissRequest = { showFilterDialog = false },
            onFilterSelected = { viewModel.setFilter(it) },
        )
    }
}

@Composable
private fun OrderListContent(
    orders: List<OrderListItem>?,
    onFilterClick: () -> Unit,
    onOrderClick: (String) -> Unit,
    onNewOrder: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.orders_title),
                actions = {
                    AppIconButton(
                        imageVector = Icons.Default.FilterList,
                        onClick = onFilterClick,
                        contentDescription = stringResource(R.string.orders_filter_title),
                        modifier = Modifier.testTag(RepairAppTestTags.OrdersFilterButton),
                    )
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                if (showScrollToTop) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier.testTag(RepairAppTestTags.ScrollToTopButton),
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                }
                FloatingActionButton(onClick = onNewOrder) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
    ) { paddingValues ->
        if (orders == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(RepairAppSpacing.screenHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.orders_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.orders_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(paddingValues).testTag("orders_list"),
                contentPadding = PaddingValues(RepairAppSpacing.screenHorizontal),
            ) {
                items(orders, key = { it.order.id }) { item ->
                    OrderRow(item, onClick = { onOrderClick(item.order.id) })
                    Spacer(Modifier.height(RepairAppSpacing.compactGap))
                }
            }
        }
    }
}

@Composable
private fun OrderRow(
    item: OrderListItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    StandardCard(
        onClick = onClick,
        modifier = Modifier.testTag(RepairAppTestTags.orderCard(item.order.id)),
    ) {
        Column(Modifier.padding(RepairAppSpacing.screenHorizontal)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.order.orderNumber, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                AppStatusBadge(
                    text = item.order.status.label(context),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(item.order.title, style = MaterialTheme.typography.titleMedium)
            Text(
                item.customer?.displayName ?: stringResource(R.string.orders_customer_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
