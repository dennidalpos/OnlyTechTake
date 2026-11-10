package com.onlytechtake.repairapp.features.dashboard.neworder

import com.onlytechtake.repairapp.domain.models.CustomerType
import com.onlytechtake.repairapp.features.workspace.QuickIntakeDraft

const val LAST_INTAKE_STEP = 5
const val SUBMIT_INTAKE_STEP = 6

fun completedStepAfterEdit(
    completedStep: Int,
    step: Int,
): Int = minOf(completedStep, step - 1).coerceAtLeast(0)

fun completedStepAfterAdvance(
    completedStep: Int,
    step: Int,
): Int = maxOf(completedStep, step).coerceIn(0, LAST_INTAKE_STEP)

fun currentStepAfterAdvance(step: Int): Int = (step + 1).coerceAtMost(SUBMIT_INTAKE_STEP)

fun isIntakeSubmitUnlocked(completedStep: Int): Boolean = completedStep >= LAST_INTAKE_STEP

fun selectedCategoryAfterClick(
    currentCategoryId: String?,
    clickedCategoryId: String,
): String? = if (currentCategoryId == clickedCategoryId) null else clickedCategoryId

fun completedStepAfterCategorySelection(completedStep: Int): Int = completedStepAfterEdit(completedStep, 2)

fun calculateIsDirty(draft: QuickIntakeDraft): Boolean =
    (draft.selectedCustomerId != null) ||
        (draft.selectedCategoryId != null) ||
        draft.problemSummary.isNotBlank() ||
        draft.problemDetails.isNotBlank() ||
        draft.stagedMedia.isNotEmpty() ||
        (draft.signatureCapture != null)

fun calculateCanSaveNewCustomer(
    customerType: CustomerType,
    firstName: String,
    lastName: String,
    companyName: String,
    phone: String,
    email: String,
): Boolean {
    val isBusiness = customerType == CustomerType.BUSINESS
    val hasContact = phone.isNotBlank() || email.isNotBlank()
    val hasName =
        if (isBusiness) companyName.isNotBlank() else firstName.isNotBlank() && lastName.isNotBlank()
    return hasContact && hasName
}
