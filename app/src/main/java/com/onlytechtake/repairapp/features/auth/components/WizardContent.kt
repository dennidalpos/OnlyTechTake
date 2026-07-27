package com.onlytechtake.repairapp.features.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardContent(
    ownerInfo: OwnerInfo,
    onSubmit: (OwnerInfo) -> Unit,
) {
    val context = LocalContext.current
    val showcaseInfo =
        if (BuildConfig.IS_SHOWCASE_MODE_ENABLED) DemoMode.getShowcaseOwnerInfo(context) else null

    val initialName =
        if ((showcaseInfo != null) && ownerInfo.name.isBlank()) {
            showcaseInfo.name
        } else {
            ownerInfo.name
        }
    val initialVat =
        if ((showcaseInfo != null) && ownerInfo.vatNumber.isBlank()) {
            showcaseInfo.vatNumber
        } else {
            ownerInfo.vatNumber
        }
    val initialAddress =
        if ((showcaseInfo != null) && ownerInfo.address.isBlank()) {
            showcaseInfo.address
        } else {
            ownerInfo.address
        }
    val initialEmail =
        if ((showcaseInfo != null) && ownerInfo.email.isBlank()) {
            showcaseInfo.email
        } else {
            ownerInfo.email
        }
    val initialPhone =
        if ((showcaseInfo != null) && ownerInfo.phone.isBlank()) {
            showcaseInfo.phone
        } else {
            ownerInfo.phone
        }

    var name by rememberSaveable { mutableStateOf(initialName) }
    var vatNumber by rememberSaveable { mutableStateOf(initialVat) }
    var address by rememberSaveable { mutableStateOf(initialAddress) }
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.config_hub_owner_title),
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            val compactHeight = maxHeight < 420.dp
            Column(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .widthIn(max = RepairAppSpacing.maxContentWidthMedium)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(
                            horizontal = RepairAppSpacing.screenHorizontal,
                            vertical = if (compactHeight) RepairAppSpacing.compactGap else RepairAppSpacing.screenHorizontal,
                        ),
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) RepairAppSpacing.compactGap else RepairAppSpacing.fieldGap),
            ) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.owner_field_name),
                    required = true,
                    imeAction = ImeAction.Next,
                    testTag = RepairAppTestTags.OwnerFieldName,
                )
                AppTextField(
                    value = vatNumber,
                    onValueChange = { vatNumber = it },
                    label = stringResource(R.string.owner_field_vat),
                    required = true,
                    imeAction = ImeAction.Next,
                    testTag = RepairAppTestTags.OwnerFieldVat,
                )
                AppTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = stringResource(R.string.owner_field_address),
                    required = true,
                    imeAction = ImeAction.Next,
                    testTag = RepairAppTestTags.OwnerFieldAddress,
                )
                AppTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.owner_field_email),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    testTag = RepairAppTestTags.OwnerFieldEmail,
                )
                AppTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.owner_field_phone),
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    testTag = RepairAppTestTags.OwnerFieldPhone,
                )

                Spacer(Modifier.height(RepairAppSpacing.sectionGap))

                AppButton(
                    text = stringResource(R.string.action_confirm),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && vatNumber.isNotBlank() && address.isNotBlank(),
                    onClick = {
                        onSubmit(
                            OwnerInfo(
                                name = name,
                                vatNumber = vatNumber,
                                address = address,
                                email = email,
                                phone = phone,
                            ),
                        )
                    },
                )
            }
        }
    }
}
