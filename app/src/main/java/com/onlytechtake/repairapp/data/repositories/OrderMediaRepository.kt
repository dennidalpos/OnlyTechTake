package com.onlytechtake.repairapp.data.repositories

import android.content.Context
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
        runCatching {
            requireWritableOrder(actor, orderId)
            require(file.exists() && file.length() > 0L) { "Foto non salvata" }

            val now = clockMillis()
            val bounds = readImageBounds(file)
            val media = OrderMediaEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                mediaType = MEDIA_TYPE_PHOTO,
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
                eventWriter.mediaAdded(orderId, actor.id, "Foto allegata", file.name, now)
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

    private fun File.deleteIfEmpty() {
        if (exists() && isDirectory && listFiles().isNullOrEmpty()) {
            delete()
        }
    }

    private companion object {
        const val MEDIA_TYPE_PHOTO = "PHOTO"
        const val MEDIA_TYPE_IMAGE = "IMAGE"
        const val MEDIA_TYPE_DOCUMENT = "DOCUMENT"
        const val MIME_TYPE_JPEG = "image/jpeg"
    }
}
