package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun SettingsPanel(
    viewModel: WorkspaceViewModel,
    isAdmin: Boolean,
    onBackupRestored: () -> Unit,
    initialSection: String? = null,
) {
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (initialSection) {
            "CATEGORIES" -> CategoriesSettings(viewModel, isAdmin, ::requestConfirm)
            "TEMPLATES" -> TemplatesSettings(viewModel, isAdmin, ::requestConfirm)
            "WHATSAPP" -> WhatsAppSettings(viewModel, ::requestConfirm)
            "EXPORT" -> BackupSettings(viewModel, isAdmin, onBackupRestored, ::requestConfirm)
            "STORAGE" -> StorageSettings(viewModel)
            else -> SettingsHub(viewModel, isAdmin)
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
