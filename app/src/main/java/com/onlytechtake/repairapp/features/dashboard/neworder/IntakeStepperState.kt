package com.onlytechtake.repairapp.features.dashboard.neworder

import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.Category
import com.onlytechtake.repairapp.domain.models.CustomerInput
import com.onlytechtake.repairapp.domain.models.CustomerType
import com.onlytechtake.repairapp.domain.models.IntakeOrderInput
import com.onlytechtake.repairapp.domain.models.Template
import com.onlytechtake.repairapp.features.workspace.QuickIntakeDraft

data class IntakeStepperState(
    private val draft: QuickIntakeDraft,
    val categories: List<Category>,
    val templates: List<Template>,
    private val anchorMappings: Map<TemplateAnchor, String>,
    val currentUser: AuthenticatedUser?,
    private val privacyFallback: String,
    private val termsFallback: String,
) {
    val activeCategories = categories.filter { it.active }

    val isBusiness = draft.customerType == CustomerType.BUSINESS

    val canSaveNewCustomer =
        (draft.phone.isNotBlank() || draft.email.isNotBlank()) &&
            if (isBusiness) draft.companyName.isNotBlank() else draft.firstName.isNotBlank() && draft.lastName.isNotBlank()

    val newCustomerInput =
        CustomerInput(
            type = draft.customerType,
            firstName = draft.firstName,
            lastName = draft.lastName,
            companyName = draft.companyName,
            phone = draft.phone,
            email = draft.email,
            vatNumber = draft.vatNumber,
            taxCode = draft.taxCode,
            address = draft.address,
            notes = draft.notes,
        )

    val intakeInput =
        IntakeOrderInput(
            existingCustomerId = draft.selectedCustomerId,
            newCustomer = null,
            problemSummary = draft.problemSummary,
            problemDetails = draft.problemDetails,
            categoryId = draft.selectedCategoryId,
            assignedToUserId = currentUser?.id,
            stagedMediaCount = draft.stagedMedia.size,
            hasPrivacySignature = draft.signatureCapture != null,
        )

    val isDirty: Boolean =
        (draft.selectedCustomerId != null) ||
            (draft.selectedCategoryId != null) ||
            draft.problemSummary.isNotBlank() ||
            draft.problemDetails.isNotBlank() ||
            draft.stagedMedia.isNotEmpty() ||
            (draft.signatureCapture != null)

    val privacyText: String by lazy {
        val code =
            anchorMappings[TemplateAnchor.PRIVACY_TEXT] ?: TemplateAnchor.PRIVACY_TEXT.defaultCode
        templates.firstOrNull { (it.code == code) && it.active }?.body
            ?: privacyFallback
    }

    val termsText: String by lazy {
        val code =
            anchorMappings[TemplateAnchor.TERMS_TEXT] ?: TemplateAnchor.TERMS_TEXT.defaultCode
        templates.firstOrNull { (it.code == code) && it.active }?.body
            ?: termsFallback
    }
}
