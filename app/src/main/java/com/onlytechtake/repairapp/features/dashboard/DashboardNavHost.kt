package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.models.OrderStatus
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
import com.onlytechtake.repairapp.features.workspace.WorkspaceManager

@Composable
fun DashboardNavHost(
    navController: NavHostController,
    summary: AppSummary?,
    dashboardViewModel: DashboardViewModel,
    isAdmin: Boolean,
    onAppDataRestored: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DashboardNavRoute.Home,
        modifier = modifier,
    ) {
        composable<DashboardNavRoute.Home> {
            val homeViewModel: HomeViewModel = hiltViewModel()
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
            val newOrderViewModel: NewOrderViewModel = hiltViewModel()
            QuickActions(
                viewModel = newOrderViewModel,
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
            val customersViewModel: CustomersViewModel = hiltViewModel()
            CustomerPanel(
                viewModel = customersViewModel,
            ) { orderId ->
                navController.navigate(DashboardNavRoute.OrderDetail(orderId))
            }
        }
        composable<DashboardNavRoute.Orders> { backStackEntry ->
            val route = backStackEntry.toRoute<DashboardNavRoute.Orders>()
            val ordersViewModel: OrdersViewModel = hiltViewModel()

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
                onOrderClick = { orderId ->
                    navController.navigate(DashboardNavRoute.OrderDetail(orderId))
                },
                onNewOrder = { navController.navigate(DashboardNavRoute.NewOrder) },
            ) { dirty -> dashboardViewModel.setFormDirty(dirty) }
        }
        composable<DashboardNavRoute.OrderDetail> {
            val orderDetailViewModel: OrderDetailViewModel = hiltViewModel()
            OrderDetailWorkspace(
                viewModel = orderDetailViewModel,
                showCloseButton = true,
            ) { navController.popBackStack() }
        }
        composable<DashboardNavRoute.Settings> { backStackEntry ->
            val route = backStackEntry.toRoute<DashboardNavRoute.Settings>()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
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
            val settingsViewModel: SettingsViewModel = hiltViewModel()
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
