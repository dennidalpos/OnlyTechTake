package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.features.dashboard.common.AppEmptyState
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass

@Composable
fun SettingsPanel(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onBackupRestored: () -> Unit,
    onNavigate: (String) -> Unit,
    initialSection: String? = null,
    fromIntake: Boolean = false,
) {
    LaunchedEffect(Unit) {
        viewModel.initializeTemplatesIfNeeded()
    }
    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    var currentSection by remember(initialSection) { mutableStateOf(initialSection) }
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestConfirm(
        title: String,
        text: String,
        action: () -> Unit,
    ) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    val onBackToHub = {
        if (isWide) {
            currentSection = null
        } else {
            onNavigate(
                com.onlytechtake.repairapp.features.dashboard.DashboardRoutes
                    .settings(),
            )
        }
    }

    if (isWide) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                SettingsHub(
                    isAdmin = isAdmin,
                ) { route ->
                    val section =
                        if (route.contains("section=")) {
                            route.substringAfter("section=")
                        } else {
                            null
                        }
                    if (section != null) {
                        currentSection = section
                    } else {
                        onNavigate(route)
                    }
                }
            }
            VerticalDivider()
            Box(Modifier.weight(2f)) {
                SettingsSectionContent(
                    section = currentSection,
                    viewModel = viewModel,
                    isAdmin = isAdmin,
                    fromIntake = fromIntake,
                    onBackupRestored = onBackupRestored,
                    requestConfirm = ::requestConfirm,
                    onNavigate = onNavigate,
                    onBack = onBackToHub,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            SettingsSectionContent(
                section = initialSection,
                viewModel = viewModel,
                isAdmin = isAdmin,
                fromIntake = fromIntake,
                onBackupRestored = onBackupRestored,
                requestConfirm = ::requestConfirm,
                onNavigate = onNavigate,
                onBack = onBackToHub,
            )
        }
    }

    confirmTitle?.let { title ->
        AppDialog(
            onDismissRequest = {
                confirmTitle = null
                confirmAction = null
            },
            title = title,
            text = confirmText,
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        confirmAction?.invoke()
                        confirmTitle = null
                        confirmAction = null
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.GenericConfirmButton,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = {
                        confirmTitle = null
                        confirmAction = null
                    },
                )
            },
        )
    }
}

@Composable
private fun SettingsSectionContent(
    section: String?,
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    fromIntake: Boolean,
    onBackupRestored: () -> Unit,
    requestConfirm: (String, String, () -> Unit) -> Unit,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val message by viewModel.message.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(bottom = RepairAppSpacing.bottomContentPadding),
    ) {
        message?.let {
            DismissibleAppMessageBanner(
                message = it.text,
                type = if (it.isError) BannerType.Error else BannerType.Info,
                onDismiss = viewModel::clearMessage,
                modifier = Modifier.padding(RepairAppSpacing.screenHorizontal),
            )
        }
        when (section) {
            "CATEGORIES" ->
                CategoriesSettings(
                    viewModel,
                    isAdmin,
                    fromIntake,
                    requestConfirm,
                    onBack = onBack,
                )

            "TEMPLATES" -> TemplatesSettings(viewModel, isAdmin, requestConfirm, onBack = onBack)
            "TEMPLATE_MAPPING" -> TemplateMappingSettings(viewModel, isAdmin, requestConfirm, onBack = onBack)
            "WHATSAPP" -> WhatsAppSettings(viewModel, requestConfirm, onBack = onBack)
            "EXPORT" ->
                BackupSettings(
                    viewModel,
                    isAdmin,
                    onBackupRestored,
                    requestConfirm,
                    onBack = onBack,
                )

            "STORAGE" -> StorageSettings(viewModel, onBack = onBack)
            "ABOUT" -> AboutSettings(onBack = onBack)
            else ->
                if (section == null) {
                    SettingsHub(isAdmin, onNavigate)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppEmptyState(
                            title = stringResource(R.string.settings_select_section_title),
                            body = stringResource(R.string.settings_select_section_body),
                        )
                    }
                }
        }
    }
}
