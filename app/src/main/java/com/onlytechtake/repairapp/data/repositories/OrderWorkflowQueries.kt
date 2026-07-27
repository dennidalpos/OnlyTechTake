package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.OrderMediaDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.SignatureDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.Order
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
internal class OrderWorkflowQueries(
    private val repairOrderDao: RepairOrderDao,
    private val customerDao: CustomerDao,
    private val categoryDao: OrderCategoryDao,
    private val userDao: UserDao,
    private val timelineEventDao: TimelineEventDao,
    private val orderMediaDao: OrderMediaDao,
    private val signatureDao: SignatureDao,
    private val messageLogDao: MessageLogDao,
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

    fun observeOrderDetail(orderId: String): Flow<OrderDetail?> =
        repairOrderDao.observeById(orderId).flatMapLatest { order ->
            if (order == null) return@flatMapLatest flowOf(null)

            val timelineFlow = timelineEventDao.observeForOrder(orderId)
            val mediaFlow = orderMediaDao.observeForOrder(orderId)
            val signaturesFlow = signatureDao.observeForOrder(orderId)
            val customerFlow = customerDao.observeById(order.customerId)
            val categoryFlow = categoryDao.observeById(order.categoryId)
            val assignedUserFlow =
                order.assignedToUserId?.let {
                    userDao.observeById(it)
                } ?: flowOf(null)

            combine(
                timelineFlow,
                mediaFlow,
                signaturesFlow,
                customerFlow,
                categoryFlow,
                assignedUserFlow,
            ) { array: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                OrderDetail(
                    order = order.toDomain(),
                    customer = (array[3] as com.onlytechtake.repairapp.data.local.entities.CustomerEntity?)?.toDomain(),
                    category = (array[4] as com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity?)?.toDomain(),
                    assignedUser = (array[5] as com.onlytechtake.repairapp.data.local.entities.UserEntity?)?.toDomain(),
                    timeline = (array[0] as List<com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity>).map { it.toDomain() },
                    media = (array[1] as List<com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity>).map { it.toDomain() },
                    signatures = (array[2] as List<com.onlytechtake.repairapp.data.local.entities.SignatureEntity>).map { it.toDomain() },
                )
            }.distinctUntilChanged()
        }
}
