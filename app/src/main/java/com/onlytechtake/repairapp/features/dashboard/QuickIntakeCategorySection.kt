package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.onlytechtake.repairapp.R
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            StepSectionTitle(stepNumber, stringResource(R.string.intake_step_category))
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
                if (currentParentId != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)
                    ) {
                        TouchButton(
                            text = stringResource(R.string.nav_back),
                            tone = ButtonTone.NEUTRAL,
                            onClick = { currentParentId = currentParent?.parentId },
                        )
                        Text(currentParent?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (selectedPath.isNotBlank()) {
                    OperatorText("Selezionata: $selectedPath")
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
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(DashboardSpacing.cardPadding)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                if (hasChildren) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Apri")
                                } else if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selezionata", tint = MaterialTheme.colorScheme.primary)
                                }
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

