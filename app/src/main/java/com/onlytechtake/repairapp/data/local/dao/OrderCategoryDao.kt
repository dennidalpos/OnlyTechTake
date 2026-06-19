package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderCategoryDao {
    @Upsert
    suspend fun upsert(category: OrderCategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<OrderCategoryEntity>)

    @Query("SELECT * FROM order_categories ORDER BY level ASC, parentId ASC, active DESC, name COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<OrderCategoryEntity>>

    @Query("SELECT * FROM order_categories WHERE active = 1 ORDER BY level ASC, parentId ASC, name COLLATE NOCASE ASC, id ASC")
    fun observeActiveCategories(): Flow<List<OrderCategoryEntity>>

    @Query("SELECT * FROM order_categories")
    suspend fun findAll(): List<OrderCategoryEntity>

    @Query("SELECT * FROM order_categories WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): OrderCategoryEntity?

    @Query("SELECT COUNT(*) FROM repair_orders WHERE categoryId IN (:categoryIds)")
    suspend fun countOrdersForCategories(categoryIds: List<String>): Int

    @Query("DELETE FROM order_categories WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query(
        """
        SELECT * FROM order_categories
        WHERE name = :name
            AND ((:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId)
        LIMIT 1
        """,
    )
    suspend fun findByParentAndName(
        parentId: String?,
        name: String,
    ): OrderCategoryEntity?
}
