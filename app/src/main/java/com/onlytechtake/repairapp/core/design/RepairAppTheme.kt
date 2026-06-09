package com.onlytechtake.repairapp.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Charcoal = Color(0xFF102026)
private val CharcoalDark = Color(0xFF071216)
private val Teal = Color(0xFF0F9F92)
private val TealDark = Color(0xFF6FE7DC)
private val Amber = Color(0xFFF59E0B)
private val AmberDark = Color(0xFFFFC857)
private val Danger = Color(0xFFB3261E)
private val DangerDark = Color(0xFFFFB4AB)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F4F0),
    onPrimaryContainer = Color(0xFF003D37),
    secondary = Charcoal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE6E9),
    onSecondaryContainer = Charcoal,
    tertiary = Amber,
    onTertiary = Color(0xFF2F2200),
    tertiaryContainer = Color(0xFFFFE6B0),
    onTertiaryContainer = Color(0xFF2C1B00),
    background = Color(0xFFF6F8FA),
    onBackground = Charcoal,
    surface = Color(0xFFFFFFFF),
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFE7ECEF),
    onSurfaceVariant = Color(0xFF3F4B50),
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF69777C),
    outlineVariant = Color(0xFFC8D1D5),
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFA4F4EC),
    secondary = Color(0xFFBFCAD0),
    onSecondary = CharcoalDark,
    secondaryContainer = Color(0xFF27383E),
    onSecondaryContainer = Color(0xFFDCE7ED),
    tertiary = AmberDark,
    onTertiary = Color(0xFF3C2800),
    tertiaryContainer = Color(0xFF5B3D00),
    onTertiaryContainer = Color(0xFFFFDEA1),
    background = Color(0xFF071216),
    onBackground = Color(0xFFE4ECEF),
    surface = Color(0xFF102026),
    onSurface = Color(0xFFE4ECEF),
    surfaceVariant = Color(0xFF334449),
    onSurfaceVariant = Color(0xFFC5D0D5),
    error = DangerDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E9BA1),
    outlineVariant = Color(0xFF3F4B50),
)

@Composable
fun RepairAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
