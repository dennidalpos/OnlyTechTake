package com.onlytechtake.repairapp.features.dashboard.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.features.dashboard.common.*

@Composable
fun OrderSignatureSection(
    detail: OrderDetail,
) {
    var selectedSignaturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSignatureTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.testTag(RepairAppTestTags.OrderSignatureSection),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
    ) {
        SectionTitle(stringResource(R.string.signature_section_title))

        if (detail.signatures.isEmpty()) {
            Text(stringResource(R.string.signature_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        } else {
            detail.signatures.forEach { signature ->
                val signatureTitle = stringResource(R.string.signature_image_description, signature.signerName)
                StandardCard {
                    Column(
                        modifier = Modifier.padding(RepairAppSpacing.compactGap),
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        LocalImagePreview(
                            path = signature.localPath,
                            description = signatureTitle,
                            onClick = {
                                selectedSignaturePath = signature.localPath
                                selectedSignatureTitle = signatureTitle
                            },
                        )
                        Text(
                            text = stringResource(R.string.signature_signer_value, signature.signerName),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
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
}
