package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface OrderExportDao {
    @Query(
        """
        SELECT
            repair_orders.orderNumber AS orderNumber,
            repair_orders.title AS title,
            repair_orders.status AS status,
            repair_orders.priority AS priority,
            CASE
                WHEN customers.type = 'BUSINESS' THEN customers.companyName
                ELSE TRIM(COALESCE(customers.firstName, '') || ' ' || COALESCE(customers.lastName, ''))
            END AS customerName,
            customers.type AS customerType,
            customers.phone AS phone,
            customers.email AS email,
            COALESCE(repair_orders.categoryPathSnapshot, order_categories.name) AS categoryName,
            users.displayName AS assignedUserName,
            (SELECT s.signerName FROM signatures s WHERE s.orderId = repair_orders.id ORDER BY s.signedAt DESC LIMIT 1) AS signerName,
            repair_orders.description AS description,
            repair_orders.internalNotes AS internalNotes,
            repair_orders.diagnosisOutcome AS diagnosisOutcome,
            CASE
                WHEN (
                    SELECT timeline_events.type
                    FROM timeline_events
                    WHERE timeline_events.orderId = repair_orders.id
                        AND timeline_events.type IN ('PAYMENT_RECEIVED', 'PAYMENT_REMOVED')
                    ORDER BY timeline_events.createdAt DESC, timeline_events.id DESC
                    LIMIT 1
                ) = 'PAYMENT_RECEIVED' THEN 1
                ELSE 0
            END AS isPaid,
            repair_orders.estimateAmountCents AS estimateAmountCents,
            repair_orders.finalAmountCents AS finalAmountCents,
            repair_orders.createdAt AS createdAt,
            repair_orders.updatedAt AS updatedAt,
            repair_orders.closedAt AS closedAt,
            repair_orders.closureReason AS closureReason
        FROM repair_orders
        LEFT JOIN customers ON customers.id = repair_orders.customerId
        LEFT JOIN order_categories ON order_categories.id = repair_orders.categoryId
        LEFT JOIN users ON users.id = repair_orders.assignedToUserId
        ORDER BY repair_orders.createdAt DESC, repair_orders.orderNumber DESC
        """,
    )
    suspend fun exportAllOrders(): List<OrderExportRow>

    @Query(
        """
        SELECT
            repair_orders.orderNumber AS orderNumber,
            repair_orders.title AS title,
            repair_orders.status AS status,
            repair_orders.priority AS priority,
            CASE
                WHEN customers.type = 'BUSINESS' THEN customers.companyName
                ELSE TRIM(COALESCE(customers.firstName, '') || ' ' || COALESCE(customers.lastName, ''))
            END AS customerName,
            customers.type AS customerType,
            customers.phone AS phone,
            customers.email AS email,
            COALESCE(repair_orders.categoryPathSnapshot, order_categories.name) AS categoryName,
            users.displayName AS assignedUserName,
            (SELECT s.signerName FROM signatures s WHERE s.orderId = repair_orders.id ORDER BY s.signedAt DESC LIMIT 1) AS signerName,
            repair_orders.description AS description,
            repair_orders.internalNotes AS internalNotes,
            repair_orders.diagnosisOutcome AS diagnosisOutcome,
            CASE
                WHEN (
                    SELECT timeline_events.type
                    FROM timeline_events
                    WHERE timeline_events.orderId = repair_orders.id
                        AND timeline_events.type IN ('PAYMENT_RECEIVED', 'PAYMENT_REMOVED')
                    ORDER BY timeline_events.createdAt DESC, timeline_events.id DESC
                    LIMIT 1
                ) = 'PAYMENT_RECEIVED' THEN 1
                ELSE 0
            END AS isPaid,
            repair_orders.estimateAmountCents AS estimateAmountCents,
            repair_orders.finalAmountCents AS finalAmountCents,
            repair_orders.createdAt AS createdAt,
            repair_orders.updatedAt AS updatedAt,
            repair_orders.closedAt AS closedAt,
            repair_orders.closureReason AS closureReason
        FROM repair_orders
        LEFT JOIN customers ON customers.id = repair_orders.customerId
        LEFT JOIN order_categories ON order_categories.id = repair_orders.categoryId
        LEFT JOIN users ON users.id = repair_orders.assignedToUserId
        ORDER BY repair_orders.createdAt DESC, repair_orders.orderNumber DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun exportOrdersPaged(
        limit: Int,
        offset: Int,
    ): List<OrderExportRow>
}
