package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.features.dashboard.customers.CustomerPanel
import com.onlytechtake.repairapp.features.dashboard.customers.CustomersViewModel
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailViewModel
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailWorkspace
import com.onlytechtake.repairapp.features.dashboard.home.HomePanel
import com.onlytechtake.repairapp.features.dashboard.home.HomeViewModel
import com.onlytechtake.repairapp.features.dashboard.neworder.NewOrderViewModel
import com.onlytechtake.repairapp.features.dashboard.neworder.QuickActions
import com.onlytechtake.repairapp.features.dashboard.orders.OrdersPanel
import com.onlytechtake.repairapp.features.dashboard.orders.OrdersViewModel
import com.onlytechtake.repairapp.features.dashboard.settings.SettingsPanel
import com.onlytechtake.repairapp.features.dashboard.settings.SettingsViewModel
import com.onlytechtake.repairapp.features.workspace.SharedWorkspaceViewModel

@Composable
fun DashboardNavHost(
    navController: NavHostController,
    summary: AppSummary?,
    sharedViewModel: SharedWorkspaceViewModel,
    isAdmin: Boolean,
    onAppDataRestored: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val context = androidx.compose.ui.platform.LocalContext.current

    NavHost(
        navController = navController,
        startDestination = DashboardRoutes.HOME,
        modifier = modifier,
    ) {
        composable(DashboardRoutes.HOME) {
            val homeViewModel: HomeViewModel =
                viewModel(
                    factory =
                        HomeViewModel.Factory(
                            authRepository = container.authRepository,
                            orderRepository = container.orderWorkflowRepository,
                            backupRepository = container.backupRepository,
                            backupReminderPreferences = container.backupReminderPreferences,
                        ),
                )
            HomePanel(
                summary = summary,
                viewModel = homeViewModel,
                onNewOrder = { navController.navigate(DashboardRoutes.NEW_ORDER) },
                onOpenQueue = { filter ->
                    val status = filter.status?.name ?: filter.statuses.firstOrNull()?.name
                    navController.navigate(
                        DashboardRoutes.orders(
                            status = status,
                            customerId = filter.customerId,
                        ),
                    ) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            ) { navController.navigate(DashboardRoutes.settings("EXPORT")) }
        }
        composable(DashboardRoutes.NEW_ORDER) {
            val newOrderViewModel: NewOrderViewModel =
                viewModel(
                    factory =
                        NewOrderViewModel.Factory(
                            context = context,
                            authRepository = container.authRepository,
                            customerRepository = container.customerRepository,
                            catalogRepository = container.catalogRepository,
                            orderMediaRepository = container.orderMediaRepository,
                            templateRepository = container.templateRepository,
                            createIntakeOrderUseCase = container.createIntakeOrderUseCase,
                            diagnosticLogger = container.diagnosticLogger,
                            onSetFormDirty = { dirty -> sharedViewModel.setFormDirty(dirty) },
                        ),
                )
            QuickActions(
                viewModel = newOrderViewModel,
                sharedViewModel = sharedViewModel,
                onOpenConfig = {
                    navController.navigate(
                        DashboardRoutes.settings(
                            "CATEGORIES",
                            fromIntake = true,
                        ),
                    )
                },
            ) { navController.popBackStack() }
        }
        composable(DashboardRoutes.CUSTOMERS) {
            val customersViewModel: CustomersViewModel =
                viewModel(
                    factory =
                        CustomersViewModel.Factory(
                            context = context,
                            authRepository = container.authRepository,
                            customerRepository = container.customerRepository,
                            orderWorkflowRepository = container.orderWorkflowRepository,
                            diagnosticLogger = container.diagnosticLogger,
                            onSetFormDirty = { dirty -> sharedViewModel.setFormDirty(dirty) },
                        ),
                )
            CustomerPanel(
                viewModel = customersViewModel,
                sharedViewModel = sharedViewModel,
            ) { orderId ->
                navController.navigate(DashboardRoutes.orderDetail(orderId))
            }
        }
        composable(
            route = DashboardRoutes.ORDERS,
            arguments =
                listOf(
                    navArgument("status") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("customerId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { backStackEntry ->
            val statusStr = backStackEntry.arguments?.getString("status")
            val customerId = backStackEntry.arguments?.getString("customerId")
            val status =
                statusStr?.let {
                    runCatching { OrderStatus.valueOf(it) }.getOrNull()
                }

            val ordersViewModel: OrdersViewModel =
                viewModel(
                    factory =
                        OrdersViewModel.Factory(
                            authRepository = container.authRepository,
                            orderRepository = container.orderWorkflowRepository,
                            backupRepository = container.backupRepository,
                            backupReminderPreferences = container.backupReminderPreferences,
                        ),
                )

            LaunchedEffect(status, customerId) {
                if ((status != null) || (customerId != null)) {
                    ordersViewModel.setFilter(
                        OrderFilter(
                            status = status,
                            customerId = customerId,
                        ),
                    )
                }
            }

            OrdersPanel(
                viewModel = ordersViewModel,
                sharedViewModel = sharedViewModel,
                onOrderClick = { orderId ->
                    navController.navigate(DashboardRoutes.orderDetail(orderId))
                },
                onNewOrder = { navController.navigate(DashboardRoutes.NEW_ORDER) },
                onOpenBackupSettings = {
                    navController.navigate(DashboardRoutes.settings("EXPORT"))
                },
            ) { dirty -> sharedViewModel.setFormDirty(dirty) }
        }
        composable(
            route = DashboardRoutes.ORDER_DETAIL,
            arguments =
                listOf(
                    navArgument("orderId") {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val orderDetailViewModel: OrderDetailViewModel =
                viewModel(
                    key = "order_detail_$orderId",
                    factory =
                        OrderDetailViewModel.Factory(
                            context = context,
                            orderId = orderId,
                            authRepository = container.authRepository,
                            orderRepository = container.orderWorkflowRepository,
                            orderMediaRepository = container.orderMediaRepository,
                            orderDocumentRepository = container.orderDocumentRepository,
                            templateRepository = container.templateRepository,
                            generateOrderStatusPdfUseCase = container.generateOrderStatusPdfUseCase,
                            shareDocumentIntentFactory = container.shareDocumentIntentFactory,
                            diagnosticLogger = container.diagnosticLogger,
                            onSetFormDirty = { dirty -> sharedViewModel.setFormDirty(dirty) },
                        ),
                )
            OrderDetailWorkspace(
                viewModel = orderDetailViewModel,
            ) { navController.popBackStack() }
        }
        composable(
            route = DashboardRoutes.SETTINGS,
            arguments =
                listOf(
                    navArgument("section") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("fromIntake") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section")
            val fromIntake = backStackEntry.arguments?.getBoolean("fromIntake") ?: false
            val settingsViewModel: SettingsViewModel =
                viewModel(
                    factory =
                        SettingsViewModel.Factory(
                            context = context,
                            authRepository = container.authRepository,
                            adminUserRepository = container.adminUserRepository,
                            catalogRepository = container.catalogRepository,
                            templateRepository = container.templateRepository,
                            backupRepository = container.backupRepository,
                            dataResetRepository = container.dataResetRepository,
                            backupReminderPreferences = container.backupReminderPreferences,
                            backupService = container.backupService,
                            ownerPreferences = container.ownerPreferences,
                            securityPreferences = container.securityPreferences,
                            uiPreferences = container.uiPreferences,
                            storageStatsService = container.storageStatsService,
                            diagnosticLogService = container.diagnosticLogService,
                            orderFileCleanupService = container.orderFileCleanupService,
                            diagnosticLogger = container.diagnosticLogger,
                            database = container.database,
                            onSetFormDirty = { dirty -> sharedViewModel.setFormDirty(dirty) },
                        ),
                )
            SettingsPanel(
                viewModel = settingsViewModel,
                isAdmin = isAdmin,
                initialSection = section,
                fromIntake = fromIntake,
                onBackupRestored = onAppDataRestored,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(DashboardRoutes.ADMIN) {
            val settingsViewModel: SettingsViewModel =
                viewModel(
                    factory =
                        SettingsViewModel.Factory(
                            context = context,
                            authRepository = container.authRepository,
                            adminUserRepository = container.adminUserRepository,
                            catalogRepository = container.catalogRepository,
                            templateRepository = container.templateRepository,
                            backupRepository = container.backupRepository,
                            dataResetRepository = container.dataResetRepository,
                            backupReminderPreferences = container.backupReminderPreferences,
                            backupService = container.backupService,
                            ownerPreferences = container.ownerPreferences,
                            securityPreferences = container.securityPreferences,
                            uiPreferences = container.uiPreferences,
                            storageStatsService = container.storageStatsService,
                            diagnosticLogService = container.diagnosticLogService,
                            orderFileCleanupService = container.orderFileCleanupService,
                            diagnosticLogger = container.diagnosticLogger,
                            database = container.database,
                            onSetFormDirty = { dirty -> sharedViewModel.setFormDirty(dirty) },
                        ),
                )
            SettingsPanel(
                viewModel = settingsViewModel,
                isAdmin = isAdmin,
                initialSection = "ADMIN",
                onBackupRestored = onAppDataRestored,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
