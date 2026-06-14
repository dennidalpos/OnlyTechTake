package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone

@Composable
fun AboutSettings(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = DashboardSpacing.maxContentWidth),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle(
                text = "Informazioni",
                tooltip = "Dettagli sulla versione dell'applicazione e contatti per lo sviluppo."
            )
        }

        item {
            StandardCard(highlight = true) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)
                ) {
                    Text("OnlyTechTake Repair App", style = MaterialTheme.typography.titleLarge)
                    Text("Versione: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyMedium)
                    Text("Build Type: ${BuildConfig.BUILD_TYPE}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        item {
            StandardCard {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)
                ) {
                    Text("Descrizione", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "OnlyTechTake è un gestionale locale per laboratori di riparazione. " +
                        "Tutti i tuoi dati restano sul dispositivo per garantirti massima privacy e velocità.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            StandardCard {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)
                ) {
                    Text("Sviluppo", style = MaterialTheme.typography.titleSmall)
                    Text("© 2026 OnlyTechTake", style = MaterialTheme.typography.bodyMedium)
                    
                    TouchButton(
                        text = "GitHub: dennidalpos",
                        tone = ButtonTone.NEUTRAL,
                        onClick = { uriHandler.openUri("https://github.com/dennidalpos") }
                    )

                    Text(
                        "Sito web: https://onlytechtake.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri("https://onlytechtake.com") }
                    )
                }
            }
        }

        item {
            TouchButton(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                text = "Chiudi Informazioni",
                tone = ButtonTone.NEUTRAL,
                onClick = onBack
            )
        }
    }
}
