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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.isReadyForSubmit
import com.onlytechtake.repairapp.domain.workspace.validationErrors
import com.onlytechtake.repairapp.features.dashboard.CustomerSuggestionCard
import com.onlytechtake.repairapp.features.dashboard.common.DEFAULT_PRIVACY_TEXT
import com.onlytechtake.repairapp.features.dashboard.common.DEFAULT_TERMS_TEXT
import com.onlytechtake.repairapp.features.dashboard.common.PRIVACY_TEMPLATE_CODE
import com.onlytechtake.repairapp.features.dashboard.common.TERMS_TEMPLATE_CODE
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StepSectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.dashboard.common.WizardNavigationBar
import com.onlytechtake.repairapp.features.dashboard.common.SignatureCaptureForm
import com.onlytechtake.repairapp.features.dashboard.common.SignatureCaptureResult
import com.onlytechtake.repairapp.features.dashboard.neworder.LAST_INTAKE_STEP
import com.onlytechtake.repairapp.features.dashboard.neworder.SUBMIT_INTAKE_STEP
import com.onlytechtake.repairapp.features.dashboard.neworder.completedStepAfterAdvance
import com.onlytechtake.repairapp.features.dashboard.neworder.completedStepAfterCategorySelection
import com.onlytechtake.repairapp.features.dashboard.neworder.completedStepAfterEdit
import com.onlytechtake.repairapp.features.dashboard.neworder.currentStepAfterAdvance
import com.onlytechtake.repairapp.features.dashboard.neworder.isIntakeSubmitUnlocked
import com.onlytechtake.repairapp.features.dashboard.neworder.selectedCategoryAfterClick
import com.onlytechtake.repairapp.features.dashboard.neworder.QuickIntakeCategorySection
import com.onlytechtake.repairapp.features.dashboard.neworder.QuickIntakeMediaSection
import com.onlytechtake.repairapp.features.dashboard.neworder.QuickIntakeProblemSection
import com.onlytechtake.repairapp.features.dashboard.neworder.QuickIntakeSubmitSection
import com.onlytechtake.repairapp.features.dashboard.neworder.IntakeCustomerFields
import com.onlytechtake.repairapp.features.dashboard.neworder.CustomerSuggestionCard
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import java.util.UUID

@Composable
fun QuickActions(viewModel: WorkspaceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val customerSuggestions by viewModel.customerSuggestions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val unnamedPrivate = stringResource(R.string.customer_unnamed_private)
    val unnamedBusiness = stringResource(R.string.customer_unnamed_business)

    var intakeSessionId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var selectedCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCustomerLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewCustomerForm by rememberSaveable { mutableStateOf(false) }
    var isBusiness by rememberSaveable { mutableStateOf(false) }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var vatNumber by rememberSaveable { mutableStateOf("") }
    var taxCode by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var problemSummary by rememberSaveable { mutableStateOf("") }
    var problemDetails by rememberSaveable { mutableStateOf("") }
    var showCamera by rememberSaveable { mutableStateOf(false) }
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var completedStep by rememberSaveable { mutableIntStateOf(0) }
    var stagedMedia by remember { mutableStateOf<List<StagedOrderMedia>>(emptyList()) }
    var signatureCapture by remember { mutableStateOf<SignatureCaptureResult?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nextAction = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val doneAction = KeyboardActions(
        onDone = {
            keyboardController?.hide()
            focusManager.clearFocus()
        },
    )
    val selectedCustomer = selectedCustomerId?.let { id -> customerSuggestions.firstOrNull { it.id == id } }
    val selectedCustomerDisplayName = selectedCustomer?.displayName(
        unnamedPrivate = unnamedPrivate,
        unnamedBusiness = unnamedBusiness
    ) ?: selectedCustomerLabel
    val activeCategories = categories.filter { it.active }
    val newCustomerInput = CustomerInput(
        type = if (isBusiness) CustomerType.BUSINESS else CustomerType.PRIVATE,
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        phone = phone,
        email = email,
        vatNumber = vatNumber,
        taxCode = taxCode,
        address = address,
        notes = notes,
    )
    val intakeInput = IntakeOrderInput(
        existingCustomerId = selectedCustomerId,
        newCustomer = null,
        problemSummary = problemSummary,
        problemDetails = problemDetails,
        categoryId = selectedCategoryId,
        assignedToUserId = currentUser?.id,
        stagedMediaCount = stagedMedia.size,
        hasPrivacySignature = signatureCapture != null,
    )
    val privacyText = templates.firstOrNull { it.code == PRIVACY_TEMPLATE_CODE && it.active }?.body ?: DEFAULT_PRIVACY_TEXT
    val termsText = templates.firstOrNull { it.code == TERMS_TEMPLATE_CODE && it.active }?.body ?: DEFAULT_TERMS_TEXT
    val missingRequirements = intakeInput.validationErrors()

    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.importIntakeAttachment(intakeSessionId, it) { staged ->
                stagedMedia = stagedMedia + staged
            }
        }
    }

    fun resetForm() {
        intakeSessionId = UUID.randomUUID().toString()
        selectedCustomerId = null
        selectedCustomerLabel = null
        selectedCategoryId = null
        showNewCustomerForm = false
        isBusiness = false
        firstName = ""
        lastName = ""
        companyName = ""
        phone = ""
        email = ""
        vatNumber = ""
        taxCode = ""
        address = ""
        notes = ""
        problemSummary = ""
        problemDetails = ""
        stagedMedia = emptyList()
        signatureCapture = null
        showCamera = false
        currentStep = 1
        completedStep = 0
        viewModel.setCustomerSearchQuery("")
    }

    fun clearNewCustomerFields() {
        isBusiness = false
        firstName = ""
        lastName = ""
        companyName = ""
        phone = ""
        email = ""
        vatNumber = ""
        taxCode = ""
        address = ""
        notes = ""
    }

    val canSaveNewCustomer = (phone.isNotBlank() || email.isNotBlank()) &&
        if (isBusiness) companyName.isNotBlank() else firstName.isNotBlank() && lastName.isNotBlank()

    fun editStep(step: Int) {
        currentStep = step
        completedStep = completedStepAfterEdit(completedStep, step)
    }

    fun completeStep(step: Int) {
        completedStep = completedStepAfterAdvance(completedStep, step)
        currentStep = currentStepAfterAdvance(step)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        SectionTitle(stringResource(R.string.new_order_title))

        StepperIndicator(
            currentStep = currentStep,
            completedStep = completedStep,
            onStepClick = { step ->
                if (step <= completedStep + 1) {
                    editStep(step)
                }
            }
        )

        if (currentStep == 1) {
            Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(DashboardSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
            ) {
                StepSectionTitle(1, stringResource(R.string.intake_step_customer))
                Field(
                    value = uiState.customerSearchQuery,
                    onValueChange = {
                        selectedCustomerId = null
                        selectedCustomerLabel = null
                        showNewCustomerForm = false
                        viewModel.setCustomerSearchQuery(it)
                    },
                    label = stringResource(R.string.intake_search_customer),
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    keyboardActions = nextAction,
                    testTag = RepairAppTestTags.NewOrderCustomerSearchField,
                )
                if (selectedCustomerId != null) {
                    Text(stringResource(R.string.intake_selected_customer, selectedCustomerDisplayName.orEmpty()))
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.intake_change_customer),
                        tone = ButtonTone.NEUTRAL,
                        onClick = {
                            selectedCustomerId = null
                            selectedCustomerLabel = null
                            showNewCustomerForm = false
                            viewModel.setCustomerSearchQuery("")
                        },
                    )
                } else if (customerSuggestions.isNotEmpty()) {
                    customerSuggestions.forEach { customer ->
                        CustomerSuggestionCard(
                            customer = customer,
                            onClick = {
                                selectedCustomerId = customer.id
                                selectedCustomerLabel = customer.displayName(
                                    unnamedPrivate = unnamedPrivate,
                                    unnamedBusiness = unnamedBusiness
                                )
                                showNewCustomerForm = false
                                viewModel.setCustomerSearchQuery(selectedCustomerLabel.orEmpty())
                            },
                        )
                    }
                }
                if (selectedCustomerId == null && !showNewCustomerForm) {
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.intake_insert_new_customer),
                        tone = ButtonTone.NEUTRAL,
                        testTag = RepairAppTestTags.NewOrderInsertCustomerButton,
                        onClick = {
                            selectedCustomerId = null
                            selectedCustomerLabel = null
                            showNewCustomerForm = true
                            viewModel.setCustomerSearchQuery("")
                        },
                    )
                }
                if (showNewCustomerForm) {
                    IntakeCustomerFields(
                        isBusiness = isBusiness,
                        onBusinessChange = { isBusiness = it },
                        firstName = firstName,
                        onFirstNameChange = { firstName = it },
                        lastName = lastName,
                        onLastNameChange = { lastName = it },
                        companyName = companyName,
                        onCompanyNameChange = { companyName = it },
                        vatNumber = vatNumber,
                        onVatNumberChange = { vatNumber = it },
                        taxCode = taxCode,
                        onTaxCodeChange = { taxCode = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        email = email,
                        onEmailChange = { email = it },
                        address = address,
                        onAddressChange = { address = it },
                        notes = notes,
                        onNotesChange = { notes = it },
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
                                selectedCustomerId = customer.id
                                selectedCustomerLabel = customer.displayName(
                                    unnamedPrivate = unnamedPrivate,
                                    unnamedBusiness = unnamedBusiness
                                )
                                showNewCustomerForm = false
                                clearNewCustomerFields()
                                viewModel.setCustomerSearchQuery(selectedCustomerLabel.orEmpty())
                            }
                        },
                    )
                }
            }
        }
            WizardNavigationBar(
                nextEnabled = selectedCustomerId != null,
                nextTestTag = RepairAppTestTags.NewOrderStep1NextButton,
                onNext = { completeStep(1) },
            )
        }

        if (currentStep == 2) {
            QuickIntakeCategorySection(
                stepNumber = 2,
                activeCategories = activeCategories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { categoryId ->
                    selectedCategoryId = selectedCategoryAfterClick(selectedCategoryId, categoryId)
                    completedStep = completedStepAfterCategorySelection(completedStep)
                },
                onOpenConfig = { viewModel.navigateTo(WorkspaceDestination.CONFIG) },
            )
            WizardNavigationBar(
                onBack = { editStep(1) },
                nextEnabled = selectedCategoryId != null,
                nextTestTag = RepairAppTestTags.NewOrderStep2NextButton,
                onNext = { completeStep(2) },
            )
        }

        if (currentStep == 3) {
            QuickIntakeProblemSection(
                stepNumber = 3,
                problemSummary = problemSummary,
                onProblemSummaryChange = { problemSummary = it },
                problemDetails = problemDetails,
                onProblemDetailsChange = { problemDetails = it },
                nextAction = nextAction,
                doneAction = doneAction,
            )
            WizardNavigationBar(
                onBack = { editStep(2) },
                nextEnabled = problemSummary.isNotBlank(),
                onNext = { completeStep(3) },
            )
        }

        if (currentStep == 4) {
            QuickIntakeMediaSection(
                stepNumber = 4,
                intakeSessionId = intakeSessionId,
                stagedMedia = stagedMedia,
                showCamera = showCamera,
                onShowCameraChange = { showCamera = it },
                onRemoveMedia = { localPath ->
                    viewModel.deleteStagedMedia(localPath)
                    stagedMedia = stagedMedia.filterNot { it.localPath == localPath }
                },
                onReplaceMedia = { media, bitmap ->
                    viewModel.replaceStagedImage(media, bitmap) { replaced ->
                        stagedMedia = stagedMedia.map { current ->
                            if (current.localPath == replaced.localPath) replaced else current
                        }
                    }
                },
                onLaunchAttachmentPicker = { attachmentLauncher.launch("*/*") },
                createPhotoFile = { sessionId, _ -> viewModel.createIntakePhotoFile(sessionId) },
                onPhotoCaptured = { file ->
                    viewModel.stageCapturedIntakePhoto(file) { staged ->
                        stagedMedia = stagedMedia + staged
                    }
                },
                onCameraError = viewModel::reportCameraError,
            )
            WizardNavigationBar(
                onBack = { editStep(3) },
                nextEnabled = stagedMedia.isNotEmpty(),
                onNext = { completeStep(4) },
            )
        }

        if (currentStep == 5) {
            key(intakeSessionId) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SignatureCaptureForm(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        title = "5. ${stringResource(R.string.intake_step_privacy)}",
                        privacyText = privacyText,
                        termsText = termsText,
                        onCaptureChanged = { signatureCapture = it },
                    )
                }
            }
            WizardNavigationBar(
                onBack = { editStep(4) },
                nextEnabled = signatureCapture != null,
                onNext = { completeStep(5) },
            )
        }

        if (currentStep == SUBMIT_INTAKE_STEP || isIntakeSubmitUnlocked(completedStep)) {
            QuickIntakeSubmitSection(
                stepNumber = 6,
                missingRequirements = if (isIntakeSubmitUnlocked(completedStep)) {
                    missingRequirements
                } else {
                    listOf(stringResource(R.string.intake_confirm_all_steps))
                },
                enabled = isIntakeSubmitUnlocked(completedStep) &&
                    activeCategories.isNotEmpty() &&
                    intakeInput.isReadyForSubmit(),
                onSubmit = {
                    signatureCapture?.let { capture ->
                        viewModel.createIntakeOrder(
                            input = intakeInput,
                            stagedMedia = stagedMedia,
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

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> Color(0xFF237A3B)
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
                androidx.compose.foundation.Canvas(modifier = Modifier.weight(1f).height(2.dp)) {
                    drawRect(color = if (i <= completedStep) Color(0xFF237A3B) else Color.LightGray)
                }
            }
        }
    }
}
