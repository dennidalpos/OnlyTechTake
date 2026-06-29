package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import androidx.compose.runtime.Immutable
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
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

data class OwnerInfo(
    val name: String = "",
    val vatNumber: String = "",
    val address: String = "",
    val email: String = "",
    val phone: String = "",
) {
    fun isComplete(): Boolean = name.isNotBlank() && vatNumber.isNotBlank() && address.isNotBlank()
}

data class OperatorInput(
    val displayName: String,
    val username: String,
    val password: CharArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OperatorInput
        if (displayName != other.displayName) return false
        if (username != other.username) return false
        if (!password.contentEquals(other.password)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = displayName.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + password.contentHashCode()
        return result
    }
}

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

fun IntakeOrderInput.validationErrors(context: Context): List<String> =
    buildList {
        val hasExistingCustomer = !existingCustomerId.isNullOrBlank()
        val hasValidNewCustomer = newCustomer?.isValidForIntake() == true
        if (!hasExistingCustomer && !hasValidNewCustomer) add(context.getString(R.string.error_customer_required))
        if (problemSummary.isBlank()) add(context.getString(R.string.error_problem_required))
        if (categoryId.isNullOrBlank()) add(context.getString(R.string.error_category_required))
        if (stagedMediaCount <= 0) add(context.getString(R.string.error_media_required))
        if (!hasPrivacySignature) add(context.getString(R.string.error_privacy_required))
    }

fun IntakeOrderInput.isReadyForSubmit(context: Context): Boolean = validationErrors(context).isEmpty()

private fun CustomerInput.isValidForIntake(): Boolean {
    val hasContact = phone.isNotBlank() || email.isNotBlank()
    val hasName =
        when (type) {
            CustomerType.PRIVATE -> firstName.isNotBlank() && lastName.isNotBlank()
            CustomerType.BUSINESS -> companyName.isNotBlank()
        }
    return hasContact && hasName
}

enum class OrderFilterMode {
    ALL,
    MINE,
    OPEN_UNASSIGNED,
    CLOSED,
    IN_PROGRESS,
}

data class OrderFilter(
    val mode: OrderFilterMode = OrderFilterMode.ALL,
    val showMineOnly: Boolean = false,
    val status: OrderStatus? = null,
    val statuses: Set<OrderStatus> = emptySet(),
    val customerId: String? = null,
    val query: String = "",
)

@Immutable
data class OrderAttachmentCounts(
    val images: Int = 0,
    val videos: Int = 0,
    val documents: Int = 0,
    val signatures: Int = 0,
    val finalPhotos: Int = 0,
)

@Immutable
data class OrderListItem(
    val order: RepairOrderEntity,
    val customer: CustomerEntity? = null,
    val attachments: OrderAttachmentCounts = OrderAttachmentCounts(),
)

@Immutable
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
        get() =
            timeline
                .filter { it.type == TimelineEventType.PAYMENT_RECEIVED || it.type == TimelineEventType.PAYMENT_REMOVED }
                .maxByOrNull { it.createdAt }
                ?.type == TimelineEventType.PAYMENT_RECEIVED
}

data class PreparedMessage(
    val orderId: String,
    val customerId: String,
    val phone: String,
    val body: String,
    val templateName: String,
)

data class FeedbackInput(
    val feedback: CustomerFeedbackType,
    val note: String = "",
    val closureReason: ClosureReason? = null,
)

fun FeedbackInput.validationErrors(context: Context): List<String> =
    buildList {
        if ((feedback == CustomerFeedbackType.REJECT) && note.isBlank()) {
            add(context.getString(R.string.error_reject_reason_required))
        }
    }

data class DiagnosisResultInput(
    val summary: String,
    val diagnosisOutcome: DiagnosisOutcome,
    val estimateAmountCents: Long? = null,
)

fun DiagnosisResultInput.validationErrors(context: Context): List<String> =
    buildList {
        if (summary.isBlank()) add(context.getString(R.string.error_result_summary_required))
        if ((diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) && ((estimateAmountCents == null) || (estimateAmountCents <= 0L))) {
            add(context.getString(R.string.error_positive_estimate_required))
        }
    }

fun DiagnosisResultInput.normalizedEstimateAmountCents(): Long? = estimateAmountCents.takeIf { diagnosisOutcome == DiagnosisOutcome.REPAIRABLE }
