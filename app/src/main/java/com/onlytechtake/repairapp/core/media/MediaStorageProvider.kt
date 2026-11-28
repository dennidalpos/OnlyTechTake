package com.onlytechtake.repairapp.core.media

import android.content.Context
import java.io.File

/**
 * Fornisce i percorsi centralizzati per l'archiviazione di tutti i media dell'app.
 * Tutti i file sono memorizzati sotto 'filesDir/app_media/'.
 */
class MediaStorageProvider(
    context: Context,
) {
    private val baseDir = File(context.filesDir, "app_media")

    /**
     * Cartella per i media (foto e allegati) di una commessa specifica.
     */
    fun getOrderMediaDir(orderId: String): File = File(baseDir, "order_media/$orderId").apply { ensureDirectory() }

    /**
     * Cartella per gli allegati (non foto dirette) di una commessa.
     */
    fun getOrderAttachmentsDir(orderId: String): File = File(baseDir, "order_media/$orderId/attachments").apply { ensureDirectory() }

    /**
     * Cartella per le firme di una commessa.
     */
    fun getOrderSignaturesDir(orderId: String): File = File(baseDir, "signatures/$orderId").apply { ensureDirectory() }

    /**
     * Cartella per i documenti PDF generati per una commessa.
     */
    fun getOrderDocumentsDir(orderId: String): File = File(baseDir, "documents/$orderId").apply { ensureDirectory() }

    /**
     * Cartella temporanea per i media acquisiti durante il wizard di presa in carico.
     */
    fun getIntakeMediaDir(sessionId: String): File = File(baseDir, "intake_media/$sessionId").apply { ensureDirectory() }

    /**
     * Cartella per gli allegati durante il wizard di presa in carico.
     */
    fun getIntakeAttachmentsDir(sessionId: String): File = File(baseDir, "intake_media/$sessionId/attachments").apply { ensureDirectory() }

    /**
     * Restituisce la cartella base di tutti i media.
     */
    fun getBaseMediaDir(): File = baseDir

    private fun File.ensureDirectory() {
        if (!exists()) {
            mkdirs()
        }
    }
}
