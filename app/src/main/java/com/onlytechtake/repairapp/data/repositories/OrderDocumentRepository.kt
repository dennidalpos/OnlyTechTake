package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.dao.OrderDocumentDao
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import kotlinx.coroutines.flow.Flow

interface OrderDocumentRepository {
    suspend fun save(document: OrderDocumentEntity)

    suspend fun markShared(
        documentId: String,
        sharedAt: Long,
    )

    suspend fun findById(id: String): OrderDocumentEntity?

    fun observeByOrder(orderId: String): Flow<List<OrderDocumentEntity>>

    suspend fun delete(documentId: String): Result<Unit>
}

class DefaultOrderDocumentRepository(
    private val context: Context,
    private val dao: OrderDocumentDao,
    private val fileCleanupService: OrderFileCleanupService,
) : OrderDocumentRepository {
    override suspend fun save(document: OrderDocumentEntity) = dao.upsert(document)

    override suspend fun markShared(
        documentId: String,
        sharedAt: Long,
    ) {
        dao.markShared(documentId, sharedAt)
    }

    override suspend fun findById(id: String): OrderDocumentEntity? = dao.getById(id)

    override fun observeByOrder(orderId: String): Flow<List<OrderDocumentEntity>> =
        dao.observeByOrder(orderId)

    override suspend fun delete(documentId: String): Result<Unit> =
        runCatching {
            val doc = dao.getById(documentId)
                ?: throw Exception(context.getString(R.string.error_document_not_found))
            dao.deleteById(documentId)
            fileCleanupService.deleteSpecificFile(doc.localPath)
        }
}
