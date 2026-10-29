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
import com.onlytechtake.repairapp.core.utils.AppLocaleManager
import com.onlytechtake.repairapp.data.preferences.UiPreferences
import com.onlytechtake.repairapp.navigation.RepairAppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var uiPreferences: UiPreferences

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val forceEnglishFromIntent = intent.getBooleanExtra("forceEnglish", false)
        AppLocaleManager.initialize(this, forceEnglishFromIntent)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val uiSettings by uiPreferences.settings.collectAsState(initial = null)

            if (uiSettings == null) return@setContent
            val darkTheme = uiSettings!!.darkTheme

            RepairAppTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val disableAutoSeed = intent.getBooleanExtra("disable_auto_seed", false)
                    RepairAppNavHost(
                        windowSizeClass = windowSizeClass,
                        disableAutoSeed = disableAutoSeed,
                    )
                }
            }
        }
    }
}
