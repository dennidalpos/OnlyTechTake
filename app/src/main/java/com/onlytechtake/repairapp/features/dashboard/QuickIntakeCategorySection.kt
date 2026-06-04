package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        OutlinedButton(
                            onClick = { onCategorySelected(category.id) },
                        ) {
                            Text(if (category.id == selectedCategoryId) "${category.name} *" else category.name)
                        }
                    }
                }
                selectedCategory?.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                if (selectedCategoryId == null) {
                    Text("Seleziona una categoria per classificare la commessa.")
                }
            }
        }
    }
}
