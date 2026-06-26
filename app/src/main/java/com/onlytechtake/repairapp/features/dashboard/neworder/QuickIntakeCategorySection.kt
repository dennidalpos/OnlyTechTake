package com.onlytechtake.repairapp.features.dashboard.neworder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppMessageBanner
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.workspace.categoryPath

@Composable
fun QuickIntakeCategorySection(
    @Suppress("UNUSED_PARAMETER") stepNumber: Int,
    activeCategories: List<OrderCategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    onOpenConfig: () -> Unit,
) {
    var currentParentId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleCategories = activeCategories.filter { it.parentId == currentParentId }
    val currentParent = currentParentId?.let { id -> activeCategories.firstOrNull { it.id == id } }
    val selectedPath = selectedCategoryId?.let { categoryPath(activeCategories, it) }.orEmpty()

    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Text(
                text = stringResource(R.string.intake_step_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (activeCategories.isEmpty()) {
                AppMessageBanner(
                    message = stringResource(R.string.intake_no_active_categories),
                    type = BannerType.Error,
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.intake_create_category),
                    onClick = onOpenConfig,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentParentId != null) {
                        AppButton(
                            text = stringResource(R.string.action_back),
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Compact,
                            onClick = { currentParentId = currentParent?.parentId },
                        )
                        Text(
                            currentParent?.name.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(
                            stringResource(R.string.config_category_root),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    AppButton(
                        text = stringResource(R.string.intake_add_missing_category),
                        variant = AppButtonVariant.Secondary,
                        size = AppButtonSize.Compact,
                        onClick = onOpenConfig,
                    )
                }

                if (selectedPath.isBlank()) {
                    OperatorText(stringResource(R.string.intake_category_select_hint))
                } else {
                    OperatorText(stringResource(R.string.intake_category_selected, selectedPath))
                }

                Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.tinyGap)) {
                    visibleCategories.forEach { category ->
                        val hasChildren =
                            activeCategories.any { child -> child.parentId == category.id }
                        val isSelected = category.id == selectedCategoryId
                        val categoryState =
                            when {
                                hasChildren -> stringResource(R.string.intake_category_branch_state)
                                isSelected -> stringResource(R.string.intake_category_selected_state)
                                else -> stringResource(R.string.intake_category_not_selected_state)
                            }
                        StandardCard(
                            modifier =
                                Modifier
                                    .testTag(RepairAppTestTags.categoryButton(category.id))
                                    .semantics {
                                        selected = isSelected
                                        stateDescription = categoryState
                                    },
                            onClick = {
                                if (hasChildren) {
                                    currentParentId = category.id
                                } else {
                                    onCategorySelected(category.id)
                                }
                            },
                            containerColor =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                },
                            role = Role.RadioButton,
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                                    if (hasChildren) {
                                        Text(
                                            text = stringResource(R.string.intake_category_step_hint),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                if (hasChildren) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.intake_category_open),
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = stringResource(R.string.intake_category_selected_icon),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                if (visibleCategories.isEmpty()) {
                    Text(
                        stringResource(R.string.config_category_empty_branch),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
