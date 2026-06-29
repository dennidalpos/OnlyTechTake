package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppSemanticColors
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSizes
import com.onlytechtake.repairapp.core.design.repairColors

enum class AppButtonVariant {
    Primary,
    Secondary,
    Success,
    Info,
    Warning,
    Danger,
    WhatsApp,
    Ghost,
}

enum class AppButtonSize {
    Compact,
    Default,
    FullWidth,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Default,
    selected: Boolean? = null,
    stateDescription: String? = null,
    testTag: String? = null,
    leadingIcon: ImageVector? = null,
) {
    val compact = size == AppButtonSize.Compact
    val repairColors = MaterialTheme.repairColors
    val colorScheme = MaterialTheme.colorScheme
    val colors =
        remember(variant, colorScheme, repairColors) {
            variant.colors(colorScheme, repairColors)
        }
    val widthModifier = if (size == AppButtonSize.FullWidth) Modifier.fillMaxWidth() else Modifier
    val effectiveEnabled = enabled && !loading

    val containerColor =
        if (effectiveEnabled || loading) {
            colors.container
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        }
    val contentColor =
        if (effectiveEnabled || loading) {
            colors.content
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        }
    val visualHeight = if (compact) RepairAppSizes.buttonCompact else RepairAppSizes.buttonDefault

    Surface(
        onClick = onClick,
        modifier =
            modifier
                .then(widthModifier)
                .heightIn(min = RepairAppSizes.touchTarget)
                .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
                .semantics {
                    role = Role.Button
                    selected?.let { this.selected = it }
                    stateDescription?.let { this.stateDescription = it }
                },
        enabled = effectiveEnabled,
        shape = if (compact) RepairAppShapes.compactButton else RepairAppShapes.button,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier =
                Modifier
                    .height(visualHeight)
                    .defaultMinSize(
                        minWidth = if (compact) 60.dp else 100.dp,
                        minHeight = if (compact) 0.dp else RepairAppSizes.touchTarget,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 12.dp else 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .padding(end = 8.dp)
                                .size(if (compact) 16.dp else 20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor,
                    )
                } else {
                    leadingIcon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .padding(end = 8.dp)
                                    .size(if (compact) 16.dp else 20.dp),
                        )
                    }
                }
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style =
                        (if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge).copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    softWrap = false,
                )
            }
        }
    }
}

private data class AppButtonColors(
    val container: Color,
    val content: Color,
)

private fun AppButtonVariant.colors(
    colorScheme: ColorScheme,
    sColors: RepairAppSemanticColors,
): AppButtonColors =
    when (this) {
        AppButtonVariant.Primary -> {
            AppButtonColors(colorScheme.primary, colorScheme.onPrimary)
        }

        AppButtonVariant.Secondary -> {
            AppButtonColors(
                colorScheme.secondaryContainer,
                colorScheme.onSecondaryContainer,
            )
        }

        AppButtonVariant.Success -> {
            AppButtonColors(
                sColors.success,
                sColors.onSuccess,
            )
        }

        AppButtonVariant.Info -> {
            AppButtonColors(
                colorScheme.tertiary,
                colorScheme.onTertiary,
            )
        }

        AppButtonVariant.Warning -> {
            AppButtonColors(
                sColors.warning,
                sColors.onWarning,
            )
        }

        AppButtonVariant.Danger -> {
            AppButtonColors(colorScheme.error, colorScheme.onError)
        }

        AppButtonVariant.WhatsApp -> {
            AppButtonColors(
                sColors.whatsapp,
                sColors.onWhatsapp,
            )
        }

        AppButtonVariant.Ghost -> {
            AppButtonColors(Color.Transparent, colorScheme.primary)
        }
    }
