package com.onlytechtake.repairapp.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onlytechtake.repairapp.core.di.DefaultAppContainer
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.features.auth.AuthGateRoute

private object Routes {
    const val AUTH_GATE = "auth_gate"
}

@Composable
fun RepairAppNavHost(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var appInstanceKey by remember { mutableIntStateOf(0) }
    val appContainer =
        remember(context, appInstanceKey) {
            DefaultAppContainer(context.applicationContext)
        }
    val recreateAppContainer = {
        appContainer.close()
        appInstanceKey += 1
    }

    CompositionLocalProvider(
        LocalAppContainer provides appContainer,
        LocalWindowSizeClass provides windowSizeClass,
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.AUTH_GATE,
        ) {
            composable(Routes.AUTH_GATE) {
                AuthGateRoute(
                    appInstanceKey = appInstanceKey,
                    onAppDataRestored = recreateAppContainer,
                )
            }
        }
    }
}

val LocalWindowSizeClass =
    androidx.compose.runtime.staticCompositionLocalOf<WindowSizeClass> {
        error("No WindowSizeClass provided")
    }
