package com.onlytechtake.repairapp.features.dashboard.settings

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
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun AboutSettings(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = RepairAppSpacing.maxContentWidthMedium),
        contentPadding = PaddingValues(RepairAppSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
    ) {
        item {
            SectionTitle(
                text = stringResource(R.string.config_hub_about_title),
                tooltip = stringResource(R.string.about_tooltip),
            )
        }

        item {
            StandardCard(highlight = true) {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(
                        stringResource(R.string.about_app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(
                            R.string.about_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.about_build_type, BuildConfig.BUILD_TYPE),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        item {
            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(
                        stringResource(R.string.about_description_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.about_description_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(
                        stringResource(R.string.about_development),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        stringResource(R.string.about_copyright),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    AppButton(
                        text = stringResource(R.string.about_github),
                        variant = AppButtonVariant.Secondary,
                        onClick = { uriHandler.openUri("https://github.com/dennidalpos") },
                    )
                }
            }
        }

        item {
            AppButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RepairAppSpacing.screenHorizontal),
                text = stringResource(R.string.about_close),
                variant = AppButtonVariant.Secondary,
                onClick = onBack,
            )
        }
    }
}
