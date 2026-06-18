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
    OPEN,
    IN_PROGRESS,
    DIAGNOSIS,
    RESULT,
    REPAIRING,
    READY_FOR_PICKUP,
    CLOSED,
}

enum class DiagnosisOutcome {
    REPAIRABLE,
    NOT_REPAIRABLE,
}

enum class CustomerActionStatus {
    NONE,
    TO_NOTIFY,
    WAITING_ACCEPTANCE,
    ACCEPTED,
    REJECTED,
    ASKED_INFO,
    CALL_REQUESTED,
    NOTIFIED,
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
    STATUS_CHANGED,
    WHATSAPP_PREPARED,
    WHATSAPP_SENT_CONFIRMED,
    CUSTOMER_FEEDBACK_RECORDED,
    ORDER_REASSIGNED,
    INTERNAL_NOTE_ADDED,
    MEDIA_ADDED,
    MEDIA_REMOVED,
    SIGNATURE_CAPTURED,
    ORDER_DELETED,
    PDF_STATUS_SHARED,
    CUSTOMER_ACTION_STATUS_CHANGED,
    PAYMENT_RECEIVED,
    PAYMENT_REMOVED,
}

enum class CustomerFeedbackType {
    READ,
    ACCEPT,
    REJECT,
    ASK_INFO,
    CALL_ME,
}

enum class MessageChannel {
    WHATSAPP,
}

enum class MessageMode {
    MANUAL_WHATSAPP,
}

enum class MessageStatus {
    PREPARED,
    SENT_CONFIRMED,
}

enum class OrderDocumentType {
    STATUS_PROGRESS_PDF,
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
    ORDER_CLEANUP,
    DATA_RESET,
}

enum class UserPermission {
    CREATE_ORDERS,
    EDIT_OWN_ORDERS,
    EDIT_ALL_ORDERS,
    CLOSE_ORDERS,
    DELETE_DATA,
    MANAGE_CUSTOMERS,
    MANAGE_CATEGORIES,
    MANAGE_TEMPLATES,
    MANAGE_USERS,
    EXPORT_DATA,
    BACKUP_RESTORE,
    MANAGE_STORAGE,
    MANAGE_PAYMENTS,
}
