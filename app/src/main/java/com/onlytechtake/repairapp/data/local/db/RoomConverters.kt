package com.onlytechtake.repairapp.data.local.db

import androidx.room.TypeConverter
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerActionStatus
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.MessageStatus
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentType
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole

class RoomConverters {
    @TypeConverter
    fun toCustomerType(value: String?): CustomerType? = enumValueOrNull<CustomerType>(value)

    @TypeConverter
    fun fromCustomerType(value: CustomerType?): String? = value?.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole? = enumValueOrNull<UserRole>(value)

    @TypeConverter
    fun fromUserRole(value: UserRole?): String? = value?.name

    @TypeConverter
    fun toOrderStatus(value: String?): OrderStatus? = enumValueOrNull<OrderStatus>(value)

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus?): String? = value?.name

    @TypeConverter
    fun toDiagnosisOutcome(value: String?): DiagnosisOutcome? =
        enumValueOrNull<DiagnosisOutcome>(value)

    @TypeConverter
    fun fromDiagnosisOutcome(value: DiagnosisOutcome?): String? = value?.name

    @TypeConverter
    fun toClosureReason(value: String?): ClosureReason? = enumValueOrNull<ClosureReason>(value)

    @TypeConverter
    fun fromClosureReason(value: ClosureReason?): String? = value?.name

    @TypeConverter
    fun toCustomerActionStatus(value: String?): CustomerActionStatus? =
        enumValueOrNull<CustomerActionStatus>(value)

    @TypeConverter
    fun fromCustomerActionStatus(value: CustomerActionStatus?): String? = value?.name

    @TypeConverter
    fun toOrderDocumentType(value: String?): OrderDocumentType? =
        enumValueOrNull<OrderDocumentType>(value)

    @TypeConverter
    fun fromOrderDocumentType(value: OrderDocumentType?): String? = value?.name

    @TypeConverter
    fun toOrderPriority(value: String?): OrderPriority? = enumValueOrNull<OrderPriority>(value)

    @TypeConverter
    fun fromOrderPriority(value: OrderPriority?): String? = value?.name

    @TypeConverter
    fun toTimelineEventType(value: String?): TimelineEventType? =
        enumValueOrNull<TimelineEventType>(value)

    @TypeConverter
    fun fromTimelineEventType(value: TimelineEventType?): String? = value?.name

    @TypeConverter
    fun toCustomerFeedbackType(value: String?): CustomerFeedbackType? =
        enumValueOrNull<CustomerFeedbackType>(value)

    @TypeConverter
    fun fromCustomerFeedbackType(value: CustomerFeedbackType?): String? = value?.name

    @TypeConverter
    fun toMessageChannel(value: String?): MessageChannel? = enumValueOrNull<MessageChannel>(value)

    @TypeConverter
    fun fromMessageChannel(value: MessageChannel?): String? = value?.name

    @TypeConverter
    fun toMessageMode(value: String?): MessageMode? = enumValueOrNull<MessageMode>(value)

    @TypeConverter
    fun fromMessageMode(value: MessageMode?): String? = value?.name

    @TypeConverter
    fun toMessageStatus(value: String?): MessageStatus? = enumValueOrNull<MessageStatus>(value)

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus?): String? = value?.name

    @TypeConverter
    fun toAuditAction(value: String?): AuditAction? = enumValueOrNull<AuditAction>(value)

    @TypeConverter
    fun fromAuditAction(value: AuditAction?): String? = value?.name

    @TypeConverter
    fun toPermissionSet(value: String?): Set<UserPermission> =
        value
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { enumValueOrNull<UserPermission>(it) }
            ?.toSet() ?: emptySet()

    @TypeConverter
    fun fromPermissionSet(value: Set<UserPermission>?): String? =
        value?.joinToString(",") { it.name }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let {
            runCatching { enumValueOf<T>(it) }.getOrNull()
        }
}
