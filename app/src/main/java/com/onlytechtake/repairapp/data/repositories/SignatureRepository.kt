package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.Bitmap
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.templates.LegalTemplateFallbacks
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.SignatureDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SignatureRepository(
    private val context: Context,
    private val repairOrderDao: RepairOrderDao,
    private val signatureDao: SignatureDao,
    private val templateDao: MessageTemplateDao,
    private val settingDao: AppSettingDao,
    private val timelineEventDao: TimelineEventDao,
    private val fileCleanupService: OrderFileCleanupService,
    private val mediaStorageProvider: MediaStorageProvider,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(context, timelineEventDao = timelineEventDao)

    suspend fun saveSignature(
        actor: AuthenticatedUser,
        orderId: String,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        bitmap: Bitmap,
    ): Result<SignatureEntity> =
        runCatching {
            val order =
                requireNotNull(repairOrderDao.findById(orderId)) { context.getString(R.string.error_order_not_found) }
            if (order.assignedToUserId != actor.id) {
                PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
            } else {
                PermissionService.requirePermission(actor, UserPermission.EDIT_OWN_ORDERS)
            }
            require(signatureDao.countForOrder(orderId) == 0) { context.getString(R.string.error_signature_exists) }
            require(privacyAccepted) { context.getString(R.string.error_privacy_consent_required) }
            require(termsAccepted) { context.getString(R.string.error_terms_acceptance_required) }
            require(bitmap.width > 0 && bitmap.height > 0) { context.getString(R.string.error_invalid_signature) }

            val now = clockMillis()
            val privacyCode =
                settingDao.findByKey(TemplateAnchor.PRIVACY_TEXT.settingKey)?.value
                    ?: TemplateAnchor.PRIVACY_TEXT.defaultCode
            val termsCode =
                settingDao.findByKey(TemplateAnchor.TERMS_TEXT.settingKey)?.value
                    ?: TemplateAnchor.TERMS_TEXT.defaultCode

            val privacyTemplate = templateDao.findActiveByCode(privacyCode)
            val termsTemplate = templateDao.findActiveByCode(termsCode)
            val directory = mediaStorageProvider.getOrderSignaturesDir(orderId)
            val file = File(directory, "signature_$now.png")

            FileOutputStream(file).use { output ->
                check(
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        PNG_QUALITY,
                        output,
                    ),
                ) { context.getString(R.string.error_signature_save_failed) }
            }

            val signature =
                SignatureEntity(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    signerName =
                        signerName
                            .trim()
                            .ifBlank { context.getString(R.string.signature_default_signer) },
                    privacyAccepted = privacyAccepted,
                    termsAccepted = termsAccepted,
                    privacyTextSnapshot =
                        privacyTemplate?.body
                            ?: LegalTemplateFallbacks.PrivacyText,
                    privacyTemplateVersion = privacyTemplate?.version ?: 0,
                    termsTextSnapshot = termsTemplate?.body ?: LegalTemplateFallbacks.TermsText,
                    termsTemplateVersion = termsTemplate?.version ?: 0,
                    localPath = file.absolutePath,
                    fileName = file.name,
                    mimeType = MIME_TYPE_PNG,
                    sizeBytes = file.length(),
                    width = bitmap.width,
                    height = bitmap.height,
                    createdByUserId = actor.id,
                    signedAt = now,
                    createdAt = now,
                )

            signatureDao.upsert(signature)
            eventWriter.signatureCaptured(orderId, actor.id, signature.signerName, now)
            signature
        }

    fun deleteSignatureFile(signature: SignatureEntity) {
        fileCleanupService.deleteSpecificFile(signature.localPath)
    }

    private companion object {
        const val MIME_TYPE_PNG = "image/png"
        const val PNG_QUALITY = 100
        const val DEFAULT_SIGNER_NAME = "Cliente"
    }
}
