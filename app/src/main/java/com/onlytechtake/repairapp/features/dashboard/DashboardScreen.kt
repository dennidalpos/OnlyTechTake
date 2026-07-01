package com.onlytechtake.repairapp.features.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.RepairAppTheme
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppStatusBadge
import com.onlytechtake.repairapp.core.design.components.AppTopBar
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.core.di.FakeAppContainer
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
    val dashboardViewModel: DashboardViewModel =
        viewModel(
            key = "dashboard_$appInstanceKey",
            factory = DashboardViewModel.Factory(container.repairAppRepository),
        )
    val sharedViewModel: SharedWorkspaceViewModel =
        viewModel(
            key = "shared_workspace_$appInstanceKey",
            factory = SharedWorkspaceViewModel.Factory(),
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
    summary: AppSummary?,
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

    val items =
        androidx.compose.runtime.remember {
            listOf(
                NavigationItem(
                    DashboardRoutes.HOME,
                    Icons.Default.Home,
                    R.string.dashboard_home,
                    RepairAppTestTags.dashboardTab("HOME"),
                ),
                NavigationItem(
                    DashboardRoutes.NEW_ORDER,
                    Icons.Default.Add,
                    R.string.dashboard_new,
                    RepairAppTestTags.dashboardTab("NEW_ORDER"),
                ),
                NavigationItem(
                    DashboardRoutes.CUSTOMERS,
                    Icons.Default.Person,
                    R.string.label_customers,
                    RepairAppTestTags.dashboardTab("CUSTOMERS"),
                ),
                NavigationItem(
                    DashboardRoutes.orders(),
                    Icons.AutoMirrored.Filled.List,
                    R.string.dashboard_orders,
                    RepairAppTestTags.dashboardTab("ORDERS"),
                ),
                NavigationItem(
                    DashboardRoutes.settings(),
                    Icons.Default.Settings,
                    R.string.dashboard_config,
                    RepairAppTestTags.dashboardTab("SETTINGS"),
                ),
            )
        }

    var pendingRoute by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<String?>(
            null,
        )
    }
    var pendingIsReset by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            value = false,
        )
    }
    var showUnsavedChangesDialog by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            value = false,
        )
    }

    fun isTabSelected(
        itemRoute: String,
        currentRoute: String?,
    ): Boolean {
        if (currentRoute == null) return false
        val baseRoute = itemRoute.split("?")[0]
        val baseCurrent = currentRoute.split("?")[0]
        if (baseRoute == baseCurrent) return true
        return when (baseRoute) {
            DashboardRoutes.ORDERS.split("?")[0] -> {
                baseCurrent ==
                    DashboardRoutes.ORDER_DETAIL.split(
                        "?",
                    )[0]
            }

            else -> {
                false
            }
        }
    }

    BackHandler(enabled = currentDestination != DashboardRoutes.HOME) {
        if (isFormDirty) {
            pendingRoute = null // Pop back
            showUnsavedChangesDialog = true
        } else {
            navController.popBackStack()
        }
    }

    fun performNavigation(
        route: String,
        isReset: Boolean = false,
    ) {
        if (isReset) {
            sharedViewModel.triggerReset()
        }
        navController.navigate(route) {
            val startDest = navController.graph.findStartDestination()
            popUpTo(startDest.id) {
                saveState = true
            }
            launchSingleTop = true
            // Restore state for everything EXCEPT the start destination
            // to ensure clicking Home always takes us to the root of Home
            restoreState = !isReset && (route != startDest.route)
        }
        sharedViewModel.setFormDirty(dirty = false)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = summary?.title ?: stringResource(R.string.app_name),
                showLogo = currentDestination == DashboardRoutes.HOME,
                navigationIcon = {
                    if (currentDestination != DashboardRoutes.HOME) {
                        val isCloseIcon =
                            (currentDestination == DashboardRoutes.NEW_ORDER) ||
                                (currentDestination.startsWith("order_detail/"))
                        AppIconButton(
                            modifier = Modifier.testTag(RepairAppTestTags.ConfigBackButton),
                            imageVector = if (isCloseIcon) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            tint = if (isCloseIcon) Color.Unspecified else MaterialTheme.colorScheme.primary,
                            contentDescription =
                                stringResource(
                                    if (isCloseIcon) R.string.action_close else R.string.action_back,
                                ),
                            onClick = {
                                if (isFormDirty) {
                                    pendingRoute = null // Pop back
                                    showUnsavedChangesDialog = true
                                } else {
                                    navController.popBackStack()
                                }
                            },
                        )
                    }
                },
                actions = {
                    currentUser?.let { user ->
                        val isAdminUser = PermissionService.isAdmin(user)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = RepairAppSpacing.compactGap),
                        ) {
                            androidx.compose.foundation.Image(
                                painter =
                                    androidx.compose.ui.res
                                        .painterResource(id = R.drawable.ic_brand_avatar),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.size(RepairAppSpacing.compactGap))
                            AppStatusBadge(
                                text =
                                    stringResource(
                                        if (isAdminUser) R.string.dashboard_role_admin else R.string.dashboard_role_operator,
                                    ),
                                containerColor = if (isAdminUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isAdminUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }
                    AppButton(
                        text = stringResource(R.string.logout),
                        onClick = onLogout,
                        variant = AppButtonVariant.Ghost,
                        size = AppButtonSize.Compact,
                        testTag = RepairAppTestTags.LogoutButton,
                    )
                },
            )
        },
        bottomBar = {
            if (!useNavRail) {
                NavigationBar(
                    modifier = Modifier.testTag(RepairAppTestTags.DashboardTabs),
                ) {
                    items.forEach { item ->
                        val selected = isTabSelected(item.route, currentDestination)
                        NavigationBarItem(
                            modifier = Modifier.testTag(item.testTag),
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            selected = selected,
                            onClick = {
                                if (isFormDirty) {
                                    pendingRoute = item.route
                                    pendingIsReset = selected
                                    showUnsavedChangesDialog = true
                                } else {
                                    performNavigation(item.route, isReset = selected)
                                }
                            },
                        )
                    }
                }
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
                NavigationRail(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .testTag(RepairAppTestTags.DashboardTabs),
                ) {
                    Spacer(Modifier.weight(1f))
                    items.forEach { item ->
                        val selected = isTabSelected(item.route, currentDestination)
                        NavigationRailItem(
                            modifier = Modifier.testTag(item.testTag),
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelRes),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            selected = selected,
                            onClick = {
                                if (isFormDirty) {
                                    pendingRoute = item.route
                                    pendingIsReset = selected
                                    showUnsavedChangesDialog = true
                                } else {
                                    performNavigation(item.route, isReset = selected)
                                }
                            },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
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
                        type = if (message.isError) BannerType.Error else BannerType.Info,
                        onDismiss = sharedViewModel::clearMessage,
                        autoDismissMillis = message.autoDismissMillis
                            ?: (if (message.isError) 8_000L else 4_000L),
                        modifier =
                            Modifier.padding(
                                horizontal = RepairAppSpacing.screenHorizontal,
                                vertical = RepairAppSpacing.compactGap,
                            ),
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
        AppDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = stringResource(R.string.dashboard_unsaved_title),
            text = stringResource(R.string.dashboard_unsaved_text),
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.dashboard_unsaved_leave),
                    onClick = {
                        showUnsavedChangesDialog = false
                        sharedViewModel.setFormDirty(dirty = false)
                        pendingRoute?.let {
                            performNavigation(it, isReset = pendingIsReset)
                        } ?: navController.popBackStack()
                        pendingRoute = null
                        pendingIsReset = false
                    },
                    variant = AppButtonVariant.Danger,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.DashboardDiscardChangesButton,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.dashboard_unsaved_continue),
                    onClick = {
                        showUnsavedChangesDialog = false
                        pendingRoute = null
                        pendingIsReset = false
                    },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }

    if (BuildConfig.IS_DEMO_VERSION) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(0.08f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = DemoMode.getVersionBanner(LocalContext.current),
                style = MaterialTheme.typography.displayLarge,
                color = Color.Gray,
                modifier = Modifier.rotate(-45f),
                softWrap = false,
            )
        }
    }
}

private data class NavigationItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
    val testTag: String,
)

@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    name = "Dark Mode",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DashboardScreenPreview() {
    CompositionLocalProvider(
        LocalWindowSizeClass provides
            WindowSizeClass.calculateFromSize(
                DpSize(1000.dp, 800.dp),
            ),
        LocalAppContainer provides
            FakeAppContainer(LocalContext.current),
    ) {
        RepairAppTheme {
            DashboardScreen(
                summary =
                    AppSummary(
                        title = "OnlyTechTake",
                        operatorCount = 1,
                        customerCount = 10,
                        activeOrderCount = 5,
                    ),
                sharedViewModel =
                    androidx.compose.runtime.remember { SharedWorkspaceViewModel() },
                onLogout = {},
            ) {
                // Preview: onAppDataRestored
            }
        }
    }
}
