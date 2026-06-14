package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.dashboard.common.TouchButtonWithIcon
import com.onlytechtake.repairapp.features.workspace.ButtonTone

@Composable
fun CategoriesSettings(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
) {
    val categories by viewModel.categories.collectAsState()

    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryParentId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentCategoryParentId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryDescription by rememberSaveable { mutableStateOf("") }
    var categoryActive by rememberSaveable { mutableStateOf(value = true) }

    fun loadNewCategory() {
        categoryId = null
        categoryParentId = currentCategoryParentId
        categoryName = ""
        categoryDescription = ""
        categoryActive = true
    }

    fun selectCategoryForEdit(category: OrderCategoryEntity) {
        categoryId = category.id
        categoryParentId = category.parentId
        categoryName = category.name
        categoryDescription = category.description.orEmpty()
        categoryActive = category.active
    }

    fun openCategoryBranch(categoryId: String?) {
        currentCategoryParentId = categoryId
        loadNewCategory()
    }

    val currentParent = currentCategoryParentId?.let { id -> categories.firstOrNull { it.id == id } }
    val visibleCategories = categories.filter { it.parentId == currentCategoryParentId }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = DashboardSpacing.maxContentWidth),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle(stringResource(R.string.config_hub_categories_title))
            StandardCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                highlight = true,
            ) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    Text(
                        text = if (categoryId == null) {
                            if (currentCategoryParentId == null) "Nuova Categoria principale"
                            else "Nuova Sottocategoria"
                        } else {
                            "Modifica Categoria"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    CategoryBreadcrumb(categories, currentCategoryParentId)
                    ActionRow {
                        TouchButton(
                            text = stringResource(R.string.config_category_root),
                            tone = if (currentCategoryParentId == null) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                            onClick = { openCategoryBranch(null) },
                        )
                        if (currentParent?.parentId != null) {
                            TouchButton(
                                text = stringResource(R.string.config_category_up),
                                tone = ButtonTone.NEUTRAL,
                                onClick = { openCategoryBranch(currentParent.parentId) },
                            )
                        } else if (currentCategoryParentId != null) {
                            TouchButton(
                                text = stringResource(R.string.config_category_up),
                                tone = ButtonTone.NEUTRAL,
                                onClick = { openCategoryBranch(null) },
                            )
                        }
                    }
                    Field(
                        categoryName,
                        { categoryName = it },
                        stringResource(R.string.config_category_name_label),
                        testTag = RepairAppTestTags.ConfigCategoryNameField
                    )
                    Field(
                        categoryDescription,
                        { categoryDescription = it },
                        stringResource(R.string.config_category_description_label),
                        showOptional = true,
                        testTag = RepairAppTestTags.ConfigCategoryDescriptionField
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (categoryActive) stringResource(R.string.config_category_active) else stringResource(R.string.config_category_archived))
                        Switch(
                            checked = categoryActive,
                            enabled = isAdmin,
                            onCheckedChange = { categoryActive = it },
                        )
                    }
                    ActionRow {
                        TouchButtonWithIcon(
                            text = stringResource(R.string.config_category_new),
                            icon = Icons.Default.Add,
                            tone = ButtonTone.NEUTRAL,
                            testTag = RepairAppTestTags.ConfigCategoryNewButton,
                            onClick = ::loadNewCategory,
                        )
                        if ((categoryId != null) && isAdmin) {
                            TouchButtonWithIcon(
                                text = stringResource(R.string.config_category_open_branch),
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                tone = ButtonTone.NEUTRAL,
                                onClick = { openCategoryBranch(categoryId) },
                            )
                        }
                        if ((categoryId == null) || isAdmin) {
                            val confirmTitle = stringResource(R.string.config_category_confirm_save_title)
                            val confirmText = stringResource(R.string.config_category_confirm_save_text)
                            TouchButtonWithIcon(
                                text = stringResource(R.string.config_category_save),
                                icon = Icons.Default.Check,
                                testTag = RepairAppTestTags.ConfigCategorySaveButton,
                                onClick = {
                                    onRequestConfirm(confirmTitle, confirmText) {
                                        viewModel.saveCategory(
                                            id = categoryId,
                                            parentId = categoryParentId,
                                            name = categoryName,
                                            description = categoryDescription,
                                            active = categoryActive,
                                        )
                                        loadNewCategory()
                                    }
                                },
                            )
                        }
                        if ((categoryId != null) && isAdmin) {
                            val confirmTitle = stringResource(R.string.config_category_confirm_delete_title)
                            val confirmText = stringResource(R.string.config_category_confirm_delete_text)
                            TouchButtonWithIcon(
                                text = stringResource(R.string.config_category_delete),
                                icon = Icons.Default.Delete,
                                tone = ButtonTone.DANGER,
                                testTag = RepairAppTestTags.ConfigCategoryDeleteButton,
                                onClick = {
                                    onRequestConfirm(confirmTitle, confirmText) {
                                        viewModel.deleteCategory(categoryId!!)
                                        loadNewCategory()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        items(visibleCategories) { category ->
            val confirmTitle = stringResource(R.string.config_category_confirm_delete_title)
            val confirmText = stringResource(R.string.config_category_confirm_delete_specific_text).format(category.name)
            StandardCard(
                onClick = { openCategoryBranch(category.id) },
            ) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            LevelDot(category.level)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(category.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = (if (category.level == 0) stringResource(R.string.config_category_level_main) else stringResource(R.string.config_category_level_sub, category.level)) + " - " + (if (category.active) stringResource(R.string.config_category_active) else stringResource(R.string.config_category_archived)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isAdmin) {
                                IconButton(
                                    onClick = { selectCategoryForEdit(category) },
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifica")
                                }
                            }
                            IconButton(
                                onClick = { openCategoryBranch(category.id) },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Apri")
                            }
                            if (isAdmin) {
                                IconButton(onClick = {
                                    onRequestConfirm(
                                        confirmTitle,
                                        confirmText
                                    ) {
                                        viewModel.deleteCategory(category.id)
                                        loadNewCategory()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    category.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        if (visibleCategories.isEmpty()) {
            item {
                Text(stringResource(R.string.config_category_empty_branch), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
