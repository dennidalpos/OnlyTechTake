package com.onlytechtake.repairapp.features.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppMessageBanner
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.features.dashboard.components.*
import com.onlytechtake.repairapp.features.workspace.SharedWorkspaceViewModel
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass

@Composable
fun DashboardRoute(
    appInstanceKey: Int,
    onLogout: () -> Unit,
    onAppDataRestored: () -> Unit,
) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel(key = "dashboard_$appInstanceKey")
    val sharedViewModel: SharedWorkspaceViewModel = hiltViewModel(key = "shared_workspace_$appInstanceKey")

    val summary by dashboardViewModel.summary.collectAsState()
    val isBackupReminderDue by dashboardViewModel.isBackupReminderDue.collectAsState()
    val currentUser by dashboardViewModel.currentUser.collectAsState(null)

    DashboardScreen(
        summary = summary,
        isBackupReminderDue = isBackupReminderDue,
        currentUser = currentUser,
        sharedViewModel = sharedViewModel,
        onLogout = onLogout,
        onAppDataRestored = onAppDataRestored,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: AppSummary?,
    isBackupReminderDue: Boolean,
    currentUser: AuthenticatedUser?,
    sharedViewModel: SharedWorkspaceViewModel,
    onLogout: () -> Unit,
    onAppDataRestored: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val globalMessage by sharedViewModel.message.collectAsState()
    val isFormDirty by sharedViewModel.isFormDirty.collectAsState()

    val isAdmin = PermissionService.isAdmin(currentUser)

    val windowSizeClass = LocalWindowSizeClass.current
    val useNavRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val items =
        remember {
            listOf(
                DashboardNavigationItem(
                    DashboardNavRoute.Home,
                    Icons.Default.Home,
                    R.string.dashboard_home,
                    RepairAppTestTags.dashboardTab("HOME"),
                ),
                DashboardNavigationItem(
                    DashboardNavRoute.NewOrder,
                    Icons.Default.Add,
                    R.string.dashboard_new,
                    RepairAppTestTags.dashboardTab("NEW_ORDER"),
                ),
                DashboardNavigationItem(
                    DashboardNavRoute.Customers,
                    Icons.Default.Person,
                    R.string.label_customers,
                    RepairAppTestTags.dashboardTab("CUSTOMERS"),
                ),
                DashboardNavigationItem(
                    DashboardNavRoute.Orders(),
                    Icons.AutoMirrored.Filled.List,
                    R.string.dashboard_orders,
                    RepairAppTestTags.dashboardTab("ORDERS"),
                ),
                DashboardNavigationItem(
                    DashboardNavRoute.Settings(),
                    Icons.Default.Settings,
                    R.string.dashboard_config,
                    RepairAppTestTags.dashboardTab("SETTINGS"),
                ),
            )
        }

    var pendingRoute by remember { mutableStateOf<DashboardNavRoute?>(null) }
    var pendingIsReset by remember { mutableStateOf(value = false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(value = false) }

    fun isTabSelected(itemRoute: DashboardNavRoute): Boolean {
        val destination = navBackStackEntry?.destination ?: return false
        return when (itemRoute) {
            is DashboardNavRoute.Home -> {
                destination.hasRoute<DashboardNavRoute.Home>()
            }

            is DashboardNavRoute.NewOrder -> {
                destination.hasRoute<DashboardNavRoute.NewOrder>()
            }

            is DashboardNavRoute.Customers -> {
                destination.hasRoute<DashboardNavRoute.Customers>()
            }

            is DashboardNavRoute.Orders -> {
                destination.hierarchy.any {
                    it.hasRoute<DashboardNavRoute.Orders>() || it.hasRoute<DashboardNavRoute.OrderDetail>()
                }
            }

            is DashboardNavRoute.Settings -> {
                destination.hasRoute<DashboardNavRoute.Settings>()
            }

            else -> {
                false
            }
        }
    }

    fun isHome(): Boolean = navBackStackEntry?.destination?.hasRoute<DashboardNavRoute.Home>() == true

    fun isCloseIcon(): Boolean {
        val destination = navBackStackEntry?.destination ?: return false
        return destination.hasRoute<DashboardNavRoute.NewOrder>() || destination.hasRoute<DashboardNavRoute.OrderDetail>()
    }

    fun performNavigation(
        route: DashboardNavRoute,
        isReset: Boolean = false,
    ) {
        if (isReset) sharedViewModel.triggerReset()
        navController.navigate(route) {
            val startDest = navController.graph.findStartDestination()
            popUpTo(startDest.id) { saveState = true }
            launchSingleTop = true
            restoreState = false
        }
        sharedViewModel.setFormDirty(dirty = false)
    }

    fun handleNavigation(
        route: DashboardNavRoute,
        isReset: Boolean,
    ) {
        val currentRouteIsNewOrder = navBackStackEntry?.destination?.hasRoute<DashboardNavRoute.NewOrder>() == true
        if (currentRouteIsNewOrder && isFormDirty) {
            pendingRoute = route
            pendingIsReset = isReset
            showUnsavedChangesDialog = true
        } else {
            if (isFormDirty) sharedViewModel.triggerAutoSave()
            performNavigation(route, isReset = isReset)
        }
    }

    BackHandler(enabled = !isHome()) {
        if (isFormDirty) {
            pendingRoute = null
            showUnsavedChangesDialog = true
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                title = summary?.title ?: stringResource(R.string.app_name),
                showLogo = isHome(),
                onBack = {
                    if (isFormDirty) {
                        pendingRoute = null
                        showUnsavedChangesDialog = true
                    } else {
                        navController.popBackStack()
                    }
                },
                currentUser = currentUser,
                onLogout = onLogout,
                isHome = isHome(),
                isCloseIcon = isCloseIcon(),
            )
        },
        bottomBar = {
            if (!useNavRail) {
                DashboardBottomBar(
                    items = items,
                    isTabSelected = ::isTabSelected,
                ) { handleNavigation(it.route, isTabSelected(it.route)) }
            }
        },
    ) { paddingValues ->
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues),
        ) {
            if (useNavRail) {
                DashboardNavRail(
                    items = items,
                    isTabSelected = ::isTabSelected,
                ) { handleNavigation(it.route, isTabSelected(it.route)) }
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .imePadding(),
            ) {
                globalMessage?.let { message ->
                    DismissibleAppMessageBanner(
                        message = message.text,
                        type = if (message.isError) BannerType.Error else BannerType.Success,
                        onDismiss = sharedViewModel::clearMessage,
                        autoDismissMillis = message.autoDismissMillis ?: (if (message.isError) 8000L else 4000L),
                        modifier =
                            Modifier.padding(
                                horizontal = RepairAppSpacing.screenHorizontal,
                                vertical = RepairAppSpacing.compactGap,
                            ),
                    )
                }

                if (isBackupReminderDue && isAdmin) {
                    AppMessageBanner(
                        message = stringResource(R.string.home_backup_reminder_title),
                        type = BannerType.Warning,
                        modifier =
                            Modifier.padding(
                                horizontal = RepairAppSpacing.screenHorizontal,
                                vertical = RepairAppSpacing.compactGap,
                            ),
                        action = {
                            AppButton(
                                text = stringResource(R.string.dashboard_config),
                                onClick = { handleNavigation(DashboardNavRoute.Settings(section = "EXPORT"), isReset = false) },
                                variant = AppButtonVariant.Ghost,
                                size = AppButtonSize.Compact,
                            )
                        },
                    )
                }

                DashboardNavHost(
                    navController = navController,
                    summary = summary,
                    sharedViewModel = sharedViewModel,
                    isAdmin = isAdmin,
                    onAppDataRestored = onAppDataRestored,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDismiss = {
                showUnsavedChangesDialog = false
                pendingRoute = null
                pendingIsReset = false
            },
        ) {
            showUnsavedChangesDialog = false
            sharedViewModel.setFormDirty(dirty = false)
            pendingRoute?.let { performNavigation(it, isReset = pendingIsReset) } ?: navController.popBackStack()
            pendingRoute = null
            pendingIsReset = false
        }
    }

    DemoBanner()
}
