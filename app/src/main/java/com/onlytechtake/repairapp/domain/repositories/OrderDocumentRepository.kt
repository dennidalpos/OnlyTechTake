package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.domain.models.OrderDocument
import kotlinx.coroutines.flow.Flow

interface OrderDocumentRepository {
    suspend fun save(document: OrderDocument)

    suspend fun markShared(
        documentId: String,
        sharedAt: Long,
    )

    suspend fun findById(id: String): OrderDocument?

    fun observeByOrder(orderId: String): Flow<List<OrderDocument>>

    suspend fun delete(documentId: String): Result<Unit>
}
