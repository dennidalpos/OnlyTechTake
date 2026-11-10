package com.onlytechtake.repairapp.core.design

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DesignTokenContrastTest {
    @Test
    fun semanticActionColorsMeetTextContrast() {
        val pairs =
            listOf(
                ContrastPair(
                    "light success",
                    LightSemanticColors.success,
                    LightSemanticColors.onSuccess,
                ),
                ContrastPair(
                    "light warning",
                    LightSemanticColors.warning,
                    LightSemanticColors.onWarning,
                ),
                ContrastPair(
                    "light whatsapp",
                    LightSemanticColors.whatsapp,
                    LightSemanticColors.onWhatsapp,
                ),
                ContrastPair(
                    "dark success",
                    DarkSemanticColors.success,
                    DarkSemanticColors.onSuccess,
                ),
                ContrastPair(
                    "dark warning",
                    DarkSemanticColors.warning,
                    DarkSemanticColors.onWarning,
                ),
                ContrastPair(
                    "dark whatsapp",
                    DarkSemanticColors.whatsapp,
                    DarkSemanticColors.onWhatsapp,
                ),
            )

        pairs.forEach { pair ->
            assertTrue(
                "${pair.name} contrast ratio ${pair.ratio()} is below $MinimumTextContrast",
                pair.ratio() >= MinimumTextContrast,
            )
        }
    }

    @Test
    fun categoryLevelDotsRemainVisibleOnThemeBackgrounds() {
        val lightSurface = Color(0xFFF4FBFA)
        val darkSurface = Color(0xFF191C1C)

        LightSemanticColors.categoryLevels.forEachIndexed { index, color ->
            val ratio = contrastRatio(color, lightSurface)
            assertTrue(
                "light category level $index contrast ratio $ratio is below $MinimumUiContrast",
                ratio >= MinimumUiContrast,
            )
        }

        DarkSemanticColors.categoryLevels.forEachIndexed { index, color ->
            val ratio = contrastRatio(color, darkSurface)
            assertTrue(
                "dark category level $index contrast ratio $ratio is below $MinimumUiContrast",
                ratio >= MinimumUiContrast,
            )
        }
    }

    private data class ContrastPair(
        val name: String,
        val background: Color,
        val foreground: Color,
    ) {
        fun ratio(): Double = contrastRatio(background, foreground)
    }

    @Suppress("ConstPropertyName")
    private companion object {
        const val MinimumTextContrast = 4.5
        const val MinimumUiContrast = 3.0
    }
}

private fun contrastRatio(
    first: Color,
    second: Color,
): Double {
    val firstLuminance = first.relativeLuminance()
    val secondLuminance = second.relativeLuminance()
    return (max(firstLuminance, secondLuminance) + 0.05) /
        (min(firstLuminance, secondLuminance) + 0.05)
}

private fun Color.relativeLuminance(): Double {
    fun channel(value: Float): Double {
        val linear = value.toDouble()
        return if (linear <= 0.03928) {
            linear / 12.92
        } else {
            ((linear + 0.055) / 1.055).pow(2.4)
        }
    }

    return (0.2126 * channel(red)) +
        (0.7152 * channel(green)) +
        (0.0722 * channel(blue))
}
