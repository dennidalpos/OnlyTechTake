package com.onlytechtake.repairapp.core.design

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat

private val Charcoal = Color(0xFF191C1D)
private val Teal = Color(0xFF006A6A)
private val TealDark = Color(0xFF4DDADA)
private val Amber = Color(0xFF7D5800)
private val AmberDark = Color(0xFFFFBA28)
private val Danger = Color(0xFFBA1A1A)
private val DangerDark = Color(0xFFFFB4AB)
private val AppTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

private val LightColors =
    lightColorScheme(
        primary = Teal,
        onPrimary = Color.White,
        primaryContainer = Color(0xFF6FF6F6),
        onPrimaryContainer = Color(0xFF002020),
        secondary = Color(0xFF4A6363),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCE8E7),
        onSecondaryContainer = Color(0xFF051F1F),
        tertiary = Amber,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFDF9E),
        onTertiaryContainer = Color(0xFF281900),
        background = Color(0xFFF4FBFA),
        onBackground = Charcoal,
        surface = Color(0xFFF4FBFA),
        onSurface = Charcoal,
        surfaceVariant = Color(0xFFDAE5E4),
        onSurfaceVariant = Color(0xFF3F4948),
        error = Danger,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF6F7979),
        outlineVariant = Color(0xFFBFC9C8),
    )

private val DarkColors =
    darkColorScheme(
        primary = TealDark,
        onPrimary = Color(0xFF003737),
        primaryContainer = Color(0xFF004F4F),
        onPrimaryContainer = Color(0xFF6FF6F6),
        secondary = Color(0xFFB0CCCC),
        onSecondary = Color(0xFF1B3435),
        secondaryContainer = Color(0xFF324B4C),
        onSecondaryContainer = Color(0xFFCCE8E7),
        tertiary = AmberDark,
        onTertiary = Color(0xFF422C00),
        tertiaryContainer = Color(0xFF5E4100),
        onTertiaryContainer = Color(0xFFFFDF9E),
        background = Color(0xFF191C1C),
        onBackground = Color(0xFFE0E3E3),
        surface = Color(0xFF191C1C),
        onSurface = Color(0xFFE0E3E3),
        surfaceVariant = Color(0xFF3F4948),
        onSurfaceVariant = Color(0xFFBFC9C8),
        error = DangerDark,
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF899392),
        outlineVariant = Color(0xFF3F4948),
    )

@Composable
fun RepairAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val forceEnglish = System.getProperty("forceEnglish") == "true"
    if (forceEnglish) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val resources = androidx.compose.ui.platform.LocalResources.current
        androidx.compose.runtime.remember(configuration) {
            if (configuration.locales[0] != java.util.Locale.ENGLISH) {
                val newConfig = Configuration(configuration)
                @Suppress("AppBundleLocaleChanges")
                newConfig.setLocale(java.util.Locale.ENGLISH)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(newConfig, resources.displayMetrics)
            }
            true
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Set light/dark appearance for status and navigation bars
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme

            // Disable contrast enforcement for a cleaner edge-to-edge look
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                @Suppress("DEPRECATION")
                window.isStatusBarContrastEnforced = false
            }

            // Set window background to match theme - helps the system IME to pick the right theme
            val color = if (darkTheme) DarkColors.background else LightColors.background
            window.setBackgroundDrawable(color.toArgb().toDrawable())

            // Explicitly set the decor view background as well
            window.decorView.setBackgroundColor(color.toArgb())
        }
    }

    CompositionLocalProvider(
        LocalRepairAppSemanticColors provides if (darkTheme) DarkSemanticColors else LightSemanticColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes =
                androidx.compose.material3.Shapes(
                    small = RepairAppShapes.compactButton,
                    medium = RepairAppShapes.card,
                    large = RepairAppShapes.card,
                ),
            content = content,
        )
    }
}

@Suppress("UnusedReceiverParameter")
val MaterialTheme.repairColors: RepairAppSemanticColors
    @Composable get() = LocalRepairAppSemanticColors.current
