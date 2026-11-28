package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.dao.OrderDocumentDao
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.data.orders.OrderFileCleanupService
import com.onlytechtake.repairapp.domain.models.OrderDocument
import com.onlytechtake.repairapp.domain.repositories.OrderDocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultOrderDocumentRepository
    @Inject
    constructor(
        private val context: Context,
        private val dao: OrderDocumentDao,
        private val fileCleanupService: OrderFileCleanupService,
    ) : OrderDocumentRepository {
        override suspend fun save(document: OrderDocument) {
            dao.upsert(
                OrderDocumentEntity(
                    id = document.id,
                    orderId = document.orderId,
                    type = document.type,
                    localPath = document.localPath,
                    fileName = document.fileName,
                    mimeType = document.mimeType,
                    sizeBytes = document.sizeBytes,
                    includesCustomerSignature = document.includesCustomerSignature,
                    createdByUserId = document.createdByUserId,
                    createdAt = document.createdAt,
                    sharedAt = document.sharedAt,
                ),
            )
        }

        override suspend fun markShared(
            documentId: String,
            sharedAt: Long,
        ) {
            dao.markShared(documentId, sharedAt)
        }

        override suspend fun findById(id: String): OrderDocument? = dao.getById(id)?.toDomain()

        override fun observeByOrder(orderId: String): Flow<List<OrderDocument>> = dao.observeByOrder(orderId).map { list -> list.map { it.toDomain() } }

        override suspend fun delete(documentId: String): Result<Unit> =
            runCatching {
                val doc =
                    dao.getById(documentId)
                        ?: throw Exception(context.getString(R.string.error_document_not_found))
                dao.deleteById(documentId)
                fileCleanupService.deleteSpecificFile(doc.localPath)
            }
    }
