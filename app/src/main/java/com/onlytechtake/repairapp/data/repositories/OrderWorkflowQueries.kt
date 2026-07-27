package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.Order
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class OrderWorkflowQueries(
    private val repairOrderDao: RepairOrderDao,
) {
    fun observeOrders(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<Order>> =
        repairOrderDao
            .observeFiltered(
                status = filter.status,
                customerId = filter.customerId,
                useStatuses = filter.statuses.isNotEmpty(),
                statuses = filter.statuses.toList(),
            ).distinctUntilChanged()
            .map { list -> list.map { it.toDomain() } }

    suspend fun countAllOrders(): Int = repairOrderDao.countAll()

    fun searchOrders(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<OrderListItem>> =
        repairOrderDao
            .search(
                query = filter.query,
                status = filter.status,
                customerId = filter.customerId,
                useStatuses = filter.statuses.isNotEmpty(),
                statuses = filter.statuses.toList(),
            ).distinctUntilChanged()
            .map { list ->
                list.map { projection ->
                    OrderListItem(
                        order = projection.order.toDomain(),
                        customer = projection.customer?.toDomain(),
                        attachments =
                            OrderAttachmentCounts(
                                images = projection.imagesCount,
                                videos = projection.videosCount,
                                documents = projection.documentsCount,
                                signatures = projection.signaturesCount,
                                finalPhotos = projection.finalPhotosCount,
                            ),
                    )
                }
            }

    fun observeOrderList(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<OrderListItem>> =
        repairOrderDao
            .observeOrderListProjection(
                status = filter.status,
                customerId = filter.customerId,
                useStatuses = filter.statuses.isNotEmpty(),
                statuses = filter.statuses.toList(),
            ).distinctUntilChanged()
            .map { list ->
                list.map { projection ->
                    OrderListItem(
                        order = projection.order.toDomain(),
                        customer = projection.customer?.toDomain(),
                        attachments =
                            OrderAttachmentCounts(
                                images = projection.imagesCount,
                                videos = projection.videosCount,
                                documents = projection.documentsCount,
                                signatures = projection.signaturesCount,
                                finalPhotos = projection.finalPhotosCount,
                            ),
                    )
                }
            }

    fun observeOrderDetail(orderId: String): Flow<OrderDetail?> = repairOrderDao.observeOrderDetailProjection(orderId).map { it?.toDomain() }.distinctUntilChanged()
}
