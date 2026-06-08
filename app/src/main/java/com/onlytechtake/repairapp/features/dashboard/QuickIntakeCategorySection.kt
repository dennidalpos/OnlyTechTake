package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
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
                ActionRow {
                    activeCategories.forEach { category ->
                        val selected = category.id == selectedCategoryId
                        TouchButton(
                            modifier = Modifier.semantics {
                                this.selected = selected
                                stateDescription = if (selected) "Selezionata" else "Non selezionata"
                            },
                            text = if (selected) "${category.name} *" else category.name,
                            tone = if (selected) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                            selected = selected,
                            onClick = { onCategorySelected(category.id) },
                        )
                    }
                }
                selectedCategory?.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
            }
        }
    }
}
