package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AttachmentCounts(
    val images: Int,
    val documents: Int,
    val signatures: Int,
    val finalPhotos: Int = 0,
)

fun attachmentCounts(media: List<OrderMediaEntity>, signatures: List<SignatureEntity>): AttachmentCounts =
    AttachmentCounts(
        images = media.count { it.isImageAttachment() && !it.isFinalPhoto() },
        documents = media.count { !it.isImageAttachment() },
        signatures = signatures.size,
        finalPhotos = media.count { it.isFinalPhoto() },
    )

fun AttachmentCounts.label(): String =
    listOf(
        "Foto $images",
        "Documenti $documents",
        "Firme $signatures",
        "Lavoro finito $finalPhotos".takeIf { finalPhotos > 0 },
    ).filterNotNull().joinToString(" - ")

fun OrderAttachmentCounts.label(): String =
    listOf(
        "Foto $images",
        "Documenti $documents",
        "Firme $signatures",
        "Lavoro finito $finalPhotos".takeIf { finalPhotos > 0 },
    ).filterNotNull().joinToString(" - ")

fun OrderMediaEntity.isImageAttachment(): Boolean =
    mediaType.equals("PHOTO", ignoreCase = true) ||
        mediaType.equals("FINAL_PHOTO", ignoreCase = true) ||
        mediaType.equals("IMAGE", ignoreCase = true) ||
        mimeType.startsWith("image/", ignoreCase = true)

fun OrderMediaEntity.isFinalPhoto(): Boolean =
    mediaType.equals("FINAL_PHOTO", ignoreCase = true)

fun formatFileSize(bytes: Long): String =
    when {
        bytes < 0L -> "0 B"
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> String.format(Locale.ITALIAN, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

fun formatUiDateTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .format(Instant.ofEpochMilli(millis).atZone(zoneId))

fun nextActionLabel(status: OrderStatus): String =
    availableOrderStatusActions(status).firstOrNull()?.label ?: "Nessuna azione"
