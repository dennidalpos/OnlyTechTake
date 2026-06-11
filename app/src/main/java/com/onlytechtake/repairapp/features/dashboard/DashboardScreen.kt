package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModelFactory

@Composable
fun DashboardRoute(
    appInstanceKey: Int,
    onLogout: () -> Unit,
    onAppDataRestored: () -> Unit,
) {
    val container = LocalAppContainer.current
    val dashboardViewModel: DashboardViewModel = viewModel(
        key = "dashboard_$appInstanceKey",
        factory = DashboardViewModel.Factory(container.repairAppRepository),
    )
    val workspaceViewModel: WorkspaceViewModel = viewModel(
        key = "workspace_$appInstanceKey",
        factory = WorkspaceViewModelFactory(
            authRepository = container.authRepository,
            adminUserRepository = container.adminUserRepository,
            customerRepository = container.customerRepository,
            catalogRepository = container.catalogRepository,
            orderRepository = container.orderWorkflowRepository,
            orderMediaRepository = container.orderMediaRepository,
            signatureRepository = container.signatureRepository,
            templateRepository = container.templateRepository,
            backupRepository = container.backupRepository,
            dataResetRepository = container.dataResetRepository,
            createIntakeOrderUseCase = container.createIntakeOrderUseCase,
            orderDocumentRepository = container.orderDocumentRepository,
            generateOrderStatusPdfUseCase = container.generateOrderStatusPdfUseCase,
            shareDocumentIntentFactory = container.shareDocumentIntentFactory,
        ),
    )
    val summary by dashboardViewModel.summary.collectAsState()

    DashboardScreen(
        summary = summary,
        workspaceViewModel = workspaceViewModel,
        onLogout = onLogout,
        onAppDataRestored = onAppDataRestored,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: AppSummary,
    workspaceViewModel: WorkspaceViewModel,
    onLogout: () -> Unit,
    onAppDataRestored: () -> Unit,
) {
    val uiState by workspaceViewModel.uiState.collectAsState()
    val currentUser by workspaceViewModel.currentUser.collectAsState()
    val orderTabDestination = if (uiState.selectedOrderId != null) {
        WorkspaceDestination.ORDER_DETAIL to stringResource(R.string.dashboard_order)
    } else {
        WorkspaceDestination.ORDERS to stringResource(R.string.dashboard_orders)
    }
    val destinations = listOf(
        WorkspaceDestination.HOME to stringResource(R.string.dashboard_home),
        WorkspaceDestination.NEW_ORDER to stringResource(R.string.dashboard_new),
        WorkspaceDestination.CUSTOMERS to stringResource(R.string.dashboard_customers),
        orderTabDestination,
        WorkspaceDestination.CONFIG to stringResource(R.string.dashboard_config),
        WorkspaceDestination.ADMIN to stringResource(R.string.dashboard_admin),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = summary.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { 
                        when (uiState.destination) {
                            WorkspaceDestination.ORDER_DETAIL -> workspaceViewModel.selectOrder(null)
                            WorkspaceDestination.HOME -> onLogout() // Or some exit logic
                            else -> workspaceViewModel.navigateTo(WorkspaceDestination.HOME)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(text = stringResource(R.string.logout))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
        ) {
            uiState.message?.let { message ->
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    text = message.text,
                    color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }

            val selectedIndex = destinations
                .indexOfFirst { it.first == uiState.destination }
                .coerceIn(0, destinations.lastIndex)
            SecondaryScrollableTabRow(
                modifier = Modifier.testTag(RepairAppTestTags.DashboardTabs),
                selectedTabIndex = selectedIndex,
            ) {
                destinations.forEach { (destination, title) ->
                    Tab(
                        modifier = Modifier.testTag(RepairAppTestTags.dashboardTab(destination.name)),
                        selected = uiState.destination == destination,
                        onClick = {
                            if (destination == WorkspaceDestination.NEW_ORDER) {
                                workspaceViewModel.openNewOrder()
                            } else {
                                workspaceViewModel.navigateTo(destination)
                            }
                        },
                        text = { Text(title) },
                    )
                }
            }

            when (uiState.destination) {
                WorkspaceDestination.HOME -> HomePanel(summary, workspaceViewModel)
                WorkspaceDestination.NEW_ORDER -> QuickActions(workspaceViewModel)
                WorkspaceDestination.CUSTOMERS -> CustomerPanel(workspaceViewModel)
                WorkspaceDestination.ORDERS -> OrdersPanel(workspaceViewModel)
                WorkspaceDestination.ORDER_DETAIL -> OrderDetailWorkspace(workspaceViewModel)
                WorkspaceDestination.CONFIG -> ConfigPanel(
                    viewModel = workspaceViewModel,
                    isAdmin = currentUser?.role == UserRole.ADMIN,
                    onBackupRestored = onAppDataRestored,
                )
                WorkspaceDestination.ADMIN -> AdminPanel(
                    viewModel = workspaceViewModel,
                    isAdmin = currentUser?.role == UserRole.ADMIN,
                    onResetComplete = onLogout,
                )
            }
        }
    }
}
