package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.repairColors

@Composable
internal fun LevelDot(level: Int) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(colorForLevel(level), CircleShape)
    )
}

@Composable
internal fun colorForLevel(level: Int): Color =
    MaterialTheme.repairColors.categoryLevels.getOrElse(level) {
        MaterialTheme.repairColors.categoryLevels.last()
    }

@Composable
internal fun CategoryBreadcrumb(categories: List<OrderCategoryEntity>, parentId: String?) {
    val byId = categories.associateBy { it.id }
    val nodes = mutableListOf<OrderCategoryEntity>()
    var current = parentId?.let(byId::get)
    while (current != null) {
        nodes.add(0, current)
        current = current.parentId?.let(byId::get)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(stringResource(R.string.config_category_root), style = MaterialTheme.typography.bodySmall)
        nodes.forEach { node ->
            Text(">", style = MaterialTheme.typography.bodySmall)
            LevelDot(node.level)
            Text(node.name, style = MaterialTheme.typography.bodySmall)
        }
    }
}
