package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
        startDestination = DashboardNavRoute.Home,
        modifier = modifier,
    ) {
        composable<DashboardNavRoute.Home> {
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
                onNewOrder = { navController.navigate(DashboardNavRoute.NewOrder) },
            ) { filter ->
                val statusArg =
                    filter.status?.name
                        ?: if (filter.statuses.size == 1) filter.statuses.first().name else null
                val statusesArg =
                    if (filter.statuses.size > 1) filter.statuses.map { it.name } else null

                navController.navigate(
                    DashboardNavRoute.Orders(
                        status = statusArg,
                        statuses = statusesArg,
                        customerId = filter.customerId,
                    ),
                ) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }
        composable<DashboardNavRoute.NewOrder> {
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
                        DashboardNavRoute.Settings(
                            section = "CATEGORIES",
                            fromIntake = true,
                        ),
                    )
                },
            ) { navController.popBackStack() }
        }
        composable<DashboardNavRoute.Customers> {
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
                navController.navigate(DashboardNavRoute.OrderDetail(orderId))
            }
        }
        composable<DashboardNavRoute.Orders> { backStackEntry ->
            val route = backStackEntry.toRoute<DashboardNavRoute.Orders>()

            val ordersViewModel: OrdersViewModel =
                viewModel(
                    factory =
                        OrdersViewModel.Factory(
                            authRepository = container.authRepository,
                            orderRepository = container.orderWorkflowRepository,
                            backupRepository = container.backupRepository,
                            backupReminderPreferences = container.backupReminderPreferences,
                            ordersPreferences = container.ordersPreferences,
                        ),
                )

            LaunchedEffect(route) {
                val status =
                    route.status?.let {
                        runCatching { OrderStatus.valueOf(it.uppercase()) }.getOrNull()
                    }
                val statuses =
                    route.statuses
                        ?.asSequence()
                        ?.mapNotNull {
                            runCatching { OrderStatus.valueOf(it.uppercase()) }.getOrNull()
                        }?.toSet() ?: emptySet()

                ordersViewModel.setFilter(
                    OrderFilter(
                        status = status,
                        statuses = statuses,
                        customerId = route.customerId,
                    ),
                )
            }

            OrdersPanel(
                viewModel = ordersViewModel,
                sharedViewModel = sharedViewModel,
                onOrderClick = { orderId ->
                    navController.navigate(DashboardNavRoute.OrderDetail(orderId))
                },
                onNewOrder = { navController.navigate(DashboardNavRoute.NewOrder) },
            ) { dirty -> sharedViewModel.setFormDirty(dirty) }
        }
        composable<DashboardNavRoute.OrderDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<DashboardNavRoute.OrderDetail>()
            val orderId = route.orderId
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
                sharedViewModel = sharedViewModel,
            ) { navController.popBackStack() }
        }
        composable<DashboardNavRoute.Settings> { backStackEntry ->
            val route = backStackEntry.toRoute<DashboardNavRoute.Settings>()
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
                initialSection = route.section,
                fromIntake = route.fromIntake,
                onBackupRestored = onAppDataRestored,
                onNavigate = { targetRoute -> navController.navigate(targetRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DashboardNavRoute.Admin> {
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
                onNavigate = { targetRoute -> navController.navigate(targetRoute) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
