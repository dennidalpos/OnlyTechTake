package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.OrderMediaDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.SignatureDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    ): Flow<List<RepairOrderEntity>> {
        val assignedToUserId =
            if (filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.MINE || filter.showMineOnly) actor.id else null
        val unassignedOnly =
            filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.OPEN_UNASSIGNED
        val assignedOnly =
            filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.IN_PROGRESS
        val isClosed =
            when (filter.mode) {
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.CLOSED -> true
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.OPEN_UNASSIGNED,
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.IN_PROGRESS,
                    -> false

                else -> {
                    if (filter.statuses.isNotEmpty()) {
                        val hasClosed =
                            filter.statuses.contains(com.onlytechtake.repairapp.data.local.entities.OrderStatus.CLOSED)
                        if (hasClosed && filter.statuses.size == 1) {
                            true
                        } else if (!hasClosed) {
                            false
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }

        return repairOrderDao.observeFiltered(
            assignedToUserId = assignedToUserId,
            status = filter.status,
            customerId = filter.customerId,
            unassignedOnly = unassignedOnly,
            assignedOnly = assignedOnly,
            isClosed = isClosed,
        )
    }

    suspend fun countAllOrders(): Int = repairOrderDao.countAll()

    fun searchOrders(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<OrderListItem>> {
        val assignedToUserId =
            if (filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.MINE || filter.showMineOnly) actor.id else null
        val unassignedOnly =
            filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.OPEN_UNASSIGNED
        val assignedOnly =
            filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.IN_PROGRESS
        val isClosed =
            when (filter.mode) {
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.CLOSED -> true
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.OPEN_UNASSIGNED,
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.IN_PROGRESS,
                    -> false

                else -> {
                    if (filter.statuses.isNotEmpty()) {
                        val hasClosed =
                            filter.statuses.contains(com.onlytechtake.repairapp.data.local.entities.OrderStatus.CLOSED)
                        if (hasClosed && filter.statuses.size == 1) {
                            true
                        } else if (!hasClosed) {
                            false
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }

        return repairOrderDao
            .search(
                query = filter.query,
                assignedToUserId = assignedToUserId,
                status = filter.status,
                customerId = filter.customerId,
                unassignedOnly = unassignedOnly,
                assignedOnly = assignedOnly,
                isClosed = isClosed,
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
        val assignedToUserId =
            if (filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.MINE || filter.showMineOnly) actor.id else null
        val unassignedOnly =
            filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.OPEN_UNASSIGNED
        val assignedOnly =
            filter.mode == com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.IN_PROGRESS
        val isClosed =
            when (filter.mode) {
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.CLOSED -> true
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.OPEN_UNASSIGNED,
                com.onlytechtake.repairapp.domain.workspace.OrderFilterMode.IN_PROGRESS,
                    -> false

                else -> {
                    if (filter.statuses.isNotEmpty()) {
                        val hasClosed =
                            filter.statuses.contains(com.onlytechtake.repairapp.data.local.entities.OrderStatus.CLOSED)
                        if (hasClosed && filter.statuses.size == 1) {
                            true
                        } else if (!hasClosed) {
                            false
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }

        return repairOrderDao
            .observeOrderListProjection(
                assignedToUserId = assignedToUserId,
                status = filter.status,
                customerId = filter.customerId,
                unassignedOnly = unassignedOnly,
                assignedOnly = assignedOnly,
                isClosed = isClosed,
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
