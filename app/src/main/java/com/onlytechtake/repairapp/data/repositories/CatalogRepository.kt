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
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeCategories(): Flow<List<OrderCategoryEntity>> = categoryDao.observeAll()

    fun observeActiveCategories(): Flow<List<OrderCategoryEntity>> = categoryDao.observeActiveCategories()

    suspend fun findCategory(id: String): OrderCategoryEntity? = categoryDao.findById(id)

    suspend fun saveCategory(actor: AuthenticatedUser, input: CategoryInput): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            require(input.name.isNotBlank()) { "Nome categoria obbligatorio" }
            val parent = input.parentId?.let {
                requireNotNull(categoryDao.findById(it)) { "Categoria padre non trovata" }
            }
            val level = (parent?.level ?: -1) + 1
            require(level in 0..2) { "Sono supportati al massimo tre livelli categoria" }
            val normalizedName = input.name.trim()
            val duplicate = categoryDao.findByParentAndName(input.parentId, normalizedName)
            require(duplicate == null || duplicate.id == input.id) { "Categoria gia' presente in questo ramo" }
            categoryDao.upsert(
                OrderCategoryEntity(
                    id = input.id ?: UUID.randomUUID().toString(),
                    parentId = input.parentId,
                    level = level,
                    name = normalizedName,
                    description = input.description.trim().ifBlank { null },
                    active = input.active,
                    sortOrder = input.sortOrder,
                ),
            )
        }
}
