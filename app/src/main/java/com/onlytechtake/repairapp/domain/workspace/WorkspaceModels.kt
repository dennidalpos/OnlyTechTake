package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import com.onlytechtake.repairapp.data.local.entities.UserEntity

data class OperatorInput(
    val displayName: String,
    val username: String,
    val password: String,
)

data class CustomerInput(
    val id: String? = null,
    val type: CustomerType = CustomerType.PRIVATE,
    val firstName: String = "",
    val lastName: String = "",
    val companyName: String = "",
    val vatNumber: String = "",
    val taxCode: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
)

data class CategoryInput(
    val id: String? = null,
    val parentId: String? = null,
    val name: String,
    val description: String,
    val active: Boolean = true,
)

data class RepairOrderInput(
    val customerId: String,
    val title: String,
    val categoryId: String,
    val description: String = "",
    val assignedToUserId: String? = null,
    val priority: OrderPriority = OrderPriority.NORMAL,
)

data class IntakeOrderInput(
    val existingCustomerId: String? = null,
    val newCustomer: CustomerInput? = null,
    val problemSummary: String = "",
    val problemDetails: String = "",
    val categoryId: String? = null,
    val assignedToUserId: String? = null,
    val stagedMediaCount: Int = 0,
    val hasPrivacySignature: Boolean = false,
)

sealed interface IntakeOrderCreationResult {
    val order: RepairOrderEntity

    data class Complete(
        override val order: RepairOrderEntity,
    ) : IntakeOrderCreationResult
}

fun IntakeOrderInput.validationErrors(): List<String> =
    buildList {
        val hasExistingCustomer = !existingCustomerId.isNullOrBlank()
        val hasValidNewCustomer = newCustomer?.isValidForIntake() == true
        if (!hasExistingCustomer && !hasValidNewCustomer) add("Cliente obbligatorio")
        if (problemSummary.isBlank()) add("Problema obbligatorio")
        if (categoryId.isNullOrBlank()) add("Categoria obbligatoria")
        if (stagedMediaCount <= 0) add("Almeno una foto o allegato obbligatorio")
        if (!hasPrivacySignature) add("Privacy e firma obbligatorie")
    }

fun IntakeOrderInput.isReadyForSubmit(): Boolean = validationErrors().isEmpty()

private fun CustomerInput.isValidForIntake(): Boolean {
    val hasContact = phone.isNotBlank() || email.isNotBlank()
    val hasName = when (type) {
        CustomerType.PRIVATE -> firstName.isNotBlank() && lastName.isNotBlank()
        CustomerType.BUSINESS -> companyName.isNotBlank()
    }
    return hasContact && hasName
}

data class OrderFilter(
    val showMineOnly: Boolean = false,
    val status: OrderStatus? = null,
    val statuses: Set<OrderStatus> = emptySet(),
    val customerId: String? = null,
)

data class OrderAttachmentCounts(
    val images: Int = 0,
    val documents: Int = 0,
    val signatures: Int = 0,
    val finalPhotos: Int = 0,
)

data class OrderListItem(
    val order: RepairOrderEntity,
    val customer: CustomerEntity? = null,
    val attachments: OrderAttachmentCounts = OrderAttachmentCounts(),
)

data class OrderDetail(
    val order: RepairOrderEntity,
    val customer: CustomerEntity?,
    val category: OrderCategoryEntity?,
    val assignedUser: UserEntity?,
    val timeline: List<TimelineEventEntity>,
    val messages: List<MessageLogEntity>,
    val media: List<OrderMediaEntity>,
    val signatures: List<SignatureEntity>,
) {
    val isPaid: Boolean
        get() = timeline.any { it.type == TimelineEventType.PAYMENT_RECEIVED }
}

data class PreparedMessage(
    val orderId: String,
    val customerId: String,
    val phone: String,
    val body: String,
)

data class FeedbackInput(
    val feedback: CustomerFeedbackType,
    val note: String = "",
    val closureReason: ClosureReason? = null,
)

fun FeedbackInput.validationErrors(): List<String> =
    buildList {
        if ((feedback == CustomerFeedbackType.REJECT) && note.isBlank()) {
            add("Motivo rifiuto obbligatorio")
        }
    }

data class DiagnosisResultInput(
    val summary: String,
    val diagnosisOutcome: DiagnosisOutcome,
    val estimateAmountCents: Long? = null,
)

fun DiagnosisResultInput.validationErrors(): List<String> =
    buildList {
        if (summary.isBlank()) add("Sintesi risultato obbligatoria")
        if ((diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) && ((estimateAmountCents == null) || (estimateAmountCents <= 0L))) {
            add("Importo preventivo positivo obbligatorio")
        }
    }

fun DiagnosisResultInput.normalizedEstimateAmountCents(): Long? =
    estimateAmountCents.takeIf { diagnosisOutcome == DiagnosisOutcome.REPAIRABLE }
