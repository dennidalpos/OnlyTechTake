package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity

@Composable
fun QuickIntakeCategorySection(
    stepNumber: Int,
    activeCategories: List<OrderCategoryEntity>,
    selectedCategoryId: String?,
    selectedCategory: OrderCategoryEntity?,
    onCategorySelected: (String) -> Unit,
    onOpenConfig: () -> Unit,
) {
    var currentParentId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleCategories = activeCategories.filter { it.parentId == currentParentId }
    val currentParent = currentParentId?.let { id -> activeCategories.firstOrNull { it.id == id } }
    val selectedPath = selectedCategoryId?.let { categoryPath(activeCategories, it) }.orEmpty()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            StepSectionTitle(stepNumber, "Categoria e dati tecnici")
            if (activeCategories.isEmpty()) {
                Text(
                    text = "Configura almeno una categoria attiva prima di creare nuove commesse.",
                    color = MaterialTheme.colorScheme.error,
                )
                TouchButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Apri configurazione",
                    onClick = onOpenConfig,
                )
            } else {
                Text("Ramo: ${categoryPath(activeCategories, currentParentId) ?: "Macrocategorie"}")
                if (selectedPath.isNotBlank()) {
                    Text("Selezionata: $selectedPath")
                }
                ActionRow {
                    TouchButton(
                        text = "Radice",
                        tone = if (currentParentId == null) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                        onClick = { currentParentId = null },
                    )
                    currentParent?.let { parent ->
                        TouchButton(
                            text = "Su",
                            tone = ButtonTone.NEUTRAL,
                            onClick = { currentParentId = parent.parentId },
                        )
                    }
                }
                ActionRow {
                    visibleCategories.forEach { category ->
                        val hasChildren = activeCategories.any { it.parentId == category.id }
                        val selected = category.id == selectedCategoryId
                        TouchButton(
                            modifier = Modifier.semantics {
                                this.selected = selected
                                stateDescription = if (hasChildren) {
                                    "Ramo categoria"
                                } else if (selected) {
                                    "Selezionata, tocca per rimuovere"
                                } else {
                                    "Non selezionata"
                                }
                            },
                            text = if (hasChildren) "${category.name} >" else if (selected) "${category.name} *" else category.name,
                            tone = if (selected) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                            selected = selected,
                            stateDescription = if (hasChildren) {
                                "Ramo categoria"
                            } else if (selected) {
                                "Selezionata, tocca per rimuovere"
                            } else {
                                "Non selezionata"
                            },
                            testTag = RepairAppTestTags.categoryButton(category.id),
                            onClick = {
                                if (hasChildren) {
                                    currentParentId = category.id
                                } else {
                                    onCategorySelected(category.id)
                                }
                            },
                        )
                    }
                }
                if (visibleCategories.isEmpty()) {
                    Text("Nessuna sottocategoria attiva in questo ramo.")
                }
                selectedCategory?.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
            }
        }
    }
}

private fun categoryPath(categories: List<OrderCategoryEntity>, categoryId: String?): String? {
    if (categoryId == null) return null
    val byId = categories.associateBy { it.id }
    val names = mutableListOf<String>()
    var current = byId[categoryId]
    repeat(3) {
        if (current == null) return@repeat
        names += current.name
        current = current.parentId?.let(byId::get)
    }
    return names.asReversed().joinToString(" > ")
}
