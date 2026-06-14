package com.onlytechtake.repairapp.features.dashboard.neworder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.isReadyForSubmit
import com.onlytechtake.repairapp.domain.workspace.validationErrors
import com.onlytechtake.repairapp.features.dashboard.common.DEFAULT_PRIVACY_TEXT
import com.onlytechtake.repairapp.features.dashboard.common.DEFAULT_TERMS_TEXT
import com.onlytechtake.repairapp.features.dashboard.common.PRIVACY_TEMPLATE_CODE
import com.onlytechtake.repairapp.features.dashboard.common.TERMS_TEMPLATE_CODE
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.StepSectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.dashboard.common.WizardNavigationBar
import com.onlytechtake.repairapp.features.dashboard.common.SignatureCaptureForm
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.QuickIntakeDraft
import com.onlytechtake.repairapp.features.workspace.QuickIntakeSignatureDraft

@Composable
fun QuickActions(
    viewModel: NewOrderViewModel,
    onOpenConfig: () -> Unit,
) {
    val draft by viewModel.quickIntakeDraft.collectAsState()
    val customerSuggestions by viewModel.customerSuggestions.collectAsState()
    val customerSearchQuery by viewModel.customerSearchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val unnamedPrivate = stringResource(R.string.customer_unnamed_private)
    val unnamedBusiness = stringResource(R.string.customer_unnamed_business)

    fun updateDraft(transform: (QuickIntakeDraft) -> QuickIntakeDraft) {
        viewModel.updateQuickIntakeDraft(transform)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (draft.returnToIntakeAfterCategorySave) {
            updateDraft { it.copy(returnToIntakeAfterCategorySave = false) }
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nextAction = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val doneAction = KeyboardActions(
        onDone = {
            keyboardController?.hide()
            focusManager.clearFocus()
        },
    )
    val selectedCustomer = draft.selectedCustomerId?.let { id -> customerSuggestions.firstOrNull { it.id == id } }
    val selectedCustomerDisplayName = selectedCustomer?.displayName(
        unnamedPrivate = unnamedPrivate,
        unnamedBusiness = unnamedBusiness,
    ) ?: draft.selectedCustomerLabel
    val activeCategories = categories.filter { it.active }
    val newCustomerInput = CustomerInput(
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
    val intakeInput = IntakeOrderInput(
        existingCustomerId = draft.selectedCustomerId,
        newCustomer = null,
        problemSummary = draft.problemSummary,
        problemDetails = draft.problemDetails,
        categoryId = draft.selectedCategoryId,
        assignedToUserId = currentUser?.id,
        stagedMediaCount = draft.stagedMedia.size,
        hasPrivacySignature = draft.signatureCapture != null,
    )
    val privacyText = templates.firstOrNull { (it.code == PRIVACY_TEMPLATE_CODE) && it.active }?.body ?: DEFAULT_PRIVACY_TEXT
    val termsText = templates.firstOrNull { (it.code == TERMS_TEMPLATE_CODE) && it.active }?.body ?: DEFAULT_TERMS_TEXT
    val missingRequirements = intakeInput.validationErrors()

    androidx.compose.runtime.LaunchedEffect(
        draft.selectedCustomerId,
        draft.selectedCategoryId,
        draft.problemSummary,
        draft.problemDetails,
        draft.stagedMedia,
        draft.signatureCapture,
    ) {
        val isDirty = draft.selectedCustomerId != null ||
                draft.selectedCategoryId != null ||
                draft.problemSummary.isNotBlank() ||
                draft.problemDetails.isNotBlank() ||
                draft.stagedMedia.isNotEmpty() ||
                draft.signatureCapture != null
        viewModel.setFormDirty(isDirty)
    }

    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.importIntakeAttachment(draft.intakeSessionId, it) { staged ->
                updateDraft { current -> current.copy(stagedMedia = current.stagedMedia + staged) }
            }
        }
    }

    fun resetForm() {
        viewModel.resetQuickIntakeDraft()
    }

    fun clearNewCustomerFields() {
        updateDraft {
            it.copy(
                customerType = CustomerType.PRIVATE,
                firstName = "",
                lastName = "",
                companyName = "",
                phone = "",
                email = "",
                vatNumber = "",
                taxCode = "",
                address = "",
                notes = "",
            )
        }
    }

    val isBusiness = draft.customerType == CustomerType.BUSINESS
    val canSaveNewCustomer = (draft.phone.isNotBlank() || draft.email.isNotBlank()) &&
        if (isBusiness) draft.companyName.isNotBlank() else draft.firstName.isNotBlank() && draft.lastName.isNotBlank()

    fun editStep(step: Int) {
        updateDraft { it.copy(currentStep = step, completedStep = completedStepAfterEdit(it.completedStep, step)) }
    }

    fun completeStep(step: Int) {
        updateDraft {
            it.copy(
                completedStep = completedStepAfterAdvance(it.completedStep, step),
                currentStep = currentStepAfterAdvance(step),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quick_actions_screen")
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        SectionTitle(stringResource(R.string.new_order_title))

        StepperIndicator(
            currentStep = draft.currentStep,
            completedStep = draft.completedStep,
        ) { step ->
            if (step <= draft.completedStep + 1) {
                editStep(step)
            }
        }

        if (draft.currentStep == 1) {
            StandardCard(highlight = true) {
            Column(
                modifier = Modifier.padding(DashboardSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
            ) {
                StepSectionTitle(1, stringResource(R.string.intake_step_customer))
                Field(
                    value = customerSearchQuery,
                    onValueChange = { query ->
                        updateDraft { draftState ->
                            draftState.copy(
                                selectedCustomerId = null,
                                selectedCustomerLabel = null,
                                showNewCustomerForm = false,
                            )
                        }
                        viewModel.setCustomerSearchQuery(query)
                    },
                    label = stringResource(R.string.intake_search_customer),
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    keyboardActions = nextAction,
                    testTag = RepairAppTestTags.NewOrderCustomerSearchField,
                )
                if (draft.selectedCustomerId != null) {
                    Text(
                        stringResource(R.string.intake_selected_customer, selectedCustomerDisplayName.orEmpty()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.intake_change_customer),
                        tone = ButtonTone.NEUTRAL,
                        onClick = {
                            updateDraft {
                                it.copy(
                                    selectedCustomerId = null,
                                    selectedCustomerLabel = null,
                                    showNewCustomerForm = false,
                                )
                            }
                            viewModel.setCustomerSearchQuery("")
                        },
                    )
                } else if (customerSuggestions.isNotEmpty()) {
                    customerSuggestions.forEach { customer ->
                        CustomerSuggestionCard(
                            customer = customer,
                            onClick = {
                                val label = customer.displayName(
                                    unnamedPrivate = unnamedPrivate,
                                    unnamedBusiness = unnamedBusiness
                                )
                                updateDraft {
                                    it.copy(
                                        selectedCustomerId = customer.id,
                                        selectedCustomerLabel = label,
                                        showNewCustomerForm = false,
                                    )
                                }
                                viewModel.setCustomerSearchQuery(label)
                            },
                        )
                    }
                }
                if (draft.selectedCustomerId == null && !draft.showNewCustomerForm) {
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.intake_insert_new_customer),
                        tone = ButtonTone.NEUTRAL,
                        testTag = RepairAppTestTags.NewOrderInsertCustomerButton,
                        onClick = {
                            updateDraft {
                                it.copy(
                                    selectedCustomerId = null,
                                    selectedCustomerLabel = null,
                                    showNewCustomerForm = true,
                                )
                            }
                            viewModel.setCustomerSearchQuery("")
                        },
                    )
                }
                if (draft.showNewCustomerForm) {
                    IntakeCustomerFields(
                        isBusiness = isBusiness,
                        onBusinessChange = { business ->
                            updateDraft {
                                it.copy(customerType = if (business) CustomerType.BUSINESS else CustomerType.PRIVATE)
                            }
                        },
                        firstName = draft.firstName,
                        onFirstNameChange = { value -> updateDraft { it.copy(firstName = value) } },
                        lastName = draft.lastName,
                        onLastNameChange = { value -> updateDraft { it.copy(lastName = value) } },
                        companyName = draft.companyName,
                        onCompanyNameChange = { value -> updateDraft { it.copy(companyName = value) } },
                        vatNumber = draft.vatNumber,
                        onVatNumberChange = { value -> updateDraft { it.copy(vatNumber = value) } },
                        taxCode = draft.taxCode,
                        onTaxCodeChange = { value -> updateDraft { it.copy(taxCode = value) } },
                        phone = draft.phone,
                        onPhoneChange = { value -> updateDraft { it.copy(phone = value) } },
                        email = draft.email,
                        onEmailChange = { value -> updateDraft { it.copy(email = value) } },
                        address = draft.address,
                        onAddressChange = { value -> updateDraft { it.copy(address = value) } },
                        notes = draft.notes,
                        onNotesChange = { value -> updateDraft { it.copy(notes = value) } },
                        nextAction = nextAction,
                        doneAction = doneAction,
                    )
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.customer_save),
                        enabled = canSaveNewCustomer,
                        testTag = RepairAppTestTags.NewOrderSaveCustomerButton,
                        onClick = {
                            viewModel.saveCustomer(newCustomerInput) { customer ->
                                val label = customer.displayName(
                                    unnamedPrivate = unnamedPrivate,
                                    unnamedBusiness = unnamedBusiness
                                )
                                updateDraft {
                                    it.copy(
                                        selectedCustomerId = customer.id,
                                        selectedCustomerLabel = label,
                                        showNewCustomerForm = false,
                                    )
                                }
                                clearNewCustomerFields()
                                viewModel.setCustomerSearchQuery(label)
                            }
                        },
                    )
                }
            }
        }
            WizardNavigationBar(
                nextEnabled = draft.selectedCustomerId != null,
                nextTestTag = RepairAppTestTags.NewOrderStep1NextButton,
                onNext = { completeStep(1) },
            )
        }

        if (draft.currentStep == 2) {
            QuickIntakeCategorySection(
                stepNumber = 2,
                activeCategories = activeCategories,
                selectedCategoryId = draft.selectedCategoryId,
                onCategorySelected = { categoryId ->
                    updateDraft {
                        it.copy(
                            selectedCategoryId = selectedCategoryAfterClick(it.selectedCategoryId, categoryId),
                            completedStep = completedStepAfterCategorySelection(it.completedStep),
                        )
                    }
                },
                onOpenConfig = onOpenConfig,
                highlight = true,
            )
            WizardNavigationBar(
                onBack = { editStep(1) },
                nextEnabled = draft.selectedCategoryId != null,
                nextTestTag = RepairAppTestTags.NewOrderStep2NextButton,
                onNext = { completeStep(2) },
            )
        }

        if (draft.currentStep == 3) {
            QuickIntakeProblemSection(
                stepNumber = 3,
                problemSummary = draft.problemSummary,
                onProblemSummaryChange = { value -> updateDraft { it.copy(problemSummary = value) } },
                problemDetails = draft.problemDetails,
                onProblemDetailsChange = { value -> updateDraft { it.copy(problemDetails = value) } },
                nextAction = nextAction,
                doneAction = doneAction,
                highlight = true,
            )
            WizardNavigationBar(
                onBack = { editStep(2) },
                nextEnabled = draft.problemSummary.isNotBlank(),
                onNext = { completeStep(3) },
            )
        }

        if (draft.currentStep == 4) {
            QuickIntakeMediaSection(
                stepNumber = 4,
                intakeSessionId = draft.intakeSessionId,
                stagedMedia = draft.stagedMedia,
                showCamera = draft.showCamera,
                onShowCameraChange = { value -> updateDraft { it.copy(showCamera = value) } },
                onRemoveMedia = { localPath ->
                    viewModel.deleteStagedMedia(localPath)
                    updateDraft { it.copy(stagedMedia = it.stagedMedia.filterNot { media -> media.localPath == localPath }) }
                },
                onReplaceMedia = { media, bitmap ->
                    viewModel.replaceStagedImage(media, bitmap) { replaced ->
                        updateDraft {
                            it.copy(
                                stagedMedia = it.stagedMedia.map { current ->
                                    if (current.localPath == replaced.localPath) replaced else current
                                },
                            )
                        }
                    }
                },
                onLaunchAttachmentPicker = { attachmentLauncher.launch("*/*") },
                createPhotoFile = { sessionId, _ -> viewModel.createIntakePhotoFile(sessionId) },
                onPhotoCaptured = { file ->
                    viewModel.stageCapturedIntakePhoto(file) { staged ->
                        updateDraft { it.copy(stagedMedia = it.stagedMedia + staged) }
                    }
                },
                onCameraError = viewModel::reportCameraError,
            )
            WizardNavigationBar(
                onBack = { editStep(3) },
                nextEnabled = draft.stagedMedia.isNotEmpty(),
                onNext = { completeStep(4) },
            )
        }

        if (draft.currentStep == 5) {
            key(draft.intakeSessionId) {
                StandardCard {
                    SignatureCaptureForm(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        title = "5. ${stringResource(R.string.intake_step_privacy)}",
                        privacyText = privacyText,
                        termsText = termsText,
                        onCaptureChanged = { capture ->
                            if (capture != null || draft.signatureCapture == null) {
                                updateDraft {
                                    it.copy(
                                        signatureCapture = capture?.let { result ->
                                            QuickIntakeSignatureDraft(
                                                signerName = result.signerName,
                                                privacyAccepted = result.privacyAccepted,
                                                termsAccepted = result.termsAccepted,
                                                bitmap = result.bitmap,
                                            )
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
            WizardNavigationBar(
                onBack = { editStep(4) },
                nextEnabled = draft.signatureCapture != null,
                onNext = { completeStep(5) },
            )
        }

        if (draft.currentStep == SUBMIT_INTAKE_STEP || isIntakeSubmitUnlocked(draft.completedStep)) {
            QuickIntakeSubmitSection(
                stepNumber = 6,
                missingRequirements = if (isIntakeSubmitUnlocked(draft.completedStep)) {
                    missingRequirements
                } else {
                    listOf(stringResource(R.string.intake_confirm_all_steps))
                },
                enabled = isIntakeSubmitUnlocked(draft.completedStep) &&
                    activeCategories.isNotEmpty() &&
                    intakeInput.isReadyForSubmit(),
                onSubmit = {
                    draft.signatureCapture?.let { capture ->
                        viewModel.createIntakeOrder(
                            input = intakeInput,
                            stagedMedia = draft.stagedMedia,
                            signerName = capture.signerName,
                            privacyAccepted = capture.privacyAccepted,
                            termsAccepted = capture.termsAccepted,
                            signatureBitmap = capture.bitmap,
                            onCreated = ::resetForm,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun StepperIndicator(
    currentStep: Int,
    completedStep: Int,
    onStepClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..LAST_INTAKE_STEP) {
            val isCompleted = i <= completedStep
            val isCurrent = i == currentStep
            val completedColor = Color(0xFF2E7D32)

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> completedColor
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    )
                    .clickable(enabled = isCompleted || i == 1) { onStepClick(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = i.toString(),
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                        isCompleted -> Color.White
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (i < LAST_INTAKE_STEP) {
                androidx.compose.foundation.Canvas(modifier = Modifier.weight(1f).height(4.dp)) {
                    drawRect(color = if (i <= completedStep) completedColor else Color.LightGray)
                }
            }
        }
    }
}
