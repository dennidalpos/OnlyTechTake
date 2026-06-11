package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.categoryPath

@Composable
fun QuickIntakeCategorySection(
    stepNumber: Int,
    activeCategories: List<OrderCategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    onOpenConfig: () -> Unit,
) {
    var currentParentId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleCategories = activeCategories.filter { it.parentId == currentParentId }
    val currentParent = currentParentId?.let { id -> activeCategories.firstOrNull { it.id == id } }
    val selectedPath = selectedCategoryId?.let { categoryPath(activeCategories, it) }.orEmpty()
    val visibleLevel = currentParent?.level?.plus(1) ?: 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            StepSectionTitle(stepNumber, "Categoria")
            if (activeCategories.isEmpty()) {
                Text(
                    text = "Nessuna categoria attiva.",
                    color = MaterialTheme.colorScheme.error,
                )
                TouchButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Apri configurazione",
                    onClick = onOpenConfig,
                )
            } else {
                Text(
                    text = "Livello $visibleLevel",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = categoryBreadcrumb(activeCategories, currentParentId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selectedPath.isNotBlank()) {
                    Text("Selezionata: $selectedPath", color = MaterialTheme.colorScheme.primary)
                }
                ActionRow {
                    TouchButton(
                        text = "Radice",
                        tone = if (currentParentId == null) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                        enabled = currentParentId != null,
                        onClick = { currentParentId = null },
                    )
                    currentParent?.let { parent ->
                        TouchButton(
                            text = "Indietro",
                            tone = ButtonTone.NEUTRAL,
                            onClick = { currentParentId = parent.parentId },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
                    visibleCategories.forEach { category ->
                        val hasChildren = activeCategories.any { child -> child.parentId == category.id }
                        val isSelected = category.id == selectedCategoryId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(RepairAppTestTags.categoryButton(category.id))
                                .semantics {
                                    selected = isSelected
                                    stateDescription = when {
                                        hasChildren -> "Ramo categoria"
                                        isSelected -> "Categoria selezionata"
                                        else -> "Categoria non selezionata"
                                    }
                                }
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        if (hasChildren) {
                                            currentParentId = category.id
                                        } else {
                                            onCategorySelected(category.id)
                                        }
                                    },
                                ),
                        ) {
                            Column(
                                modifier = Modifier.padding(DashboardSpacing.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                            ) {
                                Text(category.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = when {
                                        hasChildren -> "Apri livello ${category.level + 1}"
                                        isSelected -> "Selezionata"
                                        else -> "Seleziona"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
                if (visibleCategories.isEmpty()) {
                    Text("Nessuna categoria in questo ramo.")
                }
            }
        }
    }
}

private fun categoryBreadcrumb(categories: List<OrderCategoryEntity>, parentId: String?): String =
    listOfNotNull("Radice", categoryPath(categories, parentId))
        .joinToString(" > ")
