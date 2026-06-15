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

@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    highlight: Boolean = false,
    border: BorderStroke? = null,
    role: Role = Role.Button,
    content: @Composable ColumnScope.() -> Unit,
) {
    val effectiveBorder = border ?: if (highlight) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    }
    val cardModifier = modifier.fillMaxWidth()

    if (onClick == null) {
        Card(
            modifier = cardModifier,
            border = effectiveBorder,
            shape = RepairAppShapes.card,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            content = content,
        )
    } else {
        Card(
            modifier = cardModifier.semantics { this.role = role },
            onClick = onClick,
            border = effectiveBorder,
            shape = RepairAppShapes.card,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            content = content,
        )
    }
}
