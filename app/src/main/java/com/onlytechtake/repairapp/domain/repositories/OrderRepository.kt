package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.OrderListItem
import com.onlytechtake.repairapp.domain.workspace.RepairOrderInput
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(
        actor: AuthenticatedUser,
        filter: OrderFilter,
    ): Flow<List<RepairOrderEntity>>

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
    ): Result<RepairOrderEntity>

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
