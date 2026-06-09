package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModelFactory

@Composable
fun DashboardRoute(onLogout: () -> Unit) {
    val container = LocalAppContainer.current
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(container.repairAppRepository),
    )
    val workspaceViewModel: WorkspaceViewModel = viewModel(
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
        ),
    )
    val summary by dashboardViewModel.summary.collectAsState()

    DashboardScreen(summary = summary, workspaceViewModel = workspaceViewModel, onLogout = onLogout)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: AppSummary,
    workspaceViewModel: WorkspaceViewModel,
    onLogout: () -> Unit,
) {
    val uiState by workspaceViewModel.uiState.collectAsState()
    val currentUser by workspaceViewModel.currentUser.collectAsState()
    val orderTabDestination = if (uiState.selectedOrderId != null) {
        WorkspaceDestination.ORDER_DETAIL to "Commessa"
    } else {
        WorkspaceDestination.ORDERS to "Commesse"
    }
    val destinations = listOf(
        WorkspaceDestination.HOME to "Home",
        WorkspaceDestination.NEW_ORDER to "Nuova",
        WorkspaceDestination.CUSTOMERS to "Clienti",
        orderTabDestination,
        WorkspaceDestination.CONFIG to "Config",
        WorkspaceDestination.ADMIN to "Admin",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = summary.title, maxLines = 1) },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(text = "Logout")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
            ScrollableTabRow(selectedTabIndex = selectedIndex) {
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
                WorkspaceDestination.CONFIG -> ConfigPanel(workspaceViewModel, isAdmin = currentUser?.role == UserRole.ADMIN)
                WorkspaceDestination.ADMIN -> AdminPanel(
                    viewModel = workspaceViewModel,
                    isAdmin = currentUser?.role == UserRole.ADMIN,
                    onResetComplete = onLogout,
                )
            }
        }
    }
}
