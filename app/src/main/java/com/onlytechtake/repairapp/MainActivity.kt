package com.onlytechtake.repairapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.onlytechtake.repairapp.core.design.RepairAppTheme
import com.onlytechtake.repairapp.core.di.DefaultAppContainer
import com.onlytechtake.repairapp.navigation.RepairAppNavHost

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = DefaultAppContainer(applicationContext)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val uiSettings by appContainer.uiPreferences.settings.collectAsState(initial = null)
            val darkTheme = uiSettings?.darkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()

            RepairAppTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RepairAppNavHost(
                        windowSizeClass = windowSizeClass,
                        appContainer = appContainer,
                    )
                }
            }
        }
    }
}
