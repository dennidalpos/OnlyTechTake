package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class CatalogRepository(
    private val categoryDao: OrderCategoryDao,
) {
    fun observeCategories(): Flow<List<OrderCategoryEntity>> = categoryDao.observeAll()

    fun observeActiveCategories(): Flow<List<OrderCategoryEntity>> = categoryDao.observeActiveCategories()

    suspend fun findCategory(id: String): OrderCategoryEntity? = categoryDao.findById(id)

    suspend fun saveCategory(actor: AuthenticatedUser, input: CategoryInput): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            require(input.name.isNotBlank()) { "Nome categoria obbligatorio" }
            val normalizedName = input.name.trim()
            val duplicate = categoryDao.findByParentAndName(input.parentId, normalizedName)
            require(duplicate == null || duplicate.id == input.id) { "Categoria gia' presente in questo ramo" }
            categoryDao.upsertAll(
                normalizeCategoryTree(
                    existing = categoryDao.findAll(),
                    input = input.copy(name = normalizedName),
                    generatedId = UUID.randomUUID().toString(),
                ),
            )
        }

    suspend fun deleteCategory(actor: AuthenticatedUser, categoryId: String): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            val categories = categoryDao.findAll()
            require(categories.any { it.id == categoryId }) { "Categoria non trovata" }
            val branch = categoryBranch(categories, categoryId)
            require(categoryDao.countOrdersForCategories(branch.map { it.id }) == 0) {
                "Categoria usata da una commessa"
            }
            branch
                .sortedByDescending { it.level }
                .forEach { category ->
                    categoryDao.deleteById(category.id)
                }
        }
}

internal fun categoryBranch(categories: List<OrderCategoryEntity>, rootId: String): List<OrderCategoryEntity> {
    val byParent = categories.groupBy { it.parentId }
    val branch = mutableListOf<OrderCategoryEntity>()
    val visited = mutableSetOf<String>()

    fun visit(id: String) {
        require(visited.add(id)) { "Il ramo categorie contiene un ciclo" }
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
): List<OrderCategoryEntity> {
    val categoryId = input.id ?: generatedId
    require(input.parentId != categoryId) { "Una categoria non puo' essere padre di se stessa" }

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
        require(resolving.add(id)) { "Il ramo categorie contiene un ciclo" }
        val category = requireNotNull(categories[id]) { "Categoria non trovata" }
        val level = category.parentId?.let { parentId ->
            require(categories.containsKey(parentId)) { "Categoria padre non trovata" }
            resolveLevel(parentId) + 1
        } ?: 0
        resolving.remove(id)
        resolvedLevels[id] = level
        return level
    }

    return categories.values
        .map { category -> category.copy(level = resolveLevel(category.id)) }
        .sortedWith(
            compareBy<OrderCategoryEntity> { it.level }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.id },
        )
}
