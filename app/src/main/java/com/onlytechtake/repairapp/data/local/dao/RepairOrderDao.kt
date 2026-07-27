package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.domain.models.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairOrderDao {
    @Upsert
    suspend fun upsert(order: RepairOrderEntity)

    @Upsert
    suspend fun upsertAll(orders: List<RepairOrderEntity>)

    @Query("SELECT * FROM repair_orders WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): RepairOrderEntity?

    @Query("SELECT COUNT(*) FROM repair_orders")
    suspend fun countAll(): Int

    @Query("SELECT * FROM repair_orders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<RepairOrderEntity?>

    @Query("DELETE FROM repair_orders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        SELECT * FROM repair_orders
        WHERE (:status IS NULL OR status = :status)
        AND (:customerId IS NULL OR customerId = :customerId)
        AND (:useStatuses = 0 OR status IN (:statuses))
        ORDER BY updatedAt DESC
        LIMIT :limit
        """,
    )
    fun observeFiltered(
        status: OrderStatus? = null,
        customerId: String? = null,
        useStatuses: Boolean = false,
        statuses: List<OrderStatus> = emptyList(),
        limit: Int = 100,
    ): Flow<List<RepairOrderEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM repair_orders WHERE customerId = :customerId LIMIT 1)")
    suspend fun hasOrdersForCustomer(customerId: String): Boolean

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
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND (UPPER(m.mediaType) = 'VIDEO' OR m.mimeType LIKE 'video/%')) AS videosCount,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND NOT (UPPER(m.mediaType) IN ('PHOTO', 'IMAGE', 'FINAL_PHOTO', 'VIDEO') OR m.mimeType LIKE 'image/%' OR m.mimeType LIKE 'video/%')) AS documentsCount,
            (SELECT COUNT(*) FROM signatures s WHERE s.orderId = o.id) AS signaturesCount,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND UPPER(m.mediaType) = 'FINAL_PHOTO') AS finalPhotosCount
        FROM repair_orders o
        LEFT JOIN customers c ON o.customerId = c.id
        WHERE (:status IS NULL OR o.status = :status)
        AND (:customerId IS NULL OR o.customerId = :customerId)
        AND (:useStatuses = 0 OR o.status IN (:statuses))
        ORDER BY o.updatedAt DESC
        LIMIT :limit
        """,
    )
    fun observeOrderListProjection(
        status: OrderStatus? = null,
        customerId: String? = null,
        useStatuses: Boolean = false,
        statuses: List<OrderStatus> = emptyList(),
        limit: Int = 100,
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
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND (UPPER(m.mediaType) = 'VIDEO' OR m.mimeType LIKE 'video/%')) AS videosCount,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND NOT (UPPER(m.mediaType) IN ('PHOTO', 'IMAGE', 'FINAL_PHOTO', 'VIDEO') OR m.mimeType LIKE 'image/%' OR m.mimeType LIKE 'video/%')) AS documentsCount,
            (SELECT COUNT(*) FROM signatures s WHERE s.orderId = o.id) AS signaturesCount,
            (SELECT COUNT(*) FROM order_media m WHERE m.orderId = o.id AND UPPER(m.mediaType) = 'FINAL_PHOTO') AS finalPhotosCount
        FROM repair_orders o
        LEFT JOIN customers c ON o.customerId = c.id
        WHERE ((:query = ''
           OR o.rowid IN (SELECT rowid FROM repair_orders_fts WHERE repair_orders_fts MATCH :query || '*')
           OR c.rowid IN (SELECT rowid FROM customers_fts WHERE customers_fts MATCH :query || '*')))
           AND (:status IS NULL OR o.status = :status)
           AND (:customerId IS NULL OR o.customerId = :customerId)
           AND (:useStatuses = 0 OR o.status IN (:statuses))
        ORDER BY o.updatedAt DESC
        LIMIT :limit
        """,
    )
    fun search(
        query: String,
        status: OrderStatus? = null,
        customerId: String? = null,
        useStatuses: Boolean = false,
        statuses: List<OrderStatus> = emptyList(),
        limit: Int = 100,
    ): Flow<List<OrderListItemProjection>>
}
