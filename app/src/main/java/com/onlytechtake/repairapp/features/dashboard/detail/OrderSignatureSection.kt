package com.onlytechtake.repairapp.features.dashboard.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Delete
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import androidx.compose.material.icons.Icons
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.features.dashboard.common.LocalImagePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImageViewer
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun OrderSignatureSection(
    detail: OrderDetail,
    onDeleteSignature: (String) -> Unit = {},
) {
    var selectedSignaturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSignatureTitle by remember { mutableStateOf("") }
    var deleteSignatureId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.testTag(RepairAppTestTags.OrderSignatureSection),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
    ) {
        SectionTitle(stringResource(R.string.signature_section_title))

        if (detail.signatures.isEmpty()) {
            Text(
                stringResource(R.string.signature_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            detail.signatures.forEach { signature ->
                val signatureTitle =
                    stringResource(R.string.signature_image_description, signature.signerName)
                StandardCard {
                    Column(
                        modifier = Modifier.padding(RepairAppSpacing.compactGap),
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        LocalImagePreview(
                            path = signature.localPath,
                            description = signatureTitle,
                        ) {
                            selectedSignaturePath = signature.localPath
                            selectedSignatureTitle = signatureTitle
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = signature.signerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = com.onlytechtake.repairapp.core.utils.formatUiDateTime(signature.signedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            AppIconButton(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.content_description_delete),
                                onClick = { deleteSignatureId = signature.id },
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(
                            text = stringResource(if (signature.privacyAccepted) R.string.signature_privacy_accepted else R.string.signature_privacy_rejected),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (signature.privacyAccepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    selectedSignaturePath?.let { path ->
        LocalImageViewer(
            path = path,
            title = selectedSignatureTitle,
            onDismiss = { selectedSignaturePath = null },
        )
    }

    deleteSignatureId?.let { id ->
        AppDialog(
            onDismissRequest = { deleteSignatureId = null },
            title = "Eliminare firma?",
            text = "La firma verrà rimossa permanentemente.",
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        onDeleteSignature(id)
                        deleteSignatureId = null
                    },
                    variant = AppButtonVariant.Danger,
                    size = com.onlytechtake.repairapp.core.design.components.AppButtonSize.Compact,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = { deleteSignatureId = null },
                    variant = AppButtonVariant.Secondary,
                    size = com.onlytechtake.repairapp.core.design.components.AppButtonSize.Compact,
                )
            },
        )
    }
}
