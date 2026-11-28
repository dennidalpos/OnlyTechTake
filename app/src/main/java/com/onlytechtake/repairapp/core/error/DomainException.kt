package com.onlytechtake.repairapp.core.error

sealed class DomainException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class OrderNotFound(
        orderId: String,
    ) : DomainException("Commessa non trovata: $orderId")

    class CustomerNotFound(
        customerId: String,
    ) : DomainException("Cliente non trovato: $customerId")

    class PermissionDenied(
        permission: String,
    ) : DomainException("Permesso negato: $permission")

    class IllegalStatusTransition(
        from: String,
        to: String,
    ) : DomainException("Transizione di stato non valida: $from -> $to")

    class BusinessRuleViolation(
        message: String,
    ) : DomainException(message)
}
