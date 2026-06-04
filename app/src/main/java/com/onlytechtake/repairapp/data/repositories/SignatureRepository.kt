package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.Bitmap
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.SignatureDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SignatureRepository(
    private val context: Context,
    private val repairOrderDao: RepairOrderDao,
    private val signatureDao: SignatureDao,
    private val templateDao: MessageTemplateDao,
    private val timelineEventDao: TimelineEventDao,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(timelineEventDao = timelineEventDao)

    suspend fun saveSignature(
        actor: AuthenticatedUser,
        orderId: String,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        bitmap: Bitmap,
    ): Result<SignatureEntity> =
        runCatching {
            val order = requireNotNull(repairOrderDao.findById(orderId)) { "Commessa non trovata" }
            require(order.deletedAt == null) { "Commessa archiviata" }
            require(actor.role == UserRole.ADMIN || order.assignedToUserId == actor.id) { "Operatore non assegnato" }
            require(signerName.isNotBlank()) { "Nome firmatario obbligatorio" }
            require(privacyAccepted) { "Consenso privacy obbligatorio" }
            require(termsAccepted) { "Accettazione condizioni obbligatoria" }
            require(bitmap.width > 0 && bitmap.height > 0) { "Firma non valida" }

            val now = clockMillis()
            val privacyTemplate = templateDao.findActiveByCode(PRIVACY_TEMPLATE_CODE)
            val termsTemplate = templateDao.findActiveByCode(TERMS_TEMPLATE_CODE)
            val directory = File(context.filesDir, "orders/$orderId/signatures")
            check(directory.exists() || directory.mkdirs()) { "Cartella firme non disponibile" }
            val file = File(directory, "signature_$now.png")

            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) { "Salvataggio firma non riuscito" }
            }

            val signature = SignatureEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                signerName = signerName.trim(),
                privacyAccepted = privacyAccepted,
                termsAccepted = termsAccepted,
                privacyTextSnapshot = privacyTemplate?.body ?: DEFAULT_PRIVACY_TEXT,
                privacyTemplateVersion = privacyTemplate?.version ?: 0,
                termsTextSnapshot = termsTemplate?.body ?: DEFAULT_TERMS_TEXT,
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

    private companion object {
        const val PRIVACY_TEMPLATE_CODE = "privacy_breve"
        const val TERMS_TEMPLATE_CODE = "condizioni_presa_in_carico"
        const val MIME_TYPE_PNG = "image/png"
        const val PNG_QUALITY = 100
        const val DEFAULT_PRIVACY_TEXT = "I dati sono trattati solo per gestione riparazione, comunicazioni e obblighi fiscali."
        const val DEFAULT_TERMS_TEXT = "Il cliente autorizza la presa in carico del bene e accetta le condizioni operative comunicate."
    }
}
