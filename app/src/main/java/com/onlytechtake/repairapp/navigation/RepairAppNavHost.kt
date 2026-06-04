package com.onlytechtake.repairapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onlytechtake.repairapp.core.di.AppContainer
import com.onlytechtake.repairapp.core.di.LocalAppContainer
import com.onlytechtake.repairapp.features.auth.AuthGateRoute

private object Routes {
    const val AuthGate = "auth_gate"
}

@Composable
fun RepairAppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appContainer = remember(context) {
        AppContainer(context.applicationContext)
    }

    CompositionLocalProvider(LocalAppContainer provides appContainer) {
        NavHost(
            navController = navController,
            startDestination = Routes.AuthGate,
        ) {
            composable(Routes.AuthGate) {
                AuthGateRoute()
            }
        }
    }
}
