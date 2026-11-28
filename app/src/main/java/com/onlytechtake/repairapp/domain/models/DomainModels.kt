package com.onlytechtake.repairapp.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class Customer(
    val id: String,
    val type: CustomerType,
    val firstName: String?,
    val lastName: String?,
    val companyName: String?,
    val vatNumber: String?,
    val taxCode: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val displayName: String
        get() =
            when (type) {
                CustomerType.PRIVATE -> listOfNotNull(firstName, lastName).joinToString(" ")
                CustomerType.BUSINESS -> companyName ?: ""
            }
}

@Immutable
data class Order(
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val title: String,
    val categoryId: String,
    val status: OrderStatus,
    val priority: OrderPriority,
    val assignedToUserId: String?,
    val createdByUserId: String,
    val description: String?,
    val internalNotes: String? = null,
    val diagnosisOutcome: DiagnosisOutcome? = null,
    val estimateAmountCents: Long? = null,
    val finalAmountCents: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long? = null,
    val closureReason: ClosureReason? = null,
    val customerActionStatus: CustomerActionStatus? = null,
    val closeNote: String? = null,
    val closedByUserId: String? = null,
    val categoryPathSnapshot: String? = null,
    val lastCustomerContactAt: Long? = null,
)

@Immutable
data class Category(
    val id: String,
    val parentId: String?,
    val level: Int,
    val name: String,
    val description: String?,
    val active: Boolean,
)

@Immutable
data class User(
    val id: String,
    val displayName: String,
    val username: String,
    val role: UserRole,
    val permissions: Set<UserPermission>,
    val active: Boolean,
)

@Immutable
data class TimelineEvent(
    val id: String,
    val orderId: String,
    val type: TimelineEventType,
    val title: String,
    val description: String?,
    val visibleToCustomer: Boolean,
    val createdByUserId: String,
    val createdAt: Long,
)

@Immutable
data class OrderMedia(
    val id: String,
    val orderId: String,
    val mediaType: String,
    val fileName: String,
    val localPath: String,
    val mimeType: String,
    val capturedAt: Long,
)

@Immutable
data class Signature(
    val id: String,
    val orderId: String,
    val signerName: String,
    val privacyAccepted: Boolean,
    val termsAccepted: Boolean,
    val privacyTemplateVersion: Int,
    val termsTemplateVersion: Int,
    val fileName: String,
    val localPath: String,
    val signedAt: Long,
)

@Immutable
data class OrderDocument(
    val id: String,
    val orderId: String,
    val type: OrderDocumentType,
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val includesCustomerSignature: Boolean,
    val createdByUserId: String,
    val createdAt: Long,
    val sharedAt: Long?,
)
