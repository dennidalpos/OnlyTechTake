package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.features.dashboard.common.*

@Composable
fun ColumnScope.CategoriesSettings(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    fromIntake: Boolean = false,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val categories by viewModel.categories.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showEditCard by rememberSaveable { mutableStateOf(false) }
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
        showEditCard = true
    }

    fun selectCategoryForEdit(category: OrderCategoryEntity) {
        categoryId = category.id
        categoryParentId = category.parentId
        categoryName = category.name
        categoryDescription = category.description.orEmpty()
        categoryActive = category.active
        showEditCard = true
    }

    fun openCategoryBranch(categoryId: String?) {
        currentCategoryParentId = categoryId
        showEditCard = false
        searchQuery = ""
    }

    val currentParent = currentCategoryParentId?.let { id -> categories.firstOrNull { it.id == id } }
    
    BackHandler(enabled = currentCategoryParentId != null || showEditCard || searchQuery.isNotEmpty()) {
        when {
            showEditCard -> {
                showEditCard = false
                viewModel.setFormDirty(false)
            }
            searchQuery.isNotEmpty() -> searchQuery = ""
            currentCategoryParentId != null -> currentCategoryParentId = currentParent?.parentId
        }
    }

    val visibleCategories = remember(categories, currentCategoryParentId, searchQuery) {
        if (searchQuery.isNotBlank()) {
            categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            categories.filter { it.parentId == currentCategoryParentId }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .widthIn(max = RepairAppSpacing.maxContentWidthMedium)
            .testTag("categories_list"),
        contentPadding = PaddingValues(
            horizontal = RepairAppSpacing.screenHorizontal,
            vertical = RepairAppSpacing.screenHorizontal
        ),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
    ) {
        item {
            SectionTitle(
                text = stringResource(R.string.config_hub_categories_title),
                tooltip = stringResource(R.string.config_category_tooltip)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                AppTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.config_category_search_label),
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            AppIconButton(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.content_description_clear),
                                onClick = { searchQuery = "" }
                            )
                        }
                    }
                )

                if (searchQuery.isBlank()) {
                    CategoryBreadcrumb(categories, currentCategoryParentId)
                    if (currentCategoryParentId != null) {
                        ActionRow {
                            AppButton(
                                text = stringResource(R.string.config_category_root),
                                variant = AppButtonVariant.Secondary,
                                onClick = { openCategoryBranch(null) },
                            )
                            currentParent?.parentId?.let { parentId ->
                                AppButton(
                                    text = stringResource(R.string.config_category_up),
                                    variant = AppButtonVariant.Secondary,
                                    onClick = { openCategoryBranch(parentId) },
                                )
                            } ?: run {
                                AppButton(
                                    text = stringResource(R.string.config_category_up),
                                    variant = AppButtonVariant.Secondary,
                                    onClick = { openCategoryBranch(null) },
                                )
                            }
                        }
                    }
                }
                
                if (!showEditCard && searchQuery.isBlank()) {
                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = if (currentCategoryParentId == null) stringResource(R.string.config_category_new_root) else stringResource(R.string.config_category_new_sub),
                        leadingIcon = Icons.Default.Add,
                        variant = AppButtonVariant.Primary,
                        testTag = RepairAppTestTags.ConfigCategoryNewButton,
                        onClick = ::loadNewCategory
                    )
                }
            }
        }

        if (showEditCard) {
            item {
                StandardCard(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    highlight = true,
                ) {
                    Column(
                        modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (categoryId == null) stringResource(R.string.config_category_dialog_title_new) else stringResource(R.string.config_category_dialog_title_edit),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AppIconButton(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.content_description_close),
                                onClick = { 
                                    showEditCard = false
                                    viewModel.setFormDirty(false)
                                }
                            )
                        }
                        
                        AppTextField(
                            categoryName,
                            { 
                                categoryName = it
                                viewModel.setFormDirty(true)
                            },
                            stringResource(R.string.config_category_name_label),
                            testTag = RepairAppTestTags.ConfigCategoryNameField
                        )
                        AppTextField(
                            categoryDescription,
                            { 
                                categoryDescription = it
                                viewModel.setFormDirty(true)
                            },
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
                                onCheckedChange = { 
                                    categoryActive = it
                                    viewModel.setFormDirty(true)
                                },
                            )
                        }
                        ActionRow {
                            AppButton(
                                text = stringResource(R.string.customer_cancel),
                                variant = AppButtonVariant.Secondary,
                                onClick = { 
                                    showEditCard = false
                                    viewModel.setFormDirty(false)
                                }
                            )
                            if ((categoryId == null) || isAdmin) {
                                val confirmTitle = stringResource(R.string.config_category_confirm_save_title)
                                val confirmText = stringResource(R.string.config_category_confirm_save_text)
                                AppButton(
                                    text = stringResource(R.string.config_category_save),
                                    leadingIcon = Icons.Default.Check,
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
                                            showEditCard = false
                                        }
                                    },
                                )
                            }
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
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
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
                                AppIconButton(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.content_description_edit),
                                    onClick = { selectCategoryForEdit(category) }
                                )
                            }
                            AppIconButton(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.content_description_open),
                                onClick = { openCategoryBranch(category.id) }
                            )
                            if (isAdmin) {
                                AppIconButton(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.content_description_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        onRequestConfirm(
                                            confirmTitle,
                                            confirmText
                                        ) {
                                            viewModel.deleteCategory(category.id)
                                            loadNewCategory()
                                        }
                                    }
                                )
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

        item {
            Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                AppButton(
                    modifier = Modifier.fillMaxWidth().padding(top = RepairAppSpacing.screenHorizontal),
                    text = stringResource(R.string.config_category_close),
                    variant = AppButtonVariant.Secondary,
                    onClick = onBack
                )
                if (fromIntake) {
                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.config_category_return_to_intake),
                        variant = AppButtonVariant.Primary,
                        onClick = onBack
                    )
                }
            }
        }
    }
}


