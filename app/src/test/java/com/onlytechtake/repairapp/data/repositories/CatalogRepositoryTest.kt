package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {
    private val stubDao =
        object : OrderCategoryDao {
            override suspend fun upsert(category: OrderCategoryEntity) = Unit

            override suspend fun upsertAll(categories: List<OrderCategoryEntity>) = Unit

            override fun observeAll() = emptyFlow<List<OrderCategoryEntity>>()

            override fun observeActiveCategories() = emptyFlow<List<OrderCategoryEntity>>()

            override suspend fun findAll(): List<OrderCategoryEntity> = emptyList()

            override suspend fun findById(id: String): OrderCategoryEntity? = null

            override suspend fun countOrdersForCategories(categoryIds: List<String>): Int = 0

            override suspend fun deleteById(id: String): Int = 0

            override suspend fun findByParentAndName(
                parentId: String?,
                name: String,
            ): OrderCategoryEntity? = null
        }

    private val context = mockk<Context>(relaxed = true)
    private val repository = CatalogRepository(context, stubDao)
    private val admin =
        AuthenticatedUser("1", "admin", "Admin", UserRole.ADMIN, UserPermission.entries.toSet())
    private val operator = AuthenticatedUser(
        "2",
        "operator",
        "Operator",
        UserRole.OPERATOR,
        setOf(UserPermission.MANAGE_CATEGORIES)
    )
    private val restrictedOperator =
        AuthenticatedUser("3", "restricted", "Restricted", UserRole.OPERATOR, emptySet())

    @Test
    fun operatorCanCreateNewCategoryIfHasPermission() =
        runBlocking {
            val input = CategoryInput(name = "New", description = "")
            val result = repository.saveCategory(operator, input)
            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().isNotBlank())
        }

    @Test
    fun operatorCannotCreateNewCategoryIfNoPermission() =
        runBlocking {
            val input = CategoryInput(name = "New", description = "")
            val result = repository.saveCategory(restrictedOperator, input)
            assertTrue(result.isFailure)
        }

    @Test
    fun restrictedOperatorCannotModifyExistingCategory() =
        runBlocking {
            val input = CategoryInput(id = "1", name = "Modified", description = "")
            val result = repository.saveCategory(restrictedOperator, input)
            assertTrue(result.isFailure)
        }

    @Test
    fun adminCanModifyExistingCategory() =
        runBlocking {
            val input = CategoryInput(id = "1", name = "Modified", description = "")
            val result = repository.saveCategory(admin, input)
            assertTrue(result.isSuccess)
            assertEquals("1", result.getOrThrow())
        }
}
