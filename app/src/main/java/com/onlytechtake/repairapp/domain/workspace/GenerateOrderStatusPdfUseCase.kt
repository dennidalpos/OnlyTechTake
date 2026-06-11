package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfGenerator
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfPayload
import com.onlytechtake.repairapp.core.utils.formatEuroCents
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentType
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.OrderDocumentFileStorage
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class GenerateOrderStatusPdfUseCase(
    private val context: Context,
    private val workflowRepository: OrderWorkflowRepository,
    private val catalogRepository: CatalogRepository,
    private val documentRepository: OrderDocumentRepository,
    private val userDao: UserDao,
    private val fileStorage: OrderDocumentFileStorage,
    private val pdfGenerator: OrderStatusPdfGenerator = OrderStatusPdfGenerator(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        orderId: String,
        userId: String,
        includeCustomerSignature: Boolean = true,
    ): Result<OrderDocumentEntity> = runCatching {
        val detail = workflowRepository.observeOrderDetail(orderId).firstOrNull()
            ?: throw IllegalStateException("Commessa non trovata")
        
        val user = userDao.findById(userId) ?: throw IllegalStateException("Utente non trovato")
        
        val signature = if (includeCustomerSignature) {
            detail.signatures.maxByOrNull { it.signedAt }
        } else null
        
        if (includeCustomerSignature && (signature == null)) {
            throw IllegalStateException("Firma cliente non trovata")
        }

        val categoryPath = catalogRepository.getCategoryPath(detail.order.categoryId)
        val now = clockMillis()
        
        val payload = OrderStatusPdfPayload(
            order = detail.order,
            customer = detail.customer ?: throw IllegalStateException("Cliente non trovato"),
            categoryPath = categoryPath,
            statusLabel = detail.order.status.label(context),
            estimateAmountLabel = detail.order.estimateAmountCents?.formatEuroCents(),
            assignedOperatorName = detail.assignedUser?.displayName,
            timeline = detail.timeline,
            signature = signature,
            generatedAt = now,
            generatedBy = user.displayName,
        )
        
        val fileName = "stato-avanzamento-${detail.order.orderNumber}-$now.pdf"
        val file = fileStorage.getDocumentFile(orderId, fileName)
        
        pdfGenerator.generate(file, payload)
        
        val entity = OrderDocumentEntity(
            id = UUID.randomUUID().toString(),
            orderId = orderId,
            type = OrderDocumentType.STATUS_PROGRESS_PDF,
            localPath = file.absolutePath,
            fileName = fileName,
            mimeType = "application/pdf",
            sizeBytes = file.length(),
            includesCustomerSignature = signature != null,
            createdByUserId = userId,
            createdAt = now,
            sharedAt = null,
        )
        
        documentRepository.save(entity)
        // TODO: log event in timeline? The plan says Step 3 Timeline...
        // Actually the UseCase should probably just generate. 
        // Or it should also log the event.
        
        entity
    }
}
