package com.onlytechtake.repairapp.features.dashboard.neworder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppIconSizes
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDivider
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.AppStepper
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.domain.workspace.isReadyForSubmit
import com.onlytechtake.repairapp.domain.workspace.validationErrors
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.SignatureCaptureForm
import com.onlytechtake.repairapp.features.dashboard.common.WizardNavigationBar
import com.onlytechtake.repairapp.features.workspace.QuickIntakeDraft
import com.onlytechtake.repairapp.features.workspace.QuickIntakeSignatureDraft

@Composable
fun QuickActions(
    viewModel: NewOrderViewModel,
    onOpenConfig: () -> Unit,
    onClose: () -> Unit,
) {
    val draft by viewModel.quickIntakeDraft.collectAsState()
    val customerSuggestions by viewModel.customerSuggestions.collectAsState()
    val customerSearchQuery by viewModel.customerSearchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val anchorMappings by viewModel.anchorMappings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val message by viewModel.message.collectAsState()

    val unnamedPrivate = stringResource(R.string.customer_unnamed_private)
    val unnamedBusiness = stringResource(R.string.customer_unnamed_business)

    fun updateDraft(transform: (QuickIntakeDraft) -> QuickIntakeDraft) {
        viewModel.updateQuickIntakeDraft(transform)
    }

    LaunchedEffect(Unit) {
        if (draft.returnToIntakeAfterCategorySave) {
            updateDraft { it.copy(returnToIntakeAfterCategorySave = false) }
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nextAction = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val doneAction =
        KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
            },
        )
    val stepperState =
        remember(draft, categories, templates, anchorMappings, currentUser) {
            IntakeStepperState(draft, categories, templates, anchorMappings, currentUser)
        }

    val selectedCustomer =
        draft.selectedCustomerId?.let { id -> customerSuggestions.firstOrNull { it.id == id } }
    val selectedCustomerDisplayName =
        selectedCustomer?.displayName(
            unnamedPrivate = unnamedPrivate,
            unnamedBusiness = unnamedBusiness,
        ) ?: draft.selectedCustomerLabel

    val context = LocalContext.current
    val missingRequirements =
        remember(stepperState.intakeInput) { stepperState.intakeInput.validationErrors(context) }

    LaunchedEffect(stepperState.isDirty) {
        viewModel.setFormDirty(stepperState.isDirty)
    }

    val attachmentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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

    val isBusiness = stepperState.isBusiness
    val canSaveNewCustomer = stepperState.canSaveNewCustomer

    fun editStep(step: Int) {
        updateDraft {
            it.copy(currentStep = step)
        }
    }

    fun completeStep(step: Int) {
        updateDraft {
            it.copy(
                completedStep = completedStepAfterAdvance(it.completedStep, step),
                currentStep = currentStepAfterAdvance(step),
            )
        }
    }

    AppScreenContainer(modifier = Modifier.testTag("quick_actions_screen")) {
        message?.let {
            DismissibleAppMessageBanner(
                message = it.text,
                type = if (it.isError) BannerType.Error else BannerType.Info,
                onDismiss = viewModel::clearMessage,
            )
        }

        SectionTitle(
            text = stringResource(R.string.new_order_title),
            tooltip = stringResource(R.string.intake_help),
        )

        AppStepper(
            currentStep = draft.currentStep,
            totalSteps = LAST_INTAKE_STEP,
            completedStep = draft.completedStep,
            onStepClick = { step ->
                if ((step <= draft.completedStep + 1)) {
                    editStep(step)
                }
            },
            modifier = Modifier.padding(vertical = RepairAppSpacing.cardGap),
        ) { RepairAppTestTags.intakeStepperStep(it) }

        if (draft.currentStep == 1) {
            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    Text(
                        text = stringResource(R.string.intake_step_customer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (draft.selectedCustomerId != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(
                                    R.string.intake_selected_customer,
                                    selectedCustomerDisplayName.orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            AppButton(
                                text = stringResource(R.string.intake_change_customer),
                                variant = AppButtonVariant.Ghost,
                                size = AppButtonSize.Compact,
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
                        }
                    } else {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                        ) {
                            AppTextField(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(56.dp),
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
                                imeAction = ImeAction.Search,
                                keyboardActions = doneAction,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                    )
                                },
                                trailingIcon =
                                    if (customerSearchQuery.isNotEmpty()) {
                                        {
                                            AppIconButton(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = stringResource(R.string.content_description_clear),
                                                onClick = { viewModel.setCustomerSearchQuery("") },
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                testTag = RepairAppTestTags.NewOrderCustomerSearchField,
                            )

                            if (!draft.showNewCustomerForm) {
                                Surface(
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
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RepairAppShapes.button,
                                    modifier =
                                        Modifier
                                            .height(56.dp)
                                            .width(56.dp)
                                            .testTag(RepairAppTestTags.NewOrderInsertCustomerButton),
                                    tonalElevation = 0.dp,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.intake_insert_new_customer),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(RepairAppIconSizes.medium),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!draft.showNewCustomerForm && customerSearchQuery.isNotBlank()) {
                        if (customerSuggestions.isNotEmpty()) {
                            customerSuggestions.take(3).forEach { customer ->
                                CustomerSuggestionCard(
                                    customer = customer,
                                    onClick = {
                                        val label =
                                            customer.displayName(
                                                unnamedPrivate = unnamedPrivate,
                                                unnamedBusiness = unnamedBusiness,
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
                    }

                    if (draft.showNewCustomerForm && draft.selectedCustomerId == null) {
                        AppDivider(modifier = Modifier.padding(vertical = RepairAppSpacing.compactGap))
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
                        AppButton(
                            text = stringResource(R.string.customer_save),
                            enabled = canSaveNewCustomer,
                            variant = AppButtonVariant.Primary,
                            size = AppButtonSize.FullWidth,
                            testTag = RepairAppTestTags.NewOrderSaveCustomerButton,
                            onClick = {
                                viewModel.saveCustomer(stepperState.newCustomerInput) { customer ->
                                    val label =
                                        customer.displayName(
                                            unnamedPrivate = unnamedPrivate,
                                            unnamedBusiness = unnamedBusiness,
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
                onCancel = onClose,
            )
        }

        if (draft.currentStep == 2) {
            QuickIntakeCategorySection(
                stepNumber = 2,
                activeCategories = stepperState.activeCategories,
                selectedCategoryId = draft.selectedCategoryId,
                onCategorySelected = { categoryId ->
                    updateDraft {
                        it.copy(
                            selectedCategoryId =
                                selectedCategoryAfterClick(
                                    it.selectedCategoryId,
                                    categoryId,
                                ),
                            completedStep = completedStepAfterCategorySelection(it.completedStep),
                        )
                    }
                },
                onOpenConfig = onOpenConfig,
            )
            WizardNavigationBar(
                onBack = { editStep(1) },
                nextEnabled = draft.selectedCategoryId != null,
                nextTestTag = RepairAppTestTags.NewOrderStep2NextButton,
                onNext = { completeStep(2) },
                onCancel = onClose,
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
            )
            WizardNavigationBar(
                onBack = { editStep(2) },
                nextEnabled = draft.problemSummary.isNotBlank(),
                nextTestTag = RepairAppTestTags.NewOrderStep3NextButton,
                onNext = { completeStep(3) },
                onCancel = onClose,
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
                                stagedMedia =
                                    it.stagedMedia.map { current ->
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
                nextTestTag = RepairAppTestTags.NewOrderStep4NextButton,
                onNext = { completeStep(4) },
                onCancel = onClose,
            )
        }

        if (draft.currentStep == 5) {
            key(draft.intakeSessionId) {
                StandardCard {
                    SignatureCaptureForm(
                        modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                        title = stringResource(R.string.intake_step_privacy),
                        privacyText = stepperState.privacyText,
                        termsText = stepperState.termsText,
                        isBusiness = stepperState.isBusiness,
                        onCaptureChanged = { capture ->
                            if (capture != null || draft.signatureCapture == null) {
                                updateDraft {
                                    it.copy(
                                        signatureCapture =
                                            capture?.let { result ->
                                                QuickIntakeSignatureDraft(
                                                    signerName = result.signerName,
                                                    businessSignerName = result.businessSignerName,
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
                nextTestTag = RepairAppTestTags.NewOrderStep5NextButton,
                onNext = { completeStep(5) },
                onCancel = onClose,
            )
        }

        if (draft.currentStep == SUBMIT_INTAKE_STEP || isIntakeSubmitUnlocked(draft.completedStep)) {
            QuickIntakeSubmitSection(
                stepNumber = 6,
                missingRequirements =
                    if (isIntakeSubmitUnlocked(draft.completedStep)) {
                        missingRequirements
                    } else {
                        listOf(stringResource(R.string.intake_confirm_all_steps))
                    },
                enabled =
                    isIntakeSubmitUnlocked(draft.completedStep) &&
                        stepperState.activeCategories.isNotEmpty() &&
                        stepperState.intakeInput.isReadyForSubmit(context),
                onSubmit = {
                    draft.signatureCapture?.let { capture ->
                        val resolvedSignerName =
                            if (stepperState.isBusiness) {
                                capture.businessSignerName?.takeIf { it.isNotBlank() }
                                    ?: selectedCustomerDisplayName ?: unnamedBusiness
                            } else {
                                selectedCustomerDisplayName ?: unnamedPrivate
                            }

                        viewModel.createIntakeOrder(
                            input = stepperState.intakeInput,
                            stagedMedia = draft.stagedMedia,
                            signerName = resolvedSignerName,
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
