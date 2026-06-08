package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.room.withTransaction
import com.onlytechtake.repairapp.core.media.AttachmentImportPolicy
import com.onlytechtake.repairapp.core.media.OrderMediaFileNamer
import com.onlytechtake.repairapp.core.media.toMiBLabel
import com.onlytechtake.repairapp.data.local.dao.OrderMediaDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class OrderMediaRepository(
    private val context: Context,
    private val repairOrderDao: RepairOrderDao,
    private val orderMediaDao: OrderMediaDao,
    private val timelineEventDao: TimelineEventDao,
    private val database: RepairAppDatabase? = null,
    private val fileNamer: OrderMediaFileNamer = OrderMediaFileNamer(),
    private val attachmentPolicy: AttachmentImportPolicy = AttachmentImportPolicy(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(timelineEventDao = timelineEventDao)

    fun createPhotoFile(orderId: String, orderNumber: String): File {
        val orderDirectory = File(context.filesDir, "order_media/$orderId")
        check(orderDirectory.exists() || orderDirectory.mkdirs()) { "Cartella media non disponibile" }
        return File(orderDirectory, fileNamer.photoFileName(orderNumber, clockMillis()))
    }

    fun createIntakePhotoFile(sessionId: String): File {
        val intakeDirectory = File(context.filesDir, "intake_media/$sessionId")
        check(intakeDirectory.exists() || intakeDirectory.mkdirs()) { "Cartella media presa in carico non disponibile" }
        return File(intakeDirectory, fileNamer.photoFileName("presa_in_carico", clockMillis()))
    }

    fun stageCapturedIntakePhoto(file: File): Result<StagedOrderMedia> =
        runCatching {
            require(file.exists() && file.length() > 0L) { "Foto non salvata" }
            val bounds = readImageBounds(file)
            StagedOrderMedia(
                localPath = file.absolutePath,
                fileName = file.name,
                mimeType = MIME_TYPE_JPEG,
                mediaType = MEDIA_TYPE_PHOTO,
                sizeBytes = file.length(),
                width = bounds.width,
                height = bounds.height,
            )
        }

    suspend fun importIntakeAttachment(sessionId: String, uri: Uri): Result<StagedOrderMedia> =
        runCatching {
            val resolver = context.contentResolver
            val sourceName = resolver.displayName(uri) ?: "allegato"
            val mimeType = attachmentPolicy.normalize(resolver.getType(uri))
            attachmentPolicy.requireAllowed(mimeType, resolver.openableSize(uri))
            val directory = File(context.filesDir, "intake_media/$sessionId/attachments")
            check(directory.exists() || directory.mkdirs()) { "Cartella allegati presa in carico non disponibile" }
            val now = clockMillis()
            val file = File(directory, uniqueAttachmentName(sourceName, mimeType, now))

            try {
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        copyToWithLimit(input, output, attachmentPolicy.maxSizeBytes)
                    }
                } ?: throw IOException("Allegato non disponibile")
            } catch (error: Exception) {
                file.delete()
                throw error
            }
            require(file.exists() && file.length() > 0L) { "Allegato vuoto o non salvato" }

            val bounds = if (attachmentPolicy.isImage(mimeType)) readImageBounds(file) else ImageBounds(null, null)
            StagedOrderMedia(
                localPath = file.absolutePath,
                fileName = file.name,
                mimeType = mimeType,
                mediaType = if (attachmentPolicy.isImage(mimeType)) MEDIA_TYPE_IMAGE else MEDIA_TYPE_DOCUMENT,
                sizeBytes = file.length(),
                width = bounds.width,
                height = bounds.height,
            )
        }

    suspend fun registerCapturedPhoto(actor: AuthenticatedUser, orderId: String, file: File): Result<OrderMediaEntity> =
        registerCapturedImage(
            actor = actor,
            orderId = orderId,
            file = file,
            mediaType = MEDIA_TYPE_PHOTO,
            timelineTitle = "Foto allegata",
        )

    suspend fun registerCapturedFinalPhoto(actor: AuthenticatedUser, orderId: String, file: File): Result<OrderMediaEntity> =
        registerCapturedImage(
            actor = actor,
            orderId = orderId,
            file = file,
            mediaType = MEDIA_TYPE_FINAL_PHOTO,
            timelineTitle = "Foto lavoro finito",
        )

    private suspend fun registerCapturedImage(
        actor: AuthenticatedUser,
        orderId: String,
        file: File,
        mediaType: String,
        timelineTitle: String,
    ): Result<OrderMediaEntity> =
        runCatching {
            requireWritableOrder(actor, orderId)
            require(file.exists() && file.length() > 0L) { "Foto non salvata" }

            val now = clockMillis()
            val bounds = readImageBounds(file)
            val media = OrderMediaEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                mediaType = mediaType,
                localPath = file.absolutePath,
                fileName = file.name,
                mimeType = MIME_TYPE_JPEG,
                sizeBytes = file.length(),
                width = bounds.width,
                height = bounds.height,
                createdByUserId = actor.id,
                capturedAt = now,
                createdAt = now,
            )

            inTransaction {
                orderMediaDao.upsert(media)
                eventWriter.mediaAdded(orderId, actor.id, timelineTitle, file.name, now)
            }
            media
        }

    suspend fun importAttachment(actor: AuthenticatedUser, orderId: String, uri: Uri): Result<OrderMediaEntity> =
        runCatching {
            requireWritableOrder(actor, orderId)
            val resolver = context.contentResolver
            val sourceName = resolver.displayName(uri) ?: "allegato"
            val mimeType = attachmentPolicy.normalize(resolver.getType(uri))
            attachmentPolicy.requireAllowed(mimeType, resolver.openableSize(uri))
            val directory = File(context.filesDir, "order_media/$orderId/attachments")
            check(directory.exists() || directory.mkdirs()) { "Cartella allegati non disponibile" }
            val now = clockMillis()
            val file = File(directory, uniqueAttachmentName(sourceName, mimeType, now))

            try {
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        copyToWithLimit(input, output, attachmentPolicy.maxSizeBytes)
                    }
                } ?: throw IOException("Allegato non disponibile")
            } catch (error: Exception) {
                file.delete()
                throw error
            }
            require(file.exists() && file.length() > 0L) { "Allegato vuoto o non salvato" }

            val bounds = if (attachmentPolicy.isImage(mimeType)) readImageBounds(file) else ImageBounds(null, null)
            val media = OrderMediaEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                mediaType = if (attachmentPolicy.isImage(mimeType)) MEDIA_TYPE_IMAGE else MEDIA_TYPE_DOCUMENT,
                localPath = file.absolutePath,
                fileName = file.name,
                mimeType = mimeType,
                sizeBytes = file.length(),
                width = bounds.width,
                height = bounds.height,
                createdByUserId = actor.id,
                capturedAt = now,
                createdAt = now,
            )

            try {
                inTransaction {
                    orderMediaDao.upsert(media)
                    eventWriter.mediaAdded(orderId, actor.id, "Allegato aggiunto", file.name, now)
                }
            } catch (error: Exception) {
                file.delete()
                throw error
            }
            media
        }

    suspend fun deleteMedia(actor: AuthenticatedUser, mediaId: String): Result<Unit> =
        runCatching {
            val media = requireNotNull(orderMediaDao.findById(mediaId)) { "Media non trovato" }
            requireWritableOrder(actor, media.orderId)
            val now = clockMillis()

            inTransaction {
                val deleted = orderMediaDao.deleteById(media.id)
                require(deleted > 0) { "Media non rimosso" }
                eventWriter.mediaRemoved(media.orderId, actor.id, media.fileName, now)
            }
            deleteMediaFile(File(media.localPath))
        }

    suspend fun replaceImage(actor: AuthenticatedUser, mediaId: String, bitmap: Bitmap): Result<OrderMediaEntity> =
        runCatching {
            val media = requireNotNull(orderMediaDao.findById(mediaId)) { "Media non trovato" }
            requireWritableOrder(actor, media.orderId)
            require(media.mimeType.startsWith("image/", ignoreCase = true)) { "Il media selezionato non e' un'immagine" }

            val file = File(media.localPath)
            val savedMimeType = writeBitmapReplacing(file, bitmap, media.mimeType)
            val bounds = readImageBounds(file)
            val updated = media.copy(
                mimeType = savedMimeType,
                sizeBytes = file.length(),
                width = bounds.width,
                height = bounds.height,
            )

            inTransaction {
                orderMediaDao.upsert(updated)
                eventWriter.mediaAdded(media.orderId, actor.id, "Immagine modificata", media.fileName, clockMillis())
            }
            updated
        }

    suspend fun attachStagedMedia(
        actor: AuthenticatedUser,
        orderId: String,
        stagedMedia: List<StagedOrderMedia>,
    ): Result<List<OrderMediaEntity>> =
        runCatching {
            requireWritableOrder(actor, orderId)
            if (stagedMedia.isEmpty()) return@runCatching emptyList()

            val now = clockMillis()
            val movedFiles = mutableListOf<File>()
            val entities = stagedMedia.mapIndexed { index, staged ->
                val source = File(staged.localPath)
                require(source.exists() && source.length() > 0L) { "File presa in carico non disponibile: ${staged.fileName}" }
                val destination = moveStagedFile(orderId, source, staged, now + index)
                movedFiles += destination
                OrderMediaEntity(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    mediaType = staged.mediaType,
                    localPath = destination.absolutePath,
                    fileName = destination.name,
                    mimeType = staged.mimeType,
                    sizeBytes = destination.length(),
                    width = staged.width,
                    height = staged.height,
                    createdByUserId = actor.id,
                    capturedAt = now + index,
                    createdAt = now + index,
                )
            }

            try {
                inTransaction {
                    entities.forEach { media ->
                        orderMediaDao.upsert(media)
                        eventWriter.mediaAdded(orderId, actor.id, "Media presa in carico", media.fileName, media.createdAt)
                    }
                }
            } catch (error: Exception) {
                movedFiles.forEach { file -> deleteMediaFile(file) }
                throw error
            }
            entities
        }

    fun deleteMediaFiles(media: List<OrderMediaEntity>) {
        media.forEach { entity -> deleteMediaFile(File(entity.localPath)) }
    }

    fun deleteStagedMedia(path: String): Result<Unit> =
        runCatching {
            deleteMediaFile(File(path))
        }

    fun replaceStagedImage(staged: StagedOrderMedia, bitmap: Bitmap): Result<StagedOrderMedia> =
        runCatching {
            require(staged.mimeType.startsWith("image/", ignoreCase = true)) { "Il media selezionato non e' un'immagine" }
            val file = File(staged.localPath)
            val savedMimeType = writeBitmapReplacing(file, bitmap, staged.mimeType)
            val bounds = readImageBounds(file)
            staged.copy(
                mimeType = savedMimeType,
                sizeBytes = file.length(),
                width = bounds.width,
                height = bounds.height,
            )
        }

    private fun readImageBounds(file: File): ImageBounds {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return ImageBounds(
            width = options.outWidth.takeIf { it > 0 },
            height = options.outHeight.takeIf { it > 0 },
        )
    }

    private data class ImageBounds(
        val width: Int?,
        val height: Int?,
    )

    private suspend fun requireWritableOrder(actor: AuthenticatedUser, orderId: String) {
        val order = requireNotNull(repairOrderDao.findById(orderId)) { "Commessa non trovata" }
        require(order.deletedAt == null) { "Commessa archiviata" }
        require(actor.role == UserRole.ADMIN || order.assignedToUserId == actor.id) { "Operatore non assegnato" }
    }

    private suspend fun <T> inTransaction(block: suspend () -> T): T =
        database?.withTransaction { block() } ?: block()

    private fun copyToWithLimit(input: InputStream, output: OutputStream, maxSizeBytes: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copiedBytes = 0L

        while (true) {
            val readBytes = input.read(buffer)
            if (readBytes < 0) return copiedBytes

            copiedBytes += readBytes
            require(copiedBytes <= maxSizeBytes) {
                "Allegato troppo grande: massimo ${maxSizeBytes.toMiBLabel()}"
            }
            output.write(buffer, 0, readBytes)
        }
    }

    private fun uniqueAttachmentName(sourceName: String, mimeType: String, now: Long): String {
        val sanitizedName = sourceName
            .substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .ifBlank { "allegato" }
        val extension = sanitizedName.substringAfterLast('.', missingDelimiterValue = "")
            .ifBlank { MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty() }
        val baseName = sanitizedName.substringBeforeLast('.', sanitizedName).ifBlank { "allegato" }
        return if (extension.isBlank()) {
            "${now}_$baseName"
        } else {
            "${now}_$baseName.$extension"
        }
    }

    private fun moveStagedFile(orderId: String, source: File, staged: StagedOrderMedia, now: Long): File {
        val directory = if (staged.mediaType == MEDIA_TYPE_PHOTO) {
            File(context.filesDir, "order_media/$orderId")
        } else {
            File(context.filesDir, "order_media/$orderId/attachments")
        }
        check(directory.exists() || directory.mkdirs()) { "Cartella media commessa non disponibile" }
        val destination = File(directory, uniqueAttachmentName(staged.fileName, staged.mimeType, now))
        try {
            source.inputStream().use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            check(destination.exists() && destination.length() > 0L) { "Spostamento media non riuscito" }
            source.delete()
            return destination
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    private fun deleteMediaFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
        file.parentFile?.deleteIfEmpty()
    }

    private fun writeBitmapReplacing(file: File, bitmap: Bitmap, sourceMimeType: String): String {
        require(bitmap.width > 0 && bitmap.height > 0) { "Immagine modificata non valida" }
        val parent = requireNotNull(file.parentFile) { "Cartella media non disponibile" }
        check(parent.exists() || parent.mkdirs()) { "Cartella media non disponibile" }
        val (format, mimeType) = compressFormatFor(sourceMimeType)
        val temp = File(parent, "${file.name}.tmp")
        try {
            FileOutputStream(temp).use { output ->
                require(bitmap.compress(format, IMAGE_SAVE_QUALITY, output)) { "Salvataggio immagine non riuscito" }
            }
            require(temp.exists() && temp.length() > 0L) { "Immagine modificata vuota" }
            temp.copyTo(file, overwrite = true)
            require(file.exists() && file.length() > 0L) { "Sostituzione immagine non riuscita" }
            return mimeType
        } catch (error: Exception) {
            throw error
        } finally {
            temp.delete()
        }
    }

    @Suppress("DEPRECATION")
    private fun compressFormatFor(mimeType: String): Pair<Bitmap.CompressFormat, String> =
        when {
            mimeType.equals("image/png", ignoreCase = true) -> Bitmap.CompressFormat.PNG to "image/png"
            mimeType.equals("image/webp", ignoreCase = true) -> Bitmap.CompressFormat.WEBP to "image/webp"
            else -> Bitmap.CompressFormat.JPEG to MIME_TYPE_JPEG
        }

    private fun File.deleteIfEmpty() {
        if (exists() && isDirectory && listFiles().isNullOrEmpty()) {
            delete()
        }
    }

    private companion object {
        const val MEDIA_TYPE_PHOTO = "PHOTO"
        const val MEDIA_TYPE_FINAL_PHOTO = "FINAL_PHOTO"
        const val MEDIA_TYPE_IMAGE = "IMAGE"
        const val MEDIA_TYPE_DOCUMENT = "DOCUMENT"
        const val MIME_TYPE_JPEG = "image/jpeg"
        const val IMAGE_SAVE_QUALITY = 92
    }
}
