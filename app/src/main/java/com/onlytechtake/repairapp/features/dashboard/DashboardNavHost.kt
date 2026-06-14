package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.features.dashboard.home.HomePanel
import com.onlytechtake.repairapp.features.dashboard.home.HomeViewModel
import com.onlytechtake.repairapp.features.dashboard.neworder.QuickActions
import com.onlytechtake.repairapp.features.dashboard.neworder.NewOrderViewModel
import com.onlytechtake.repairapp.features.dashboard.customers.CustomerPanel
import com.onlytechtake.repairapp.features.dashboard.customers.CustomersViewModel
import com.onlytechtake.repairapp.features.dashboard.orders.OrdersPanel
import com.onlytechtake.repairapp.features.dashboard.orders.OrdersViewModel
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailWorkspace
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailViewModel
import com.onlytechtake.repairapp.features.dashboard.settings.SettingsPanel
import com.onlytechtake.repairapp.features.dashboard.settings.SettingsViewModel
import com.onlytechtake.repairapp.features.dashboard.admin.AdminPanel
import com.onlytechtake.repairapp.features.dashboard.admin.AdminViewModel
import com.onlytechtake.repairapp.features.workspace.SharedWorkspaceViewModel
import com.onlytechtake.repairapp.domain.models.AppSummary

@Composable
fun DashboardNavHost(
    navController: NavHostController,
    summary: AppSummary,
    sharedViewModel: SharedWorkspaceViewModel,
    isAdmin: Boolean,
    onAppDataRestored: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current

    NavHost(
        navController = navController,
        startDestination = DashboardRoutes.HOME,
        modifier = modifier
    ) {
        composable(DashboardRoutes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    authRepository = container.authRepository,
                    orderRepository = container.orderWorkflowRepository,
                    backupRepository = container.backupRepository,
                    backupReminderPreferences = container.backupReminderPreferences,
                )
            )
            HomePanel(
                summary = summary,
                viewModel = homeViewModel,
                isAdmin = isAdmin,
                onNewOrder = { navController.navigate(DashboardRoutes.NEW_ORDER) },
                onSelectOrder = { orderId -> navController.navigate(DashboardRoutes.orderDetail(orderId)) },
                onOpenQueue = { filter ->
                    val status = filter.status?.name ?: filter.statuses.firstOrNull()?.name
                    navController.navigate(DashboardRoutes.orders(status = status, customerId = filter.customerId))
                },
                onOpenBackupSettings = { navController.navigate(DashboardRoutes.settings("EXPORT")) }
            )
        }
        composable(DashboardRoutes.NEW_ORDER) {
            val newOrderViewModel: NewOrderViewModel = viewModel(
                factory = NewOrderViewModel.Factory(
                    authRepository = container.authRepository,
                    customerRepository = container.customerRepository,
                    catalogRepository = container.catalogRepository,
                    orderMediaRepository = container.orderMediaRepository,
                    templateRepository = container.templateRepository,
                    createIntakeOrderUseCase = container.createIntakeOrderUseCase,
                    onSetFormDirty = { dirty -> sharedViewModel.setFormDirty(dirty) }
                )
            )
            QuickActions(
                viewModel = newOrderViewModel,
                onOpenConfig = { navController.navigate(DashboardRoutes.settings("CATEGORIES")) },
                onClose = { navController.popBackStack() }
            )
        }
        composable(DashboardRoutes.CUSTOMERS) {
            val customersViewModel: CustomersViewModel = viewModel(
                factory = CustomersViewModel.Factory(
                    authRepository = container.authRepository,
                    customerRepository = container.customerRepository,
                    orderWorkflowRepository = container.orderWorkflowRepository,
                    onSetFormDirty = { dirty -> sharedViewModel.setFormDirty(dirty) }
                )
            )
            CustomerPanel(
                viewModel = customersViewModel,
                onOrderClick = { orderId -> navController.navigate(DashboardRoutes.orderDetail(orderId)) }
            )
        }
        composable(
            route = DashboardRoutes.ORDERS,
            arguments = listOf(
                navArgument("status") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null },
                navArgument("customerId") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val statusStr = backStackEntry.arguments?.getString("status")
            val customerId = backStackEntry.arguments?.getString("customerId")
            val status = statusStr?.let { runCatching { com.onlytechtake.repairapp.data.local.entities.OrderStatus.valueOf(it) }.getOrNull() }

            val ordersViewModel: OrdersViewModel = viewModel(
                factory = OrdersViewModel.Factory(
                    authRepository = container.authRepository,
                    orderRepository = container.orderWorkflowRepository,
                )
            )
            
            androidx.compose.runtime.LaunchedEffect(status, customerId) {
                if (status != null || customerId != null) {
                    ordersViewModel.setFilter(com.onlytechtake.repairapp.domain.workspace.OrderFilter(
                        status = status,
                        customerId = customerId
                    ))
                }
            }

            OrdersPanel(
                viewModel = ordersViewModel,
                onOrderClick = { orderId ->
                    navController.navigate(DashboardRoutes.orderDetail(orderId))
                }
            )
        }
        composable(
            route = DashboardRoutes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val orderDetailViewModel: OrderDetailViewModel = viewModel(
                key = "order_detail_$orderId",
                factory = OrderDetailViewModel.Factory(
                    orderId = orderId,
                    authRepository = container.authRepository,
                    orderRepository = container.orderWorkflowRepository,
                    orderMediaRepository = container.orderMediaRepository,
                    orderDocumentRepository = container.orderDocumentRepository,
                    templateRepository = container.templateRepository,
                    generateOrderStatusPdfUseCase = container.generateOrderStatusPdfUseCase,
                    shareDocumentIntentFactory = container.shareDocumentIntentFactory,
                    onSetFormDirty = { dirty -> sharedViewModel.setFormDirty(dirty) }
                )
            )
            OrderDetailWorkspace(
                viewModel = orderDetailViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = DashboardRoutes.SETTINGS,
            arguments = listOf(navArgument("section") { 
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section")
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    authRepository = container.authRepository,
                    adminUserRepository = container.adminUserRepository,
                    catalogRepository = container.catalogRepository,
                    templateRepository = container.templateRepository,
                    backupRepository = container.backupRepository,
                    backupReminderPreferences = container.backupReminderPreferences,
                    storageStatsService = container.storageStatsService,
                    orderFileCleanupService = container.orderFileCleanupService,
                )
            )
            SettingsPanel(
                viewModel = settingsViewModel,
                isAdmin = isAdmin,
                initialSection = section,
                onBackupRestored = onAppDataRestored,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(DashboardRoutes.ADMIN) {
            val adminViewModel: AdminViewModel = viewModel(
                factory = AdminViewModel.Factory(
                    authRepository = container.authRepository,
                    adminUserRepository = container.adminUserRepository,
                    backupRepository = container.backupRepository,
                    dataResetRepository = container.dataResetRepository,
                )
            )
            AdminPanel(
                viewModel = adminViewModel,
                isAdmin = isAdmin,
                onResetComplete = onLogout
            )
        }
    }
}
