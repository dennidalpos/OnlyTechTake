package com.onlytechtake.repairapp.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class RepairAppSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val whatsapp: Color,
    val onWhatsapp: Color,
    val paid: Color,
    val paidContainer: Color,
    val categoryLevels: List<Color>,
    val stepCompleted: Color,
    val stepInactive: Color,
)

internal val LightSemanticColors = RepairAppSemanticColors(
    success = Color(0xFF237A3B),
    onSuccess = Color.White,
    successContainer = Color(0xFFD8F8DE),
    onSuccessContainer = Color(0xFF002108),
    warning = Color(0xFFE8A600),
    onWarning = Color(0xFF2A2100),
    warningContainer = Color(0xFFFFE08A),
    onWarningContainer = Color(0xFF241A00),
    whatsapp = Color(0xFF128C4A),
    onWhatsapp = Color.White,
    paid = Color(0xFF237A3B),
    paidContainer = Color(0xFFE8F5E9),
    categoryLevels = listOf(
        Color(0xFF1976D2),
        Color(0xFF388E3C),
        Color(0xFFF57C00),
        Color(0xFF7B1FA2),
        Color(0xFF616161),
    ),
    stepCompleted = Color(0xFF237A3B),
    stepInactive = Color(0xFFBFC9C8),
)

internal val DarkSemanticColors = RepairAppSemanticColors(
    success = Color(0xFF83DA91),
    onSuccess = Color(0xFF003913),
    successContainer = Color(0xFF075224),
    onSuccessContainer = Color(0xFF9CF7AA),
    warning = Color(0xFFFFC53D),
    onWarning = Color(0xFF432F00),
    warningContainer = Color(0xFF604500),
    onWarningContainer = Color(0xFFFFDF8B),
    whatsapp = Color(0xFF69DD91),
    onWhatsapp = Color(0xFF00391D),
    paid = Color(0xFF83DA91),
    paidContainer = Color(0xFF075224),
    categoryLevels = listOf(
        Color(0xFF8FC7FF),
        Color(0xFF83DA91),
        Color(0xFFFFB77B),
        Color(0xFFE5B5F4),
        Color(0xFFCACACA),
    ),
    stepCompleted = Color(0xFF83DA91),
    stepInactive = Color(0xFF3F4948),
)

internal val LocalRepairAppSemanticColors = staticCompositionLocalOf {
    LightSemanticColors
}

object RepairAppSpacing {
    val screenHorizontal = 16.dp
    val screenVertical = 16.dp
    val sectionGap = 24.dp
    val cardGap = 16.dp
    val cardPadding = 16.dp
    val fieldGap = 12.dp
    val actionGap = 16.dp
    val compactGap = 8.dp
    val bottomContentPadding = 80.dp
    
    val maxContentWidthCompact = 600.dp
    val maxContentWidthMedium = 840.dp
    val maxContentWidthExpanded = 1200.dp
    
    // Deprecated/Legacy aliases to avoid immediate breakage
    val screenPadding = screenHorizontal
    val listGap = cardGap
    val formGap = fieldGap
    val maxContentWidth = maxContentWidthMedium
    val dialogElevation = 6.dp
}

object RepairAppSizes {
    val touchTarget = 48.dp
    val buttonDefault = 48.dp
    val buttonCompact = 36.dp
    val iconButton = 40.dp
    val intakeStepperDot = 32.dp
    val orderStepperDot = 12.dp
    val metricCardHeight = 100.dp
}

object RepairAppShapes {
    val screenContainer = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val card = RoundedCornerShape(16.dp)
    val cardSelected = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(12.dp)
    val compactButton = RoundedCornerShape(8.dp)
    val pill = RoundedCornerShape(50)
    val chip = RoundedCornerShape(8.dp)
    val badge = RoundedCornerShape(4.dp)
    val field = RoundedCornerShape(12.dp)
    val dialog = RoundedCornerShape(28.dp)
}
