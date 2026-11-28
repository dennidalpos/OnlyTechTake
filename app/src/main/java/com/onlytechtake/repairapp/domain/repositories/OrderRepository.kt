package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.ClosureReason
import com.onlytechtake.repairapp.domain.models.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.models.FeedbackInput
import com.onlytechtake.repairapp.domain.models.Order
import com.onlytechtake.repairapp.domain.models.OrderDetail
import com.onlytechtake.repairapp.domain.models.OrderFilter
import com.onlytechtake.repairapp.domain.models.OrderListItem
import com.onlytechtake.repairapp.domain.models.OrderPriority
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.models.RepairOrderInput
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<Order>>

    suspend fun countAllOrders(): Int

    fun searchOrders(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<OrderListItem>>

    fun observeOrderList(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<OrderListItem>>

    fun observeOrderDetail(orderId: String): Flow<OrderDetail?>

    suspend fun createOrder(
        actor: AuthenticatedUser,
        input: RepairOrderInput,
    ): Result<Order>

    suspend fun changeStatus(
        actor: AuthenticatedUser,
        orderId: String,
        status: OrderStatus,
        note: String = "",
        closureReason: ClosureReason? = null,
    ): Result<Unit>

    suspend fun recordResult(
        actor: AuthenticatedUser,
        orderId: String,
        input: DiagnosisResultInput,
    ): Result<Unit>

    suspend fun reassign(
        actor: AuthenticatedUser,
        orderId: String,
        assignedToUserId: String?,
    ): Result<Unit>

    suspend fun addInternalNote(
        actor: AuthenticatedUser,
        orderId: String,
        note: String,
    ): Result<Unit>

    suspend fun recordFeedback(
        actor: AuthenticatedUser,
        orderId: String,
        input: FeedbackInput,
    ): Result<Unit>

    suspend fun recordPdfShared(
        actor: AuthenticatedUser,
        orderId: String,
        fileName: String,
    ): Result<Unit>

    suspend fun recordMediaRemoved(
        actor: AuthenticatedUser,
        orderId: String,
        fileName: String,
    ): Result<Unit>

    suspend fun togglePaymentStatus(
        actor: AuthenticatedUser,
        orderId: String,
        isPaid: Boolean,
    ): Result<Unit>

    suspend fun deleteOrder(
        actor: AuthenticatedUser,
        orderId: String,
    ): Result<Unit>

    suspend fun deleteSignature(
        actor: AuthenticatedUser,
        signatureId: String,
    ): Result<Unit>
}
