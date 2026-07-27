package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import androidx.compose.runtime.Immutable
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.domain.models.Category
import com.onlytechtake.repairapp.domain.models.ClosureReason
import com.onlytechtake.repairapp.domain.models.Customer
import com.onlytechtake.repairapp.domain.models.CustomerFeedbackType
import com.onlytechtake.repairapp.domain.models.CustomerType
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.Order
import com.onlytechtake.repairapp.domain.models.OrderMedia
import com.onlytechtake.repairapp.domain.models.OrderPriority
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.models.Signature
import com.onlytechtake.repairapp.domain.models.TimelineEvent
import com.onlytechtake.repairapp.domain.models.TimelineEventType
import com.onlytechtake.repairapp.domain.models.User

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
    val order: Order

    data class Complete(
        override val order: Order,
    ) : IntakeOrderCreationResult
}

enum class IntakeValidationError {
    CUSTOMER_REQUIRED,
    PROBLEM_REQUIRED,
    CATEGORY_REQUIRED,
    MEDIA_REQUIRED,
    PRIVACY_REQUIRED,
}

fun IntakeOrderInput.validationErrors(): List<IntakeValidationError> =
    buildList {
        val hasExistingCustomer = !existingCustomerId.isNullOrBlank()
        val hasValidNewCustomer = newCustomer?.isValidForIntake() == true
        if (!hasExistingCustomer && !hasValidNewCustomer) add(IntakeValidationError.CUSTOMER_REQUIRED)
        if (problemSummary.isBlank()) add(IntakeValidationError.PROBLEM_REQUIRED)
        if (categoryId.isNullOrBlank()) add(IntakeValidationError.CATEGORY_REQUIRED)
        if (stagedMediaCount <= 0) add(IntakeValidationError.MEDIA_REQUIRED)
        if (!hasPrivacySignature) add(IntakeValidationError.PRIVACY_REQUIRED)
    }

fun IntakeOrderInput.validationErrors(context: Context): List<String> =
    validationErrors().map { error ->
        when (error) {
            IntakeValidationError.CUSTOMER_REQUIRED -> context.getString(R.string.error_customer_required)
            IntakeValidationError.PROBLEM_REQUIRED -> context.getString(R.string.error_problem_required)
            IntakeValidationError.CATEGORY_REQUIRED -> context.getString(R.string.error_category_required)
            IntakeValidationError.MEDIA_REQUIRED -> context.getString(R.string.error_media_required)
            IntakeValidationError.PRIVACY_REQUIRED -> context.getString(R.string.error_privacy_required)
        }
    }

fun IntakeOrderInput.isReadyForSubmit(context: Context): Boolean = validationErrors().isEmpty()

private fun CustomerInput.isValidForIntake(): Boolean {
    val hasContact = phone.isNotBlank() || email.isNotBlank()
    val hasName =
        when (type) {
            CustomerType.PRIVATE -> firstName.isNotBlank() && lastName.isNotBlank()
            CustomerType.BUSINESS -> companyName.isNotBlank()
        }
    return hasContact && hasName
}

data class OrderFilter(
    val query: String = "",
    val status: OrderStatus? = null,
    val statuses: Set<OrderStatus> = emptySet(),
    val customerId: String? = null,
) {
    fun hasActiveFilter(): Boolean = query.isNotBlank() || customerId != null || status != null || statuses.isNotEmpty()
}

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
    val order: Order,
    val customer: Customer? = null,
    val attachments: OrderAttachmentCounts = OrderAttachmentCounts(),
)

@Immutable
data class OrderDetail(
    val order: Order,
    val customer: Customer?,
    val category: Category?,
    val assignedUser: User?,
    val timeline: List<TimelineEvent>,
    val media: List<OrderMedia>,
    val signatures: List<Signature>,
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

enum class FeedbackValidationError {
    REJECT_REASON_REQUIRED,
}

fun FeedbackInput.validationErrors(): List<FeedbackValidationError> =
    buildList {
        if ((feedback == CustomerFeedbackType.REJECT) && note.isBlank()) {
            add(FeedbackValidationError.REJECT_REASON_REQUIRED)
        }
    }

data class DiagnosisResultInput(
    val summary: String,
    val diagnosisOutcome: DiagnosisOutcome,
    val estimateAmountCents: Long? = null,
)

enum class DiagnosisValidationError {
    SUMMARY_REQUIRED,
    POSITIVE_ESTIMATE_REQUIRED,
}

fun DiagnosisResultInput.validationErrors(): List<DiagnosisValidationError> =
    buildList {
        if (summary.isBlank()) add(DiagnosisValidationError.SUMMARY_REQUIRED)
        if ((diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) && ((estimateAmountCents == null) || (estimateAmountCents <= 0L))) {
            add(DiagnosisValidationError.POSITIVE_ESTIMATE_REQUIRED)
        }
    }

fun DiagnosisResultInput.normalizedEstimateAmountCents(): Long? = estimateAmountCents.takeIf { diagnosisOutcome == DiagnosisOutcome.REPAIRABLE }
