package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass

@Composable
fun SettingsPanel(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onBackupRestored: () -> Unit,
    onNavigate: (String) -> Unit,
    initialSection: String? = null,
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

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    val onBackToHub = { 
        if (isWide) {
            currentSection = null
        } else {
            onNavigate(com.onlytechtake.repairapp.features.dashboard.DashboardRoutes.settings())
        }
    }

    if (isWide) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                SettingsHub(
                    isAdmin = isAdmin,
                    onNavigate = { route ->
                        val section = if (route.contains("section=")) {
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
                )
            }
            VerticalDivider()
            Box(Modifier.weight(2f)) {
                SettingsSectionContent(
                    section = currentSection,
                    viewModel = viewModel,
                    isAdmin = isAdmin,
                    onBackupRestored = onBackupRestored,
                    requestConfirm = ::requestConfirm,
                    onNavigate = onNavigate,
                    onBack = onBackToHub
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
                onBackupRestored = onBackupRestored,
                requestConfirm = ::requestConfirm,
                onNavigate = onNavigate,
                onBack = onBackToHub
            )
        }
    }

    confirmTitle?.let { title ->
        AlertDialog(
            onDismissRequest = {
                confirmTitle = null
                confirmAction = null
            },
            title = { Text(title) },
            text = { Text(confirmText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction?.invoke()
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text(stringResource(R.string.customer_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionContent(
    section: String?,
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onBackupRestored: () -> Unit,
    requestConfirm: (String, String, () -> Unit) -> Unit,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    when (section) {
        "CATEGORIES" -> CategoriesSettings(viewModel, isAdmin, requestConfirm, onBack = onBack)
        "TEMPLATES" -> TemplatesSettings(viewModel, isAdmin, requestConfirm, onBack = onBack)
        "WHATSAPP" -> WhatsAppSettings(viewModel, requestConfirm, onBack = onBack)
        "EXPORT" -> BackupSettings(viewModel, isAdmin, onBackupRestored, requestConfirm, onBack = onBack)
        "STORAGE" -> StorageSettings(viewModel, onBack = onBack)
        "ABOUT" -> AboutSettings(onBack = onBack)
        else -> if (section == null) SettingsHub(isAdmin, onNavigate) else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Seleziona una sezione")
            }
        }
    }
}
