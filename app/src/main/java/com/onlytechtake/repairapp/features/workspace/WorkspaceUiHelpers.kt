package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AttachmentCounts(
    val images: Int,
    val documents: Int,
    val signatures: Int,
)

fun attachmentCounts(media: List<OrderMediaEntity>, signatures: List<SignatureEntity>): AttachmentCounts =
    AttachmentCounts(
        images = media.count { it.isImageAttachment() },
        documents = media.count { !it.isImageAttachment() },
        signatures = signatures.size,
    )

fun AttachmentCounts.label(): String =
    "Foto ${images} - Documenti ${documents} - Firme ${signatures}"

fun OrderMediaEntity.isImageAttachment(): Boolean =
    mediaType.equals("PHOTO", ignoreCase = true) ||
        mediaType.equals("IMAGE", ignoreCase = true) ||
        mimeType.startsWith("image/", ignoreCase = true)

fun formatFileSize(bytes: Long): String =
    when {
        bytes < 0L -> "0 B"
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

fun formatUiDateTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .format(Instant.ofEpochMilli(millis).atZone(zoneId))

fun nextActionLabel(status: OrderStatus): String =
    availableOrderStatusActions(status).firstOrNull()?.label ?: "Nessuna azione"
