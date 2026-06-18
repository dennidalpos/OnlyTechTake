package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppShapes

enum class StandardCardVariant {
    Outlined,
    Filled,
    Highlight,
}

enum class StandardCardDensity {
    Comfortable,
    Compact,
}

@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    highlight: Boolean = false,
    variant: StandardCardVariant = if (highlight) StandardCardVariant.Highlight else StandardCardVariant.Outlined,
    density: StandardCardDensity = StandardCardDensity.Comfortable,
    border: BorderStroke? = null,
    role: Role = Role.Button,
    content: @Composable ColumnScope.() -> Unit,
) {
    val effectiveBorder =
        border ?: when (variant) {
            StandardCardVariant.Highlight -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            StandardCardVariant.Outlined -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            StandardCardVariant.Filled -> null
        }
    val cardModifier = modifier.fillMaxWidth()
    val effectiveContainerColor =
        when (variant) {
            StandardCardVariant.Filled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
            else -> containerColor
        }
    val contentModifier = Modifier
    val cardContent: @Composable ColumnScope.() -> Unit = {
        androidx.compose.foundation.layout.Column(modifier = contentModifier) {
            content()
        }
    }

    if (onClick == null) {
        Card(
            modifier = cardModifier,
            border = effectiveBorder,
            shape = RepairAppShapes.card,
            colors = CardDefaults.cardColors(containerColor = effectiveContainerColor),
            content = cardContent,
        )
    } else {
        Card(
            modifier = cardModifier.semantics { this.role = role },
            onClick = onClick,
            border = effectiveBorder,
            shape = RepairAppShapes.card,
            colors = CardDefaults.cardColors(containerColor = effectiveContainerColor),
            content = cardContent,
        )
    }
}
