package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.OrderDocumentDao
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import kotlinx.coroutines.flow.Flow

interface OrderDocumentRepository {
    suspend fun save(document: OrderDocumentEntity)
    suspend fun markShared(documentId: String, sharedAt: Long)
    suspend fun findById(id: String): OrderDocumentEntity?
    fun observeByOrder(orderId: String): Flow<List<OrderDocumentEntity>>
}

class DefaultOrderDocumentRepository(
    private val dao: OrderDocumentDao
) : OrderDocumentRepository {
    override suspend fun save(document: OrderDocumentEntity) = dao.upsert(document)

    override suspend fun markShared(documentId: String, sharedAt: Long) {
        dao.markShared(documentId, sharedAt)
    }

    override suspend fun findById(id: String): OrderDocumentEntity? {
        return dao.getById(id)
    }

    override fun observeByOrder(orderId: String): Flow<List<OrderDocumentEntity>> {
        return dao.observeByOrder(orderId)
    }
}
