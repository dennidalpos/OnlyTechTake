package com.onlytechtake.repairapp.features.dashboard.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.features.dashboard.common.LocalImagePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImageViewer
import com.onlytechtake.repairapp.features.dashboard.common.CompactExpandableRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.core.utils.formatUiDateTime

@Composable
fun OrderSignatureSection(
    detail: OrderDetail,
) {
    var selectedSignaturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSignatureTitle by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
        SectionTitle("Firma privacy")
        if (detail.signatures.isEmpty()) {
            Text("Nessuna firma acquisita.")
        } else {
            detail.signatures.forEach { signature ->
                StandardCard {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.compactGap),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                    ) {
                        LocalImagePreview(
                            path = signature.localPath,
                            description = "Firma ${signature.signerName}",
                            onClick = {
                                selectedSignaturePath = signature.localPath
                                selectedSignatureTitle = "Firma ${signature.signerName}"
                            },
                        )
                        CompactExpandableRow(
                            title = signature.signerName,
                            subtitle = formatUiDateTime(signature.signedAt),
                            detail = "Privacy accettata: ${if (signature.privacyAccepted) "si" else "no"}\nCondizioni accettate: ${if (signature.termsAccepted) "si" else "no"}",
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
