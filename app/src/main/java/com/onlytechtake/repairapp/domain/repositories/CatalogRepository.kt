package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.Category
import com.onlytechtake.repairapp.domain.models.CategoryInput
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeCategories(): Flow<List<Category>>

    fun observeActiveCategories(): Flow<List<Category>>

    suspend fun getCategoryPath(id: String): String

    suspend fun saveCategory(
        actor: AuthenticatedUser,
        input: CategoryInput,
    ): Result<String>

    suspend fun deleteCategory(
        actor: AuthenticatedUser,
        categoryId: String,
    ): Result<Unit>
}
