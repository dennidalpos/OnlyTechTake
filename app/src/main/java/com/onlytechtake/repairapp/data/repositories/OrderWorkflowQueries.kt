package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.*
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

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
    ): Flow<List<RepairOrderEntity>> {
        val assignedToUserId = actor.id.takeIf { filter.showMineOnly }
        return repairOrderDao.observeFiltered(
            assignedToUserId = assignedToUserId,
            status = filter.status,
            customerId = filter.customerId,
        )
    }

    suspend fun countAllOrders(): Int = repairOrderDao.countAll()

    fun searchOrders(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<OrderListItem>> {
        val assignedToUserId = actor.id.takeIf { filter.showMineOnly }
        return repairOrderDao
            .search(
                query = filter.query,
                assignedToUserId = assignedToUserId,
                status = filter.status,
                customerId = filter.customerId,
            ).map { list ->
                list.map { projection ->
                    OrderListItem(
                        order = projection.order,
                        customer = projection.customer,
                        attachments =
                            OrderAttachmentCounts(
                                images = projection.imagesCount,
                                documents = projection.documentsCount,
                                signatures = projection.signaturesCount,
                                finalPhotos = projection.finalPhotosCount,
                            ),
                    )
                }
            }
    }

    fun observeOrderList(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<OrderListItem>> {
        val assignedToUserId = actor.id.takeIf { filter.showMineOnly }
        return repairOrderDao
            .observeOrderListProjection(
                assignedToUserId = assignedToUserId,
                status = filter.status,
                customerId = filter.customerId,
            ).map { list ->
                list.map { projection ->
                    OrderListItem(
                        order = projection.order,
                        customer = projection.customer,
                        attachments =
                            OrderAttachmentCounts(
                                images = projection.imagesCount,
                                documents = projection.documentsCount,
                                signatures = projection.signaturesCount,
                                finalPhotos = projection.finalPhotosCount,
                            ),
                    )
                }
            }
    }

    fun observeOrderDetail(orderId: String): Flow<OrderDetail?> {
        val orderFlow = repairOrderDao.observeById(orderId)
        val timelineFlow = timelineEventDao.observeForOrder(orderId)
        val messagesFlow = messageLogDao.observeForOrder(orderId)
        val mediaFlow = orderMediaDao.observeForOrder(orderId)
        val signaturesFlow = signatureDao.observeForOrder(orderId)

        return combine(
            orderFlow,
            timelineFlow,
            messagesFlow,
            mediaFlow,
            signaturesFlow,
        ) { order, timeline, messages, media, signatures ->
            order?.let {
                val customer = it.customerId.let { id -> customerDao.findById(id) }
                val category = it.categoryId.let { id -> categoryDao.findById(id) }
                val assignedUser = it.assignedToUserId?.let { id -> userDao.findById(id) }

                OrderDetail(
                    order = it,
                    customer = customer,
                    category = category,
                    assignedUser = assignedUser,
                    timeline = timeline,
                    messages = messages,
                    media = media,
                    signatures = signatures,
                )
            }
        }
    }
}
