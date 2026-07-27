package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfGenerator
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfPayload
import com.onlytechtake.repairapp.core.pdf.OrderStatusPdfWriter
import com.onlytechtake.repairapp.core.utils.formatEuroCents
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.OrderDocumentFileStorage
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.domain.models.OrderDocument
import com.onlytechtake.repairapp.domain.models.OrderDocumentType
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.storage.StoragePolicy
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class GenerateOrderStatusPdfUseCase(
    private val context: Context,
    private val workflowRepository: OrderRepository,
    private val catalogRepository: CatalogRepository,
    private val documentRepository: OrderDocumentRepository,
    private val userDao: UserDao,
    private val fileStorage: OrderDocumentFileStorage,
    private val ownerPreferences: OwnerPreferences,
    private val pdfGenerator: OrderStatusPdfWriter = OrderStatusPdfGenerator(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        orderId: String,
        userId: String,
        includeCustomerSignature: Boolean = true,
    ): Result<OrderDocument> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
                val detail =
                    workflowRepository.observeOrderDetail(orderId).firstOrNull()
                        ?: throw IllegalStateException("Commessa non trovata")

                val user =
                    userDao.findById(userId) ?: throw IllegalStateException("Utente non trovato")

                val signature =
                    if (includeCustomerSignature) {
                        detail.signatures.maxByOrNull { it.signedAt }
                    } else {
                        null
                    }

                if (includeCustomerSignature && (signature == null)) {
                    throw IllegalStateException("Firma cliente non trovata")
                }

                val categoryPath = catalogRepository.getCategoryPath(detail.order.categoryId)
                val now = clockMillis()
                val ownerInfo = ownerPreferences.ownerInfo.firstOrNull()

                val payload =
                    OrderStatusPdfPayload(
                        order = detail.order,
                        customer =
                            detail.customer
                                ?: throw IllegalStateException("Cliente non trovato"),
                        categoryPath = categoryPath,
                        statusLabel = detail.order.status.label(context),
                        estimateAmountLabel = detail.order.estimateAmountCents?.formatEuroCents(),
                        assignedOperatorName = detail.assignedUser?.displayName,
                        timeline = detail.timeline,
                        signature = signature,
                        generatedAt = now,
                        generatedBy = user.displayName,
                        ownerInfo = ownerInfo,
                        watermarkText =
                            if (BuildConfig.IS_DEMO_VERSION || BuildConfig.IS_SHOWCASE_MODE_ENABLED) {
                                DemoMode.getVersionBanner(context)
                            } else {
                                null
                            },
                    )

                val fileName = "stato-avanzamento-${detail.order.orderNumber}-$now.pdf"
                val file = fileStorage.getDocumentFile(orderId, fileName)

                pdfGenerator.generate(context, file, payload)

                val entity =
                    OrderDocumentEntity(
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
                entity.toDomain()
            }
        }
}
