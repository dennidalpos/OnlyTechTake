package com.onlytechtake.repairapp.data.local.entities

enum class CustomerType {
    PRIVATE,
    BUSINESS,
}

enum class UserRole {
    ADMIN,
    OPERATOR,
}

enum class OrderStatus {
    NEW,
    DIAGNOSIS,
    WAITING_CUSTOMER,
    WORKING,
    WAITING_PARTS,
    READY_FOR_PICKUP,
    CLOSED,
}

enum class ClosureReason {
    COMPLETED,
    CUSTOMER_DECLINED,
    NOT_REPAIRABLE,
    CANCELLED,
    OTHER,
}

enum class OrderPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
}

enum class TimelineEventType {
    ORDER_CREATED,
    CUSTOMER_CREATED,
    STATUS_CHANGED,
    ESTIMATE_UPDATED,
    WHATSAPP_PREPARED,
    WHATSAPP_SENT_CONFIRMED,
    CUSTOMER_FEEDBACK_RECORDED,
    ORDER_REASSIGNED,
    INTERNAL_NOTE_ADDED,
    MEDIA_ADDED,
    SIGNATURE_CAPTURED,
    ORDER_CLOSED,
    ORDER_DELETED,
}

enum class CustomerFeedbackType {
    NONE,
    READ,
    ACCEPT,
    REJECT,
    ASK_INFO,
    CALL_ME,
    CUSTOM,
}

enum class MessageChannel {
    WHATSAPP,
}

enum class MessageMode {
    MANUAL_WHATSAPP,
}

enum class MessageStatus {
    DRAFT,
    PREPARED,
    SENT_CONFIRMED,
    FAILED,
}

enum class AuditAction {
    ADMIN_LOGIN,
    LOGIN_FAILED,
    USER_CREATED,
    USER_UPDATED,
    PASSWORD_RESET,
    TEMPLATE_UPDATED,
    PRIVACY_TEXT_UPDATED,
    DATA_EXPORTED,
    ORDER_DELETED,
    ORDER_REASSIGNED,
    ORDER_STATUS_CHANGED,
    DATA_RESET,
}
