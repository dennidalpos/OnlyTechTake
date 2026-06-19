package com.onlytechtake.repairapp.features.workspace

import android.graphics.Bitmap
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import java.util.UUID

data class WorkspaceMessage(
    val text: String,
    val isError: Boolean,
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
