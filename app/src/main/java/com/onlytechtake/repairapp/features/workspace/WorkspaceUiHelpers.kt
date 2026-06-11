package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
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

fun StagedOrderMedia.isImageAttachment(): Boolean =
    mediaType.equals("PHOTO", ignoreCase = true) ||
        mediaType.equals("IMAGE", ignoreCase = true) ||
        mimeType.startsWith("image/", ignoreCase = true)

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
    if (status == OrderStatus.RESULT) {
        "Registra esito preventivo"
    } else {
        availableOrderStatusActions(status).firstOrNull()?.label ?: "Nessuna azione"
    }

fun nextActionLabel(order: RepairOrderEntity): String =
    when {
        order.status == OrderStatus.RESULT && order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE ->
            "Attendere accettazione o rifiuto preventivo"
        order.status == OrderStatus.RESULT && order.diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE ->
            "Avvisare il cliente e preparare il ritiro"
        else -> nextActionLabel(order.status)
    }

fun shouldShowDiagnosisEstimate(outcome: DiagnosisOutcome?): Boolean =
    outcome == DiagnosisOutcome.REPAIRABLE

fun canConfirmDiagnosisResult(
    summary: String,
    outcome: DiagnosisOutcome?,
    estimateAmountCents: Long?,
): Boolean =
    summary.isNotBlank() &&
        outcome != null &&
        (outcome != DiagnosisOutcome.REPAIRABLE || (estimateAmountCents != null && estimateAmountCents > 0L))

fun DiagnosisOutcome.diagnosisOutcomeLabel(): String =
    when (this) {
        DiagnosisOutcome.REPAIRABLE -> "Riparabile"
        DiagnosisOutcome.NOT_REPAIRABLE -> "Non riparabile"
    }

fun categoryPath(categories: List<OrderCategoryEntity>, categoryId: String?): String? {
    if (categoryId == null) return null
    val byId = categories.associateBy { it.id }
    val names = mutableListOf<String>()
    val visited = mutableSetOf<String>()
    var current = byId[categoryId]
    while (current != null && visited.add(current.id)) {
        names += current.name
        current = current.parentId?.let(byId::get)
    }
    return names.asReversed().joinToString(" > ").ifBlank { null }
}

fun parseEuroCentsOrNull(value: String): Long? {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isBlank()) return null
    val parts = normalized.split('.', limit = 2)
    val euros = parts.getOrNull(0)?.filter(Char::isDigit)?.toLongOrNull() ?: return null
    val cents = parts.getOrNull(1)
        ?.filter(Char::isDigit)
        ?.take(2)
        ?.padEnd(2, '0')
        ?.toLongOrNull()
        ?: 0L
    return euros * 100L + cents
}

fun applyStatusAction(
    action: OrderStatusAction,
    orderId: String,
    viewModel: WorkspaceViewModel,
    note: String,
    closureReason: ClosureReason?,
    resultSummary: String,
    resultAmount: String,
    diagnosisOutcome: DiagnosisOutcome?,
) {
    if (action == OrderStatusAction.RECORD_RESULT) {
        val outcome = diagnosisOutcome ?: return
        val estimateAmountCents = parseEuroCentsOrNull(resultAmount)
        viewModel.recordResult(
            orderId = orderId,
            summary = resultSummary,
            diagnosisOutcome = outcome,
            estimateAmountCents = estimateAmountCents,
            onSuccess = {
                viewModel.prepareMessage(
                    orderId,
                    if (outcome == DiagnosisOutcome.REPAIRABLE) {
                        "whatsapp_preventivo_intervento"
                    } else {
                        "whatsapp_non_riparabile"
                    },
                )
            },
        )
        return
    }
    val targetStatus = action.targetStatus ?: return
    viewModel.changeStatus(
        orderId = orderId,
        status = targetStatus,
        note = note,
        closureReason = closureReason,
        onSuccess = {
            if (targetStatus == OrderStatus.CLOSED && closureReason == ClosureReason.NOT_REPAIRABLE) {
                viewModel.prepareMessage(orderId, "whatsapp_non_riparabile")
            } else if (targetStatus == OrderStatus.CLOSED) {
                viewModel.selectOrder(null)
            }
        },
    )
}
