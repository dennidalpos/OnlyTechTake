package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.core.orders.OrderNumberGenerator
import com.onlytechtake.repairapp.data.local.dao.*
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.*
import kotlinx.coroutines.flow.Flow

import com.onlytechtake.repairapp.domain.repositories.OrderRepository

class OrderWorkflowRepository(
    private val context: Context,
    private val repairOrderDao: RepairOrderDao,
    private val customerDao: CustomerDao,
    private val categoryDao: OrderCategoryDao,
    private val userDao: UserDao,
    private val timelineEventDao: TimelineEventDao,
    private val orderMediaDao: OrderMediaDao,
    private val signatureDao: SignatureDao,
    private val messageLogDao: MessageLogDao,
    private val auditLogDao: AuditLogDao,
    private val database: RepairAppDatabase?,
    private val orderNumberGenerator: OrderNumberGenerator = OrderNumberGenerator(),
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : OrderRepository {
    private val eventWriter = OrderWorkflowEventWriter(
        context = context,
        timelineEventDao = timelineEventDao,
        auditLogDao = auditLogDao,
    )

    private val queries = OrderWorkflowQueries(
        repairOrderDao = repairOrderDao,
        customerDao = customerDao,
        categoryDao = categoryDao,
        userDao = userDao,
        timelineEventDao = timelineEventDao,
        orderMediaDao = orderMediaDao,
        signatureDao = signatureDao,
        messageLogDao = messageLogDao,
    )

    private val commands = OrderWorkflowCommands(
        context = context,
        repairOrderDao = repairOrderDao,
        customerDao = customerDao,
        categoryDao = categoryDao,
        messageLogDao = messageLogDao,
        database = database,
        orderNumberGenerator = orderNumberGenerator,
        clockMillis = clockMillis,
        eventWriter = eventWriter,
    )

    override fun observeOrders(actor: AuthenticatedUser, filter: OrderFilter): Flow<List<RepairOrderEntity>> =
        queries.observeOrders(actor, filter)

    override suspend fun countAllOrders(): Int = queries.countAllOrders()

    override fun searchOrders(actor: AuthenticatedUser, filter: OrderFilter): Flow<List<OrderListItem>> =
        queries.searchOrders(actor, filter)

    override fun observeOrderList(actor: AuthenticatedUser, filter: OrderFilter): Flow<List<OrderListItem>> =
        queries.observeOrderList(actor, filter)

    override fun observeOrderDetail(orderId: String): Flow<OrderDetail?> =
        queries.observeOrderDetail(orderId)

    override suspend fun createOrder(actor: AuthenticatedUser, input: RepairOrderInput): Result<RepairOrderEntity> =
        commands.createOrder(actor, input)

    override suspend fun changeStatus(
        actor: AuthenticatedUser,
        orderId: String,
        status: OrderStatus,
        note: String,
        closureReason: ClosureReason?,
    ): Result<Unit> =
        commands.changeStatus(actor, orderId, status, note, closureReason)

    override suspend fun recordResult(
        actor: AuthenticatedUser,
        orderId: String,
        input: DiagnosisResultInput,
    ): Result<Unit> =
        commands.recordResult(actor, orderId, input)

    override suspend fun reassign(actor: AuthenticatedUser, orderId: String, assignedToUserId: String?): Result<Unit> =
        commands.reassign(actor, orderId, assignedToUserId)

    override suspend fun addInternalNote(actor: AuthenticatedUser, orderId: String, note: String): Result<Unit> =
        commands.addInternalNote(actor, orderId, note)

    override suspend fun recordFeedback(actor: AuthenticatedUser, orderId: String, input: FeedbackInput): Result<Unit> =
        commands.recordFeedback(actor, orderId, input)

    override suspend fun recordPdfShared(actor: AuthenticatedUser, orderId: String, fileName: String): Result<Unit> =
        commands.recordPdfShared(actor, orderId, fileName)

    override suspend fun recordMediaRemoved(actor: AuthenticatedUser, orderId: String, fileName: String): Result<Unit> =
        commands.recordMediaRemoved(actor, orderId, fileName)

    override suspend fun togglePaymentStatus(actor: AuthenticatedUser, orderId: String, isPaid: Boolean): Result<Unit> =
        commands.togglePaymentStatus(actor, orderId, isPaid)
}
