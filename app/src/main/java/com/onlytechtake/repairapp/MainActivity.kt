package com.onlytechtake.repairapp

import android.content.Context
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.onlytechtake.repairapp.core.design.LocaleForcer
import com.onlytechtake.repairapp.core.design.RepairAppTheme
import com.onlytechtake.repairapp.navigation.RepairAppNavHost

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val isForceEnglish =
            LocaleForcer.forceEnglish ||
                System.getProperty("forceEnglish") == "true" ||
                (newBase.applicationContext as? RepairAppApplication)?.let { false } ?: false // Cannot easily check intent here

        if (isForceEnglish) {
            val locale = java.util.Locale.ENGLISH
            java.util.Locale.setDefault(locale)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocales(android.os.LocaleList(locale))
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val forceEnglishFromIntent = intent.getBooleanExtra("forceEnglish", false)
        if (forceEnglishFromIntent) {
            LocaleForcer.forceEnglish = true
        }

        if (LocaleForcer.forceEnglish || System.getProperty("forceEnglish") == "true") {
            val locale = java.util.Locale.ENGLISH
            java.util.Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocales(android.os.LocaleList(locale))
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }

        super.onCreate(savedInstanceState)

        val appContainer = (application as RepairAppApplication).appContainer

        // Default to dark style at system level to ensure dark IME and avoid white flash
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val uiSettings by appContainer.uiPreferences.settings.collectAsState(
                initial = null,
            )

            // Wait for settings to load to avoid theme flicker/loops on startup/recreation
            if (uiSettings == null) {
                return@setContent
            }

            val darkTheme = uiSettings!!.darkTheme

            // Force AppCompat night mode to keep IME (keyboard) in sync with app theme
            // Only update if it changes from the initial/current value to avoid loops
            LaunchedEffect(darkTheme) {
                val currentMode =
                    if (darkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                if (AppCompatDelegate.getDefaultNightMode() != currentMode) {
                    AppCompatDelegate.setDefaultNightMode(currentMode)
                }
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle =
                        if (darkTheme) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                            )
                        },
                    navigationBarStyle =
                        if (darkTheme) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                            )
                        },
                )
                onDispose {}
            }

            RepairAppTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val disableAutoSeed = intent.getBooleanExtra("disable_auto_seed", false)
                    RepairAppNavHost(
                        windowSizeClass = windowSizeClass,
                        appContainer = appContainer,
                        disableAutoSeed = disableAutoSeed,
                    )
                }
            }
        }
    }
}
