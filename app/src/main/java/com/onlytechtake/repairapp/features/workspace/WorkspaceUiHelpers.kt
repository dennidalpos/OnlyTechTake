package com.onlytechtake.repairapp.features.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.core.utils.label
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppSizes
import com.onlytechtake.repairapp.core.design.repairColors
import com.onlytechtake.repairapp.core.utils.parseEuroCentsOrNull
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction

data class AttachmentCounts(
    val images: Int,
    val documents: Int,
    val signatures: Int,
    val finalPhotos: Int = 0,
)

fun attachmentCounts(
    media: List<OrderMediaEntity>,
    signatures: List<SignatureEntity>,
): AttachmentCounts =
    AttachmentCounts(
        images = media.count { it.isImageAttachment() && !it.isFinalPhoto() },
        documents = media.count { !it.isImageAttachment() },
        signatures = signatures.size,
        finalPhotos = media.count { it.isFinalPhoto() },
    )

fun AttachmentCounts.label(): String =
    buildList {
        add("Foto $images")
        add("Documenti $documents")
        add("Firme $signatures")
        if (finalPhotos > 0) add("Lavoro finito $finalPhotos")
    }.joinToString(" - ")

fun OrderAttachmentCounts.label(): String =
    buildList {
        add("Foto $images")
        add("Documenti $documents")
        add("Firme $signatures")
        if (finalPhotos > 0) add("Lavoro finito $finalPhotos")
    }.joinToString(" - ")

fun OrderMediaEntity.isImageAttachment(): Boolean =
    mediaType.equals("PHOTO", ignoreCase = true) ||
        mediaType.equals("FINAL_PHOTO", ignoreCase = true) ||
        mediaType.equals("IMAGE", ignoreCase = true) ||
        mimeType.startsWith("image/", ignoreCase = true)

fun OrderMediaEntity.isFinalPhoto(): Boolean = mediaType.equals("FINAL_PHOTO", ignoreCase = true)

fun StagedOrderMedia.isImageAttachment(): Boolean =
    mediaType.equals("PHOTO", ignoreCase = true) ||
        mediaType.equals("IMAGE", ignoreCase = true) ||
        mimeType.startsWith("image/", ignoreCase = true)

fun categoryPath(
    categories: List<OrderCategoryEntity>,
    categoryId: String?,
): String? {
    if (categoryId == null) return null
    val byId = categories.associateBy { it.id }
    val names = mutableListOf<String>()
    val visited = mutableSetOf<String>()
    var current = byId[categoryId]
    while (current != null && (visited.add(current.id))) {
        names += current.name
        current = current.parentId?.let(byId::get)
    }
    return names.asReversed().joinToString(" > ").ifBlank { null }
}

fun orderStatusStepNumber(status: OrderStatus): Int =
    when (status) {
        OrderStatus.OPEN -> 1
        OrderStatus.IN_PROGRESS, OrderStatus.DIAGNOSIS -> 2
        OrderStatus.RESULT -> 3
        OrderStatus.REPAIRING -> 4
        OrderStatus.READY_FOR_PICKUP -> 5
        OrderStatus.CLOSED -> 6
    }

@Composable
fun OrderStatusStepper(
    status: OrderStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    stepTestTag: ((Int) -> String)? = null,
) {
    val context = LocalContext.current
    val steps =
        listOf(
            OrderStatus.OPEN,
            OrderStatus.DIAGNOSIS,
            OrderStatus.RESULT,
            OrderStatus.REPAIRING,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.CLOSED,
        )

    val currentStepIndex = orderStatusStepNumber(status) - 1

    val dotSize = if (compact) RepairAppSizes.orderStepperDot else 24.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 4.dp else 12.dp),
    ) {
        val completedColor = MaterialTheme.repairColors.stepCompleted
        val inactiveColor = MaterialTheme.repairColors.stepInactive

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            steps.forEachIndexed { index, step ->
                val isCurrent = index == currentStepIndex
                val color =
                    when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        index < currentStepIndex -> completedColor
                        else -> inactiveColor
                    }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    if (!compact) {
                        Text(
                            text = if (isCurrent) step.label(context) else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp),
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier =
                        Modifier
                            .size(dotSize)
                            .background(color = color, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            modifier = stepTestTag?.let { Modifier.testTag(it(index + 1)) } ?: Modifier,
                            color =
                            when {
                                isCurrent -> MaterialTheme.colorScheme.onPrimary
                                index < currentStepIndex -> MaterialTheme.repairColors.onSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                if (index < steps.size - 1) {
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 2.dp else 4.dp)
                            .padding(horizontal = 2.dp)
                            .padding(bottom = dotSize / 2 - (if (compact) 1.dp else 2.dp))
                    ) {
                        drawRect(color = if (index < currentStepIndex) completedColor else inactiveColor)
                    }
                }
            }
        }
    }
}
