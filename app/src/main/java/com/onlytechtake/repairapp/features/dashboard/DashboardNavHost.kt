package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.onlytechtake.repairapp.features.dashboard.home.HomePanel
import com.onlytechtake.repairapp.features.dashboard.neworder.QuickActions
import com.onlytechtake.repairapp.features.dashboard.customers.CustomerPanel
import com.onlytechtake.repairapp.features.dashboard.orders.OrdersPanel
import com.onlytechtake.repairapp.features.dashboard.detail.OrderDetailWorkspace
import com.onlytechtake.repairapp.features.dashboard.settings.SettingsPanel
import com.onlytechtake.repairapp.features.dashboard.admin.AdminPanel
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.data.local.entities.UserRole

@Composable
fun DashboardNavHost(
    navController: NavHostController,
    summary: AppSummary,
    workspaceViewModel: WorkspaceViewModel,
    isAdmin: Boolean,
    onAppDataRestored: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = WorkspaceDestination.HOME.name,
        modifier = modifier
    ) {
        composable(WorkspaceDestination.HOME.name) {
            HomePanel(summary, workspaceViewModel, isAdmin)
        }
        composable(WorkspaceDestination.NEW_ORDER.name) {
            QuickActions(workspaceViewModel)
        }
        composable(WorkspaceDestination.CUSTOMERS.name) {
            CustomerPanel(workspaceViewModel)
        }
        composable(WorkspaceDestination.ORDERS.name) {
            OrdersPanel(workspaceViewModel)
        }
        composable(WorkspaceDestination.ORDER_DETAIL.name) {
            OrderDetailWorkspace(workspaceViewModel)
        }
        composable(WorkspaceDestination.SETTINGS.name) {
            SettingsPanel(
                viewModel = workspaceViewModel,
                isAdmin = isAdmin,
                onBackupRestored = onAppDataRestored
            )
        }
        composable(WorkspaceDestination.SETTINGS_CATEGORIES.name) {
            SettingsPanel(
                viewModel = workspaceViewModel,
                isAdmin = isAdmin,
                initialSection = "CATEGORIES",
                onBackupRestored = onAppDataRestored
            )
        }
        composable(WorkspaceDestination.SETTINGS_TEMPLATES.name) {
            SettingsPanel(
                viewModel = workspaceViewModel,
                isAdmin = isAdmin,
                initialSection = "TEMPLATES",
                onBackupRestored = onAppDataRestored
            )
        }
        composable(WorkspaceDestination.SETTINGS_WHATSAPP.name) {
            SettingsPanel(
                viewModel = workspaceViewModel,
                isAdmin = isAdmin,
                initialSection = "WHATSAPP",
                onBackupRestored = onAppDataRestored
            )
        }
        composable(WorkspaceDestination.SETTINGS_BACKUP.name) {
            SettingsPanel(
                viewModel = workspaceViewModel,
                isAdmin = isAdmin,
                initialSection = if (isAdmin) "EXPORT" else null,
                onBackupRestored = onAppDataRestored
            )
        }
        composable(WorkspaceDestination.SETTINGS_STORAGE.name) {
            SettingsPanel(
                viewModel = workspaceViewModel,
                isAdmin = isAdmin,
                initialSection = "STORAGE",
                onBackupRestored = onAppDataRestored
            )
        }
        composable(WorkspaceDestination.ADMIN.name) {
            AdminPanel(
                viewModel = workspaceViewModel,
                isAdmin = isAdmin,
                onResetComplete = onLogout
            )
        }
    }
}
