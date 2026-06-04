package com.onlytechtake.repairapp.features.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.isReadyForSubmit
import com.onlytechtake.repairapp.domain.workspace.validationErrors
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

    var intakeSessionId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var selectedCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewCustomerForm by rememberSaveable { mutableStateOf(true) }
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
    val activeCategories = categories.filter { it.active }
    val selectedCategory = activeCategories.firstOrNull { it.id == selectedCategoryId }
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
        existingCustomerId = selectedCustomerId?.takeIf { !showNewCustomerForm },
        newCustomer = newCustomerInput.takeIf { showNewCustomerForm },
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
        selectedCategoryId = null
        showNewCustomerForm = true
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
        viewModel.setCustomerSearchQuery("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("Nuova commessa")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dati cliente", style = MaterialTheme.typography.titleMedium)
                Field(
                    value = uiState.customerSearchQuery,
                    onValueChange = {
                        selectedCustomerId = null
                        showNewCustomerForm = it.isBlank()
                        viewModel.setCustomerSearchQuery(it)
                    },
                    label = "Cerca cliente esistente",
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    keyboardActions = nextAction,
                )
                if (selectedCustomer != null) {
                    Text("Cliente selezionato: ${selectedCustomer.displayName()}")
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            selectedCustomerId = null
                            showNewCustomerForm = true
                            viewModel.setCustomerSearchQuery("")
                        },
                    ) {
                        Text("Cambia cliente")
                    }
                } else if (customerSuggestions.isNotEmpty() && uiState.customerSearchQuery.isNotBlank()) {
                    customerSuggestions.take(5).forEach { customer ->
                        CustomerSuggestionCard(
                            customer = customer,
                            onClick = {
                                selectedCustomerId = customer.id
                                showNewCustomerForm = false
                                viewModel.setCustomerSearchQuery(customer.displayName())
                            },
                        )
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedCustomerId = null
                        showNewCustomerForm = true
                    },
                ) {
                    Text("Inserisci nuovo cliente")
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
                }
            }
        }

        QuickIntakeCategorySection(
            activeCategories = activeCategories,
            selectedCategoryId = selectedCategoryId,
            selectedCategory = selectedCategory,
            onCategorySelected = { categoryId ->
                selectedCategoryId = categoryId
            },
            onOpenConfig = { viewModel.navigateTo(WorkspaceDestination.CONFIG) },
        )

        QuickIntakeProblemSection(
            problemSummary = problemSummary,
            onProblemSummaryChange = { problemSummary = it },
            problemDetails = problemDetails,
            onProblemDetailsChange = { problemDetails = it },
            nextAction = nextAction,
            doneAction = doneAction,
        )

        QuickIntakeMediaSection(
            intakeSessionId = intakeSessionId,
            stagedMedia = stagedMedia,
            showCamera = showCamera,
            onShowCameraChange = { showCamera = it },
            onRemoveMedia = { localPath -> stagedMedia = stagedMedia.filterNot { it.localPath == localPath } },
            onLaunchAttachmentPicker = { attachmentLauncher.launch("*/*") },
            createPhotoFile = { sessionId, _ -> viewModel.createIntakePhotoFile(sessionId) },
            onPhotoCaptured = { file ->
                viewModel.stageCapturedIntakePhoto(file) { staged ->
                    stagedMedia = stagedMedia + staged
                }
            },
            onCameraError = viewModel::reportCameraError,
        )

        key(intakeSessionId) {
            Card(modifier = Modifier.fillMaxWidth()) {
                SignatureCaptureForm(
                    modifier = Modifier.padding(12.dp),
                    title = "Privacy e firma",
                    privacyText = privacyText,
                    termsText = termsText,
                    onCaptureChanged = { signatureCapture = it },
                )
            }
        }

        QuickIntakeSubmitSection(
            missingRequirements = missingRequirements,
            enabled = activeCategories.isNotEmpty() && intakeInput.isReadyForSubmit(),
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
