package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.core.utils.capitalizeFirst
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class CatalogRepository(
    private val context: Context,
    private val categoryDao: OrderCategoryDao,
) {
    fun observeCategories(): Flow<List<OrderCategoryEntity>> {
        if (DemoMode.isEnabled) {
            return flowOf(DemoMode.demoCategories)
        }
        return categoryDao.observeAll()
    }

    suspend fun getCategoryPath(id: String): String {
        val all = if (DemoMode.isEnabled) DemoMode.demoCategories else categoryDao.findAll()
        val path = mutableListOf<String>()
        var currentId: String? = id
        while (currentId != null) {
            val category = all.find { it.id == currentId }
            currentId = if (category != null) {
                path.add(0, category.name)
                category.parentId
            } else {
                null
            }
        }
        return path.joinToString(context.getString(R.string.catalog_path_separator))
    }

    suspend fun saveCategory(actor: AuthenticatedUser, input: CategoryInput): Result<String> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_CATEGORIES)
            require(input.name.isNotBlank()) {
                context.getString(R.string.catalog_error_name_required)
            }
            val normalizedName = input.name.trim().capitalizeFirst()
            val duplicate = categoryDao.findByParentAndName(input.parentId, normalizedName)
            require((duplicate == null) || (duplicate.id == input.id)) {
                context.getString(R.string.catalog_error_duplicate_name)
            }
            val categoryId = input.id ?: UUID.randomUUID().toString()
            categoryDao.upsertAll(
                normalizeCategoryTree(
                    existing = categoryDao.findAll(),
                    input = input.copy(
                        name = normalizedName,
                        description = input.description.trim().capitalizeFirst(),
                    ),
                    generatedId = categoryId,
                    errorCycle = context.getString(R.string.catalog_error_cycle_detected),
                    errorNotFound = context.getString(R.string.catalog_error_not_found),
                    errorParentNotFound = context.getString(R.string.catalog_error_parent_not_found),
                    errorSelfParent = context.getString(R.string.catalog_error_self_parent),
                    errorDepthLimit = context.getString(R.string.catalog_error_depth_limit),
                ),
            )
            categoryId
        }

    suspend fun deleteCategory(actor: AuthenticatedUser, categoryId: String): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_CATEGORIES)
            val categories = categoryDao.findAll()
            require(categories.any { it.id == categoryId }) {
                context.getString(R.string.catalog_error_not_found)
            }
            val branch = categoryBranch(
                categories = categories,
                rootId = categoryId,
                errorCycle = context.getString(R.string.catalog_error_cycle_detected),
            )
            require(categoryDao.countOrdersForCategories(branch.map { it.id }) == 0) {
                context.getString(R.string.catalog_error_used_by_order)
            }
            branch
                .sortedByDescending { it.level }
                .forEach { category ->
                    categoryDao.deleteById(category.id)
                }
        }
}

internal fun categoryBranch(
    categories: List<OrderCategoryEntity>,
    rootId: String,
    errorCycle: String,
): List<OrderCategoryEntity> {
    val byParent = categories.groupBy { it.parentId }
    val branch = mutableListOf<OrderCategoryEntity>()
    val visited = mutableSetOf<String>()

    fun visit(id: String) {
        require(visited.add(id)) { errorCycle }
        val category = categories.firstOrNull { it.id == id } ?: return
        branch += category
        byParent[id].orEmpty().forEach { child -> visit(child.id) }
    }

    visit(rootId)
    return branch
}

internal fun normalizeCategoryTree(
    existing: List<OrderCategoryEntity>,
    input: CategoryInput,
    generatedId: String,
    errorCycle: String,
    errorNotFound: String,
    errorParentNotFound: String,
    errorSelfParent: String,
    errorDepthLimit: String,
): List<OrderCategoryEntity> {
    val categoryId = input.id ?: generatedId
    require(input.parentId != categoryId) { errorSelfParent }

    val categories = existing.associateBy { it.id }.toMutableMap()
    categories[categoryId] = OrderCategoryEntity(
        id = categoryId,
        parentId = input.parentId,
        level = 0,
        name = input.name.trim(),
        description = input.description.trim().ifBlank { null },
        active = input.active,
    )

    val resolvedLevels = mutableMapOf<String, Int>()
    val resolving = mutableSetOf<String>()

    fun resolveLevel(id: String): Int {
        resolvedLevels[id]?.let { return it }
        require(resolving.add(id)) { errorCycle }
        val category = requireNotNull(categories[id]) { errorNotFound }
        val level = category.parentId?.let { parentId ->
            require(categories.containsKey(parentId)) { errorParentNotFound }
            resolveLevel(parentId) + 1
        } ?: 0
        require(level < 4) { errorDepthLimit }
        resolving.remove(id)
        resolvedLevels[id] = level
        return level
    }

    return categories.values.asSequence()
        .map { category -> category.copy(level = resolveLevel(category.id)) }
        .sortedWith(
            compareBy<OrderCategoryEntity> { it.level }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.id },
        )
        .toList()
}
