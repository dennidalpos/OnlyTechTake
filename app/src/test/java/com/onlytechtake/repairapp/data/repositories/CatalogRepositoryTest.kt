package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {
    private val stubDao = object : OrderCategoryDao {
        override suspend fun upsert(category: OrderCategoryEntity) = Unit
        override suspend fun upsertAll(categories: List<OrderCategoryEntity>) = Unit
        override fun observeAll() = TODO()
        override fun observeActiveCategories() = TODO()
        override suspend fun findAll(): List<OrderCategoryEntity> = emptyList()
        override suspend fun findById(id: String): OrderCategoryEntity? = null
        override suspend fun countOrdersForCategories(categoryIds: List<String>): Int = 0
        override suspend fun deleteById(id: String): Int = 0
        override suspend fun findByParentAndName(parentId: String?, name: String): OrderCategoryEntity? = null
    }

    private val repository = CatalogRepository(stubDao)
    private val admin = AuthenticatedUser("1", "admin", "Admin", UserRole.ADMIN)
    private val operator = AuthenticatedUser("2", "operator", "Operator", UserRole.OPERATOR)

    @Test
    fun operatorCanCreateNewCategory() = runBlocking {
        val input = CategoryInput(name = "New", description = "")
        val result = repository.saveCategory(operator, input)
        assertTrue(result.isSuccess)
    }

    @Test
    fun operatorCannotModifyExistingCategory() = runBlocking {
        val input = CategoryInput(id = "1", name = "Modified", description = "")
        val result = repository.saveCategory(operator, input)
        assertTrue(result.isFailure)
    }

    @Test
    fun adminCanModifyExistingCategory() = runBlocking {
        val input = CategoryInput(id = "1", name = "Modified", description = "")
        val result = repository.saveCategory(admin, input)
        assertTrue(result.isSuccess)
    }
}
