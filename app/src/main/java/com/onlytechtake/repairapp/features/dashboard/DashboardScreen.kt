package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.di.LocalAppContainer
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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route ?: WorkspaceDestination.HOME.name

    val uiState by workspaceViewModel.uiState.collectAsState()
    val currentUser by workspaceViewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == UserRole.ADMIN

    val items = listOf(
        NavigationItem(WorkspaceDestination.HOME, Icons.Default.Home, R.string.dashboard_home),
        NavigationItem(WorkspaceDestination.NEW_ORDER, Icons.Default.Add, R.string.dashboard_new),
        NavigationItem(WorkspaceDestination.CUSTOMERS, Icons.Default.Person, R.string.dashboard_customers),
        NavigationItem(WorkspaceDestination.ORDERS, Icons.Default.List, R.string.dashboard_orders),
        NavigationItem(WorkspaceDestination.CONFIG, Icons.Default.Settings, R.string.dashboard_config),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = summary.title) },
                navigationIcon = {
                    if (currentDestination != WorkspaceDestination.HOME.name) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
                actions = {
                    currentUser?.let { user ->
                        Text(
                            text = if (user.role == UserRole.ADMIN) "Admin" else "Operatore",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    TextButton(onClick = onLogout) {
                        Text(text = stringResource(R.string.logout))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelRes)) },
                        selected = currentDestination == item.destination.name,
                        onClick = {
                            if (currentDestination != item.destination.name) {
                                navController.navigate(item.destination.name) {
                                    popUpTo(WorkspaceDestination.HOME.name) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
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

            DashboardNavHost(
                navController = navController,
                summary = summary,
                workspaceViewModel = workspaceViewModel,
                isAdmin = isAdmin,
                onAppDataRestored = onAppDataRestored,
                onLogout = onLogout,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private data class NavigationItem(
    val destination: WorkspaceDestination,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
)
