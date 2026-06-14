package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import androidx.compose.ui.platform.testTag
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
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.features.workspace.SharedWorkspaceViewModel
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass

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
    val sharedViewModel: SharedWorkspaceViewModel = viewModel(
        key = "shared_workspace_$appInstanceKey",
        factory = SharedWorkspaceViewModel.Factory()
    )
    val summary by dashboardViewModel.summary.collectAsState()

    DashboardScreen(
        summary = summary,
        sharedViewModel = sharedViewModel,
        onLogout = onLogout,
        onAppDataRestored = onAppDataRestored,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: AppSummary,
    sharedViewModel: SharedWorkspaceViewModel,
    onLogout: () -> Unit,
    onAppDataRestored: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route ?: DashboardRoutes.HOME

    val globalMessage by sharedViewModel.message.collectAsState()
    val isFormDirty by sharedViewModel.isFormDirty.collectAsState()

    val container = LocalAppContainer.current
    val currentUser by container.authRepository.currentUser.collectAsState(null)
    val isAdmin = PermissionService.isAdmin(currentUser)

    val windowSizeClass = LocalWindowSizeClass.current
    val useNavRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val items = listOf(
        NavigationItem(DashboardRoutes.HOME, Icons.Default.Home, R.string.dashboard_home, RepairAppTestTags.dashboardTab("HOME")),
        NavigationItem(DashboardRoutes.NEW_ORDER, Icons.Default.Add, R.string.dashboard_new, RepairAppTestTags.dashboardTab("NEW_ORDER")),
        NavigationItem(DashboardRoutes.CUSTOMERS, Icons.Default.Person, R.string.dashboard_customers, RepairAppTestTags.dashboardTab("CUSTOMERS")),
        NavigationItem(DashboardRoutes.ORDERS, Icons.AutoMirrored.Filled.List, R.string.dashboard_orders, RepairAppTestTags.dashboardTab("ORDERS")),
        NavigationItem(DashboardRoutes.SETTINGS, Icons.Default.Settings, R.string.dashboard_config, RepairAppTestTags.dashboardTab("SETTINGS")),
    )

    var pendingRoute by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var showUnsavedChangesDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(value = false) }

    fun performNavigation(route: String) {
        navController.navigate(route) {
            popUpTo(DashboardRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        sharedViewModel.setFormDirty(dirty = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = summary.title) },
                navigationIcon = {
                    if (currentDestination != DashboardRoutes.HOME) {
                        val isCloseIcon = currentDestination == DashboardRoutes.NEW_ORDER || 
                                         currentDestination.startsWith("order_detail/")
                        IconButton(
                            modifier = Modifier.testTag(RepairAppTestTags.ConfigBackButton),
                            onClick = {
                                if (isFormDirty) {
                                    pendingRoute = null // Pop back
                                    showUnsavedChangesDialog = true
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Icon(
                                if (isCloseIcon) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                },
                actions = {
                    currentUser?.let { user ->
                        Text(
                            text = if (PermissionService.isAdmin(user)) "Admin" else "Operatore",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag(RepairAppTestTags.LogoutButton)
                    ) {
                        Text(text = stringResource(R.string.logout))
                    }
                },
            )
        },
        bottomBar = {
            if (!useNavRail) {
                NavigationBar(
                    modifier = Modifier.testTag(RepairAppTestTags.DashboardTabs)
                ) {
                    items.forEach { item ->
                        val selected = currentDestination == item.route
                        NavigationBarItem(
                            modifier = Modifier.testTag(item.testTag),
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    if (isFormDirty) {
                                        pendingRoute = item.route
                                        showUnsavedChangesDialog = true
                                    } else {
                                        performNavigation(item.route)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            if (useNavRail) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag(RepairAppTestTags.DashboardTabs),
                ) {
                    Spacer(Modifier.weight(1f))
                    items.forEach { item ->
                        val selected = currentDestination == item.route
                        NavigationRailItem(
                            modifier = Modifier.testTag(item.testTag),
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    if (isFormDirty) {
                                        pendingRoute = item.route
                                        showUnsavedChangesDialog = true
                                    } else {
                                        performNavigation(item.route)
                                    }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .imePadding()
            ) {
                globalMessage?.let { message ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        text = message.text,
                        color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }

                DashboardNavHost(
                    navController = navController,
                    summary = summary,
                    sharedViewModel = sharedViewModel,
                    isAdmin = isAdmin,
                    onAppDataRestored = onAppDataRestored,
                    onLogout = onLogout,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Dati non salvati") },
            text = { Text("Ci sono modifiche non salvate. Vuoi uscire comunque perdendo i dati o continuare a compilare?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedChangesDialog = false
                        sharedViewModel.setFormDirty(dirty = false)
                        pendingRoute?.let {
                            performNavigation(it)
                        } ?: navController.popBackStack()
                    }
                ) {
                    Text("Esci e perdi dati")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text("Continua compilazione")
                }
            }
        )
    }
}

private data class NavigationItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
    val testTag: String,
)
