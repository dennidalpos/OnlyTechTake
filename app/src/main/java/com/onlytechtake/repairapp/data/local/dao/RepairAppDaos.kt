package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.LoginAttemptEntity
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import kotlinx.coroutines.flow.Flow

data class OrderMediaAggregate(
    val orderId: String,
    val images: Int,
    val documents: Int,
    val finalPhotos: Int,
)

data class SignatureAggregate(
    val orderId: String,
    val signatures: Int,
)

data class OrderExportRow(
    val orderNumber: String,
    val title: String,
    val status: OrderStatus,
    val priority: OrderPriority,
    val customerName: String?,
    val customerType: CustomerType?,
    val phone: String?,
    val email: String?,
    val categoryName: String?,
    val assignedUserName: String?,
    val description: String?,
    val internalNotes: String?,
    val diagnosisOutcome: DiagnosisOutcome? = null,
    val isPaid: Boolean,
    val estimateAmountCents: Long?,
    val finalAmountCents: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long?,
    val closureReason: ClosureReason?,
)

data class OrderListItemProjection(
    @Embedded val order: RepairOrderEntity,
    @Embedded(prefix = "cust_") val customer: CustomerEntity?,
    val imagesCount: Int,
    val documentsCount: Int,
    val signaturesCount: Int,
    val finalPhotosCount: Int,
)

@Dao
interface DashboardDao {
    @Query("SELECT COUNT(*) FROM users")
    fun observeUserCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM repair_orders")
    fun observeActiveOrderCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers")
    fun observeCustomerCount(): Flow<Int>
}

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
    suspend fun exportOrdersPaged(limit: Int, offset: Int): List<OrderExportRow>
}

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users ORDER BY role ASC, active DESC, displayName ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE active = 1 ORDER BY displayName ASC")
    fun observeActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'ADMIN'")
    suspend fun findAllAdmins(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    fun observeCountByRole(role: UserRole): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE role = :role AND active = 1")
    suspend fun countActiveByRole(role: UserRole): Int
}

@Dao
interface CustomerDao {
    @Upsert
    suspend fun upsert(customer: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun observeRecent(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CustomerEntity?

    @Query(
        """
        SELECT * FROM customers
        WHERE :query = '' 
           OR rowid IN (SELECT rowid FROM customers_fts WHERE customers_fts MATCH :query || '*')
           OR id IN (SELECT customerId FROM repair_orders WHERE rowid IN (SELECT rowid FROM repair_orders_fts WHERE repair_orders_fts MATCH :query || '*'))
        ORDER BY updatedAt DESC
        """,
    )
    fun search(query: String): Flow<List<CustomerEntity>>
}

@Dao
interface OrderCategoryDao {
    @Upsert
    suspend fun upsert(category: OrderCategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<OrderCategoryEntity>)

    @Query("SELECT * FROM order_categories ORDER BY level ASC, parentId ASC, active DESC, name COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<OrderCategoryEntity>>

    @Query("SELECT * FROM order_categories WHERE active = 1 ORDER BY level ASC, parentId ASC, name COLLATE NOCASE ASC, id ASC")
    fun observeActiveCategories(): Flow<List<OrderCategoryEntity>>

    @Query("SELECT * FROM order_categories")
    suspend fun findAll(): List<OrderCategoryEntity>

    @Query("SELECT * FROM order_categories WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): OrderCategoryEntity?

    @Query("SELECT COUNT(*) FROM repair_orders WHERE categoryId IN (:categoryIds)")
    suspend fun countOrdersForCategories(categoryIds: List<String>): Int

    @Query("DELETE FROM order_categories WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query(
        """
        SELECT * FROM order_categories
        WHERE name = :name
            AND ((:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId)
        LIMIT 1
        """,
    )
    suspend fun findByParentAndName(parentId: String?, name: String): OrderCategoryEntity?
}

@Dao
interface RepairOrderDao {
    @Upsert
    suspend fun upsert(order: RepairOrderEntity)

    @Query("SELECT * FROM repair_orders WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): RepairOrderEntity?

    @Query("SELECT COUNT(*) FROM repair_orders")
    suspend fun countAll(): Int

    @Query("SELECT * FROM repair_orders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<RepairOrderEntity?>

    @Query(
        """
        SELECT * FROM repair_orders
        WHERE (:assignedToUserId IS NULL OR assignedToUserId = :assignedToUserId)
            AND (:status IS NULL OR status = :status)
            AND (:customerId IS NULL OR customerId = :customerId)
        ORDER BY updatedAt DESC
        LIMIT :limit
        """,
    )
    fun observeFiltered(
        assignedToUserId: String?,
        status: OrderStatus?,
        customerId: String?,
        limit: Int = 100,
    ): Flow<List<RepairOrderEntity>>

    @Query("SELECT orderNumber FROM repair_orders WHERE orderNumber LIKE :yearPrefix || '%' ORDER BY orderNumber DESC LIMIT 1")
    suspend fun findLatestOrderNumberForYear(yearPrefix: String): String?

    @Query("SELECT * FROM repair_orders ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentActiveOrders(limit: Int = 50): Flow<List<RepairOrderEntity>>

    @Query(
        """
        SELECT 
            o.*,
            c.id AS cust_id, c.type AS cust_type, c.firstName AS cust_firstName, c.lastName AS cust_lastName, 
            c.companyName AS cust_companyName, c.vatNumber AS cust_vatNumber, c.taxCode AS cust_taxCode, 
            c.phone AS cust_phone, c.email AS cust_email, c.address AS cust_address, c.notes AS cust_notes,
            c.createdAt AS cust_createdAt, c.updatedAt AS cust_updatedAt,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND (UPPER(m.mediaType) IN ('PHOTO', 'IMAGE') OR (m.mimeType LIKE 'image/%' AND UPPER(m.mediaType) != 'FINAL_PHOTO'))) AS imagesCount,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND NOT (UPPER(m.mediaType) IN ('PHOTO', 'IMAGE', 'FINAL_PHOTO') OR m.mimeType LIKE 'image/%')) AS documentsCount,
            (SELECT COUNT(*) FROM signatures s WHERE s.orderId = o.id) AS signaturesCount,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND UPPER(m.mediaType) = 'FINAL_PHOTO') AS finalPhotosCount
        FROM repair_orders o
        LEFT JOIN customers c ON o.customerId = c.id
        WHERE (:assignedToUserId IS NULL OR o.assignedToUserId = :assignedToUserId)
            AND (:status IS NULL OR o.status = :status)
            AND (:customerId IS NULL OR o.customerId = :customerId)
        ORDER BY o.updatedAt DESC
        LIMIT :limit
        """
    )
    fun observeOrderListProjection(
        assignedToUserId: String?,
        status: OrderStatus?,
        customerId: String?,
        limit: Int = 100
    ): Flow<List<OrderListItemProjection>>

    @Query("DELETE FROM repair_orders WHERE id IN (:orderIds)")
    suspend fun deleteByIds(orderIds: List<String>): Int

    @Query(
        """
        SELECT 
            o.*,
            c.id AS cust_id, c.type AS cust_type, c.firstName AS cust_firstName, c.lastName AS cust_lastName, 
            c.companyName AS cust_companyName, c.vatNumber AS cust_vatNumber, c.taxCode AS cust_taxCode, 
            c.phone AS cust_phone, c.email AS cust_email, c.address AS cust_address, c.notes AS cust_notes,
            c.createdAt AS cust_createdAt, c.updatedAt AS cust_updatedAt,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND (UPPER(m.mediaType) IN ('PHOTO', 'IMAGE') OR (m.mimeType LIKE 'image/%' AND UPPER(m.mediaType) != 'FINAL_PHOTO'))) AS imagesCount,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND NOT (UPPER(m.mediaType) IN ('PHOTO', 'IMAGE', 'FINAL_PHOTO') OR m.mimeType LIKE 'image/%')) AS documentsCount,
            (SELECT COUNT(*) FROM signatures s WHERE s.orderId = o.id) AS signaturesCount,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND UPPER(m.mediaType) = 'FINAL_PHOTO') AS finalPhotosCount
        FROM repair_orders o
        LEFT JOIN customers c ON o.customerId = c.id
        WHERE :query = ''
           OR o.rowid IN (SELECT rowid FROM repair_orders_fts WHERE repair_orders_fts MATCH :query || '*')
           OR c.rowid IN (SELECT rowid FROM customers_fts WHERE customers_fts MATCH :query || '*')
        ORDER BY o.updatedAt DESC
        LIMIT :limit
        """
    )
    fun search(query: String, limit: Int = 100): Flow<List<OrderListItemProjection>>
}

@Dao
interface TimelineEventDao {
    @Upsert
    suspend fun upsert(event: TimelineEventEntity)

    @Query("SELECT * FROM timeline_events WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun observeForOrder(orderId: String): Flow<List<TimelineEventEntity>>
}

@Dao
interface MessageTemplateDao {
    @Upsert
    suspend fun upsert(template: MessageTemplateEntity)

    @Query("SELECT * FROM message_templates ORDER BY code ASC")
    fun observeAll(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE code = :code AND active = 1 LIMIT 1")
    suspend fun findActiveByCode(code: String): MessageTemplateEntity?

    @Query("SELECT * FROM message_templates WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): MessageTemplateEntity?
}

@Dao
interface OrderMediaDao {
    @Upsert
    suspend fun upsert(media: OrderMediaEntity)

    @Query("SELECT * FROM order_media WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): OrderMediaEntity?

    @Query("SELECT * FROM order_media WHERE orderId = :orderId ORDER BY capturedAt DESC")
    fun observeForOrder(orderId: String): Flow<List<OrderMediaEntity>>

    @Query("SELECT * FROM order_media WHERE orderId IN (:orderIds)")
    suspend fun findForOrders(orderIds: List<String>): List<OrderMediaEntity>

    @Query(
        """
        SELECT
            orderId,
            COUNT(CASE WHEN UPPER(mediaType) IN ('PHOTO', 'IMAGE') OR (mimeType LIKE 'image/%' AND UPPER(mediaType) != 'FINAL_PHOTO') THEN 1 END) AS images,
            COUNT(CASE WHEN NOT (UPPER(mediaType) IN ('PHOTO', 'IMAGE', 'FINAL_PHOTO') OR mimeType LIKE 'image/%') THEN 1 END) AS documents,
            COUNT(CASE WHEN UPPER(mediaType) = 'FINAL_PHOTO' THEN 1 END) AS finalPhotos
        FROM order_media
        GROUP BY orderId
        """,
    )
    fun observeAggregates(): Flow<List<OrderMediaAggregate>>

    @Query("DELETE FROM order_media WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT localPath FROM order_media")
    suspend fun getAllPaths(): List<String>
}

@Dao
interface SignatureDao {
    @Upsert
    suspend fun upsert(signature: SignatureEntity)

    @Query("SELECT * FROM signatures WHERE orderId = :orderId ORDER BY signedAt DESC")
    fun observeForOrder(orderId: String): Flow<List<SignatureEntity>>

    @Query("SELECT * FROM signatures WHERE orderId IN (:orderIds)")
    suspend fun findForOrders(orderIds: List<String>): List<SignatureEntity>

    @Query("SELECT COUNT(*) FROM signatures WHERE orderId = :orderId")
    suspend fun countForOrder(orderId: String): Int

    @Query("SELECT orderId, COUNT(*) AS signatures FROM signatures GROUP BY orderId")
    fun observeAggregates(): Flow<List<SignatureAggregate>>

    @Query("SELECT localPath FROM signatures")
    suspend fun getAllPaths(): List<String>
}

@Dao
interface MessageLogDao {
    @Upsert
    suspend fun upsert(log: MessageLogEntity)

    @Query("SELECT * FROM message_logs WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun observeForOrder(orderId: String): Flow<List<MessageLogEntity>>

    @Query("SELECT * FROM message_logs WHERE orderId = :orderId ORDER BY createdAt DESC LIMIT 1")
    suspend fun findLatestForOrder(orderId: String): MessageLogEntity?
}

@Dao
interface AppSettingDao {
    @Upsert
    suspend fun upsert(setting: AppSettingEntity)

    @Query("SELECT * FROM app_settings ORDER BY `key` ASC")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): AppSettingEntity?
}

@Dao
interface AuditLogDao {
    @Upsert
    suspend fun upsert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<AuditLogEntity>>
}

@Dao
interface LoginAttemptDao {
    @Upsert
    suspend fun upsert(attempt: LoginAttemptEntity)

    @Query("SELECT COUNT(*) FROM login_attempts WHERE username = :username AND successful = 0 AND attemptAt > :since")
    suspend fun countFailedAttempts(username: String, since: Long): Int

    @Query("DELETE FROM login_attempts WHERE username = :username")
    suspend fun clearAttempts(username: String)

    @Query("DELETE FROM login_attempts WHERE attemptAt < :before")
    suspend fun cleanupOldAttempts(before: Long)
}

@Dao
interface OrderDocumentDao {
    @Upsert
    suspend fun upsert(document: OrderDocumentEntity)

    @Query(
        """
        SELECT * FROM order_documents
        WHERE orderId = :orderId
        ORDER BY createdAt DESC
        """,
    )
    fun observeByOrder(orderId: String): Flow<List<OrderDocumentEntity>>

    @Query("SELECT * FROM order_documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OrderDocumentEntity?

    @Query("UPDATE order_documents SET sharedAt = :sharedAt WHERE id = :id")
    suspend fun markShared(id: String, sharedAt: Long)

    @Query("DELETE FROM order_documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT localPath FROM order_documents")
    suspend fun getAllPaths(): List<String>
}
