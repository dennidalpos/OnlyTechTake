package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.util.AtomicFile
import androidx.exifinterface.media.ExifInterface
import androidx.room.withTransaction
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.AttachmentImportPolicy
import com.onlytechtake.repairapp.core.media.MediaOptimizer
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.media.OrderMediaFileNamer
import com.onlytechtake.repairapp.core.media.exifOrientationTransform
import com.onlytechtake.repairapp.core.media.toMiBLabel
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.data.local.dao.OrderMediaDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.storage.StoragePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val fileCleanupService: OrderFileCleanupService,
    private val mediaStorageProvider: MediaStorageProvider,
    private val database: RepairAppDatabase? = null,
    private val fileNamer: OrderMediaFileNamer = OrderMediaFileNamer(),
    private val attachmentPolicy: AttachmentImportPolicy = AttachmentImportPolicy(),
    private val mediaOptimizer: MediaOptimizer = MediaOptimizer(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val eventWriter = OrderWorkflowEventWriter(context, timelineEventDao = timelineEventDao)

    fun createPhotoFile(
        orderId: String,
        orderNumber: String,
    ): File {
        StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
        return File(
            mediaStorageProvider.getOrderMediaDir(orderId),
            fileNamer.photoFileName(context, orderNumber, clockMillis()),
        )
    }

    fun createIntakePhotoFile(sessionId: String): File {
        StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
        return File(
            mediaStorageProvider.getIntakeMediaDir(sessionId),
            fileNamer.photoFileName(context, "presa_in_carico", clockMillis()),
        )
    }

    suspend fun stageCapturedIntakePhoto(file: File): Result<StagedOrderMedia> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(file.exists() && (file.length() > 0L)) { context.getString(R.string.error_photo_not_saved) }
                mediaOptimizer.optimize(file)
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
        }

    suspend fun importIntakeAttachment(
        sessionId: String,
        uri: Uri,
    ): Result<StagedOrderMedia> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val sourceName = resolver.displayName(uri) ?: context.getString(R.string.label_attachment)
                val mimeType = attachmentPolicy.normalize(resolver.getType(uri))
                attachmentPolicy.requireAllowed(context, mimeType, resolver.openableSize(uri))
                val directory = mediaStorageProvider.getIntakeAttachmentsDir(sessionId)
                val now = clockMillis()
                val file = File(directory, uniqueAttachmentName(sourceName, mimeType, now))

                try {
                    resolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            copyToWithLimit(input, output, attachmentPolicy.maxSizeBytes)
                        }
                    } ?: throw IOException(context.getString(R.string.error_attachment_unavailable))
                } catch (error: Exception) {
                    file.delete()
                    throw error
                }
                require(file.exists() && file.length() > 0L) { context.getString(R.string.error_attachment_empty) }

                mediaOptimizer.optimize(file)
                val bounds =
                    if (attachmentPolicy.isImage(mimeType)) {
                        readImageBounds(file)
                    } else {
                        ImageBounds(
                            null,
                            null,
                        )
                    }
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
        }

    suspend fun registerCapturedPhoto(
        actor: AuthenticatedUser,
        orderId: String,
        file: File,
    ): Result<OrderMediaEntity> =
        registerCapturedImage(
            actor = actor,
            orderId = orderId,
            file = file,
            mediaType = MEDIA_TYPE_PHOTO,
            timelineTitle = context.getString(R.string.timeline_photo_attached),
        )

    suspend fun registerCapturedFinalPhoto(
        actor: AuthenticatedUser,
        orderId: String,
        file: File,
    ): Result<OrderMediaEntity> =
        registerCapturedImage(
            actor = actor,
            orderId = orderId,
            file = file,
            mediaType = MEDIA_TYPE_FINAL_PHOTO,
            timelineTitle = context.getString(R.string.timeline_photo_final),
        )

    private suspend fun registerCapturedImage(
        actor: AuthenticatedUser,
        orderId: String,
        file: File,
        mediaType: String,
        timelineTitle: String,
    ): Result<OrderMediaEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireWritableOrder(actor, orderId)
                require(file.exists() && file.length() > 0L) { context.getString(R.string.error_photo_not_saved) }

                mediaOptimizer.optimize(file)

                val now = clockMillis()
                val bounds = readImageBounds(file)
                val media =
                    OrderMediaEntity(
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
        }

    suspend fun importAttachment(
        actor: AuthenticatedUser,
        orderId: String,
        uri: Uri,
    ): Result<OrderMediaEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                StoragePolicy.requireSpaceForHeavyWrite(context.filesDir)
                requireWritableOrder(actor, orderId)
                val resolver = context.contentResolver
                val sourceName = resolver.displayName(uri) ?: context.getString(R.string.label_attachment)
                val mimeType = attachmentPolicy.normalize(resolver.getType(uri))
                attachmentPolicy.requireAllowed(context, mimeType, resolver.openableSize(uri))
                val directory = mediaStorageProvider.getOrderAttachmentsDir(orderId)
                val now = clockMillis()
                val file = File(directory, uniqueAttachmentName(sourceName, mimeType, now))

                try {
                    resolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            copyToWithLimit(input, output, attachmentPolicy.maxSizeBytes)
                        }
                    } ?: throw IOException(context.getString(R.string.error_attachment_unavailable))
                } catch (error: Exception) {
                    file.delete()
                    throw error
                }
                require(file.exists() && file.length() > 0L) { context.getString(R.string.error_attachment_empty) }

                mediaOptimizer.optimize(file)
                val bounds =
                    if (attachmentPolicy.isImage(mimeType)) {
                        readImageBounds(file)
                    } else {
                        ImageBounds(
                            null,
                            null,
                        )
                    }
                val media =
                    OrderMediaEntity(
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
                        eventWriter.mediaAdded(
                            orderId,
                            actor.id,
                            context.getString(R.string.timeline_attachment_added),
                            file.name,
                            now,
                        )
                    }
                } catch (error: Exception) {
                    file.delete()
                    throw error
                }
                media
            }
        }

    suspend fun deleteMedia(
        actor: AuthenticatedUser,
        mediaId: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val media = requireNotNull(orderMediaDao.findById(mediaId)) { context.getString(R.string.error_media_not_found) }
                requireWritableOrder(actor, media.orderId)
                val now = clockMillis()

                inTransaction {
                    val deleted = orderMediaDao.deleteById(media.id)
                    require(deleted > 0) { context.getString(R.string.error_media_not_removed) }
                    eventWriter.mediaRemoved(media.orderId, actor.id, media.fileName, now)
                }
                fileCleanupService.deleteSpecificFile(media.localPath)
            }
        }

    suspend fun replaceImage(
        actor: AuthenticatedUser,
        mediaId: String,
        bitmap: Bitmap,
    ): Result<OrderMediaEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val media = requireNotNull(orderMediaDao.findById(mediaId)) { context.getString(R.string.error_media_not_found) }
                requireWritableOrder(actor, media.orderId)
                require(
                    media.mimeType.startsWith(
                        "image/",
                        ignoreCase = true,
                    ),
                ) { context.getString(R.string.error_not_an_image) }

                val file = File(media.localPath)
                val savedMimeType = writeBitmapReplacing(file, bitmap, media.mimeType)
                val bounds = readImageBounds(file)
                val updated =
                    media.copy(
                        mimeType = savedMimeType,
                        sizeBytes = file.length(),
                        width = bounds.width,
                        height = bounds.height,
                    )

                inTransaction {
                    orderMediaDao.upsert(updated)
                    eventWriter.mediaAdded(
                        media.orderId,
                        actor.id,
                        context.getString(R.string.timeline_image_modified),
                        media.fileName,
                        clockMillis(),
                    )
                }
                updated
            }
        }

    suspend fun attachStagedMedia(
        actor: AuthenticatedUser,
        orderId: String,
        stagedMedia: List<StagedOrderMedia>,
    ): Result<List<OrderMediaEntity>> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireWritableOrder(actor, orderId)
                if (stagedMedia.isEmpty()) return@runCatching emptyList()

                val now = clockMillis()
                val movedFiles = mutableListOf<File>()
                val entities =
                    stagedMedia.mapIndexed { index, staged ->
                        val source = File(staged.localPath)
                        require(source.exists() && source.length() > 0L) { context.getString(R.string.error_intake_file_unavailable, staged.fileName) }
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
                            eventWriter.mediaAdded(
                                orderId,
                                actor.id,
                                context.getString(R.string.timeline_media_intake),
                                media.fileName,
                                media.createdAt,
                            )
                        }
                    }
                } catch (error: Exception) {
                    movedFiles.forEach { file -> fileCleanupService.deleteSpecificFile(file.absolutePath) }
                    throw error
                }
                entities
            }
        }

    suspend fun deleteMediaFiles(media: List<OrderMediaEntity>) {
        withContext(Dispatchers.IO) {
            media.forEach { entity -> fileCleanupService.deleteSpecificFile(entity.localPath) }
        }
    }

    suspend fun deleteStagedMedia(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                fileCleanupService.deleteSpecificFile(path)
            }
        }

    suspend fun replaceStagedImage(
        staged: StagedOrderMedia,
        bitmap: Bitmap,
    ): Result<StagedOrderMedia> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(
                    staged.mimeType.startsWith(
                        "image/",
                        ignoreCase = true,
                    ),
                ) { context.getString(R.string.error_not_an_image) }
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
        }

    private fun readImageBounds(file: File): ImageBounds {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val rotation =
            runCatching {
                val orientation =
                    ExifInterface(file.absolutePath).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                exifOrientationTransform(orientation).rotationDegrees
            }.getOrDefault(0f)
        val swapDimensions = rotation == 90f || rotation == -90f
        return ImageBounds(
            width = (if (swapDimensions) options.outHeight else options.outWidth).takeIf { it > 0 },
            height = (if (swapDimensions) options.outWidth else options.outHeight).takeIf { it > 0 },
        )
    }

    private data class ImageBounds(
        val width: Int?,
        val height: Int?,
    )

    private suspend fun requireWritableOrder(
        actor: AuthenticatedUser,
        orderId: String,
    ) {
        val order = requireNotNull(repairOrderDao.findById(orderId)) { context.getString(R.string.error_order_not_found) }
        if (order.assignedToUserId != actor.id) {
            PermissionService.requirePermission(actor, UserPermission.EDIT_ALL_ORDERS)
        } else {
            PermissionService.requirePermission(actor, UserPermission.EDIT_OWN_ORDERS)
        }
    }

    private suspend fun <T> inTransaction(block: suspend () -> T): T = database?.withTransaction { block() } ?: block()

    private fun copyToWithLimit(
        input: InputStream,
        output: OutputStream,
        maxSizeBytes: Long,
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copiedBytes = 0L

        while (true) {
            val readBytes = input.read(buffer)
            if (readBytes < 0) return copiedBytes

            copiedBytes += readBytes
            require(copiedBytes <= maxSizeBytes) {
                context.getString(R.string.error_attachment_too_large, maxSizeBytes.toMiBLabel(context))
            }
            output.write(buffer, 0, readBytes)
        }
    }

    private fun uniqueAttachmentName(
        sourceName: String,
        mimeType: String,
        now: Long,
    ): String {
        val sanitizedName =
            sourceName
                .substringAfterLast('/')
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .trim('_')
                .ifBlank { context.getString(R.string.label_attachment) }
        val extension =
            sanitizedName
                .substringAfterLast('.', missingDelimiterValue = "")
                .ifBlank { MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty() }
        val baseName = sanitizedName.substringBeforeLast('.', sanitizedName).ifBlank { context.getString(R.string.label_attachment) }
        return if (extension.isBlank()) {
            "${now}_$baseName"
        } else {
            "${now}_$baseName.$extension"
        }
    }

    private fun moveStagedFile(
        orderId: String,
        source: File,
        staged: StagedOrderMedia,
        now: Long,
    ): File {
        val directory =
            if (staged.mediaType == MEDIA_TYPE_PHOTO) {
                mediaStorageProvider.getOrderMediaDir(orderId)
            } else {
                mediaStorageProvider.getOrderAttachmentsDir(orderId)
            }
        val destination =
            File(directory, uniqueAttachmentName(staged.fileName, staged.mimeType, now))
        return try {
            source.inputStream().use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            check(destination.exists() && destination.length() > 0L) { context.getString(R.string.error_media_move_failed) }
            source.delete()
            destination
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    private fun writeBitmapReplacing(
        file: File,
        bitmap: Bitmap,
        sourceMimeType: String,
    ): String {
        require(bitmap.width > 0 && bitmap.height > 0) { context.getString(R.string.error_invalid_modified_image) }
        val (format, mimeType) = compressFormatFor(sourceMimeType)
        val atomicFile = AtomicFile(file)
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            require(
                bitmap.compress(
                    format,
                    IMAGE_SAVE_QUALITY,
                    stream,
                ),
            ) { context.getString(R.string.error_image_save_failed) }
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
            throw e
        }
        return mimeType
    }

    @Suppress("DEPRECATION")
    private fun compressFormatFor(mimeType: String): Pair<Bitmap.CompressFormat, String> =
        when {
            mimeType.equals(
                "image/png",
                ignoreCase = true,
            ) -> Bitmap.CompressFormat.PNG to "image/png"

            mimeType.equals(
                "image/webp",
                ignoreCase = true,
            ) -> Bitmap.CompressFormat.WEBP to "image/webp"

            else -> Bitmap.CompressFormat.JPEG to MIME_TYPE_JPEG
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
