package com.onlytechtake.repairapp.navigation

import android.app.Activity
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.features.auth.AuthGateViewModel
import com.onlytechtake.repairapp.features.dashboard.DashboardRoute
import kotlinx.serialization.Serializable

@Serializable sealed interface MainRoute {
    @Serializable data object Auth : MainRoute

    @Serializable data object Dashboard : MainRoute
}

@Composable
fun RepairAppNavHost(
    windowSizeClass: WindowSizeClass,
    disableAutoSeed: Boolean = false,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authViewModel: AuthGateViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.currentUser, authState.initialized) {
        if (authState.initialized && authState.currentUser == null) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && !currentRoute.contains("Auth")) {
                navController.navigate(MainRoute.Auth) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val onAppDataRestored = {
        RepairAppDatabase.closeInstance()
        (context as? Activity)?.recreate()
        Unit
    }

    CompositionLocalProvider(
        LocalWindowSizeClass provides windowSizeClass,
    ) {
        NavHost(
            navController = navController,
            startDestination = MainRoute.Auth,
        ) {
            composable<MainRoute.Auth> {
                AuthNavHost(
                    onAuthenticated = {
                        navController.navigate(MainRoute.Dashboard) {
                            popUpTo(MainRoute.Auth) { inclusive = true }
                        }
                    },
                    onAppDataRestored = onAppDataRestored,
                    onExit = { (context as? Activity)?.finish() },
                    disableAutoSeed = disableAutoSeed,
                )
            }
            composable<MainRoute.Dashboard> {
                DashboardRoute(
                    appInstanceKey = 0,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(MainRoute.Auth) {
                            popUpTo(MainRoute.Dashboard) { inclusive = true }
                        }
                    },
                    onAppDataRestored = onAppDataRestored,
                )
            }
        }
    }
}

val LocalWindowSizeClass =
    staticCompositionLocalOf<WindowSizeClass> {
        error("No WindowSizeClass provided")
    }
