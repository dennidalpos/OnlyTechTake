package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.domain.models.OwnerInfo
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun OwnerSettings(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val ownerInfo by viewModel.ownerInfo.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var vatNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(ownerInfo) {
        name = ownerInfo.name
        vatNumber = ownerInfo.vatNumber
        address = ownerInfo.address
        email = ownerInfo.email
        phone = ownerInfo.phone
    }

    AppScreenContainer {
        if (isProcessing) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(RepairAppSpacing.compactGap),
                )
            }
        }

        SectionTitle(
            text = stringResource(R.string.config_hub_owner_title),
            tooltip = stringResource(R.string.config_owner_tooltip),
        )

        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
            ) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.owner_field_name),
                    required = true,
                    testTag = RepairAppTestTags.OwnerFieldName,
                )
                AppTextField(
                    value = vatNumber,
                    onValueChange = { vatNumber = it },
                    label = stringResource(R.string.owner_field_vat),
                    testTag = RepairAppTestTags.OwnerFieldVat,
                )
                AppTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = stringResource(R.string.owner_field_address),
                    testTag = RepairAppTestTags.OwnerFieldAddress,
                )
                AppTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.owner_field_email),
                    testTag = RepairAppTestTags.OwnerFieldEmail,
                )
                AppTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.owner_field_phone),
                    testTag = RepairAppTestTags.OwnerFieldPhone,
                )

                Spacer(Modifier.height(RepairAppSpacing.cardGap))

                AppButton(
                    text = stringResource(R.string.customer_save),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && !isProcessing,
                    onClick = {
                        viewModel.updateOwnerInfo(
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

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        AppButton(
            text = stringResource(R.string.owner_close),
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Default,
            modifier = Modifier.fillMaxWidth(),
            onClick = onBack,
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    name = "Dark Mode",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun OwnerSettingsPreview() {
    MaterialTheme {
        OwnerSettings(
            viewModel =
                androidx.lifecycle.viewmodel.compose
                    .viewModel(),
            onBack = {},
        )
    }
}
