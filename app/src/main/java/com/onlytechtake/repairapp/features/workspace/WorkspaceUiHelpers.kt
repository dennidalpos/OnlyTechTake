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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
        "Lavoro finale $finalPhotos".takeIf { finalPhotos > 0 },
    ).filterNotNull().joinToString(" - ")

fun OrderAttachmentCounts.label(): String =
    listOf(
        "Foto $images",
        "Documenti $documents",
        "Firme $signatures",
        "Lavoro finale $finalPhotos".takeIf { finalPhotos > 0 },
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

@Composable
fun OrderStatusStepper(
    status: OrderStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val steps = listOf(
        OrderStatus.OPEN,
        OrderStatus.DIAGNOSIS,
        OrderStatus.RESULT,
        OrderStatus.REPAIRING,
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.CLOSED
    )

    val currentStepIndex = when (status) {
        OrderStatus.OPEN -> 0
        OrderStatus.IN_PROGRESS, OrderStatus.DIAGNOSIS -> 1
        OrderStatus.RESULT -> 2
        OrderStatus.REPAIRING -> 3
        OrderStatus.READY_FOR_PICKUP -> 4
        OrderStatus.CLOSED -> 5
    }

    val dotSize = if (compact) 12.dp else 24.dp
    val spacing = if (compact) 4.dp else 12.dp

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = spacing),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val completedColor = Color(0xFF2E7D32) // Use a standard green
        val inactiveColor = MaterialTheme.colorScheme.outlineVariant

        steps.forEachIndexed { index, _ ->
            val isCompleted = index < currentStepIndex
            val isCurrent = index == currentStepIndex
            val color = when {
                isCurrent -> MaterialTheme.colorScheme.primary
                isCompleted -> completedColor
                else -> inactiveColor
            }

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(color = color, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!compact) {
                    if (isCompleted) {
                        Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text((index + 1).toString(), color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (index < steps.size - 1) {
                Canvas(modifier = Modifier.weight(1f).height(if (compact) 2.dp else 4.dp).padding(horizontal = 2.dp)) {
                    drawRect(color = if (index < currentStepIndex) completedColor else inactiveColor)
                }
            }
        }
    }
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
