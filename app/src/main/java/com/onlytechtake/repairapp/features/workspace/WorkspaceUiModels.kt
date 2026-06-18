package com.onlytechtake.repairapp.features.workspace

import android.content.Intent
import android.graphics.Bitmap
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.data.services.StorageStats
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import java.util.UUID

data class WorkspaceMessage(
    val text: String,
    val isError: Boolean,
)

data class WorkspaceUiState(
    val message: WorkspaceMessage? = null,
    val destination: WorkspaceDestination = WorkspaceDestination.HOME,
    val selectedOrderId: String? = null,
    val orderFilter: OrderFilter = OrderFilter(),
    val customerSearchQuery: String = "",
    val preparedMessage: PreparedMessage? = null,
    val preparedMessageBody: String = "",
    val isGeneratingStatusPdf: Boolean = false,
    val statusPdfError: String? = null,
    val showPdfSentConfirmation: Boolean = false,
    val pendingShareIntent: Intent? = null,
    val lastGeneratedDocumentId: String? = null,
    val storageStats: StorageStats? = null,
)

data class QuickIntakeSignatureDraft(
    val signerName: String,
    val businessSignerName: String? = null,
    val privacyAccepted: Boolean,
    val termsAccepted: Boolean,
    val bitmap: Bitmap,
)

data class QuickIntakeDraft(
    val intakeSessionId: String = UUID.randomUUID().toString(),
    val selectedCustomerId: String? = null,
    val selectedCustomerLabel: String? = null,
    val selectedCategoryId: String? = null,
    val showNewCustomerForm: Boolean = false,
    val customerType: CustomerType = CustomerType.PRIVATE,
    val firstName: String = "",
    val lastName: String = "",
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val vatNumber: String = "",
    val taxCode: String = "",
    val address: String = "",
    val notes: String = "",
    val problemSummary: String = "",
    val problemDetails: String = "",
    val showCamera: Boolean = false,
    val currentStep: Int = 1,
    val completedStep: Int = 0,
    val stagedMedia: List<StagedOrderMedia> = emptyList(),
    val signatureCapture: QuickIntakeSignatureDraft? = null,
    val returnToIntakeAfterCategorySave: Boolean = false,
)

fun QuickIntakeDraft.withCategoryCreatedFromIntake(categoryId: String): QuickIntakeDraft =
    copy(
        selectedCategoryId = categoryId,
        currentStep = 2,
        completedStep = minOf(completedStep, 1),
        returnToIntakeAfterCategorySave = false,
    )

fun WorkspaceUiState.withoutPreparedMessage(): WorkspaceUiState = copy(preparedMessage = null, preparedMessageBody = "")

enum class WorkspaceDestination {
    HOME,
    CUSTOMERS,
    ORDERS,
    NEW_ORDER,
    ORDER_DETAIL,
    SETTINGS,
    SETTINGS_CATEGORIES,
    SETTINGS_TEMPLATES,
    SETTINGS_WHATSAPP,
    SETTINGS_BACKUP,
    SETTINGS_STORAGE,
    ADMIN,
}

enum class ButtonTone {
    PRIMARY,
    NEUTRAL,
    SUCCESS,
    INFO,
    WARNING,
    DANGER,
    BRAND_WHATSAPP,
}
