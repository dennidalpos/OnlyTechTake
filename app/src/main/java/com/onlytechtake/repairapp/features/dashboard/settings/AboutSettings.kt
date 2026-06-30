package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.utils.LicenseTexts
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun AboutSettings(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val apacheLicenseName = stringResource(R.string.about_license_apache)
    val bsdLicenseName = stringResource(R.string.about_license_bsd)
    var selectedLicenseName by remember { mutableStateOf<String?>(null) }
    var selectedLicenseText by remember { mutableStateOf("") }

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
                Row(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.screenHorizontal),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_brand_avatar),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        Text(
                            stringResource(R.string.about_app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.about_version,
                                    BuildConfig.VERSION_NAME,
                                    BuildConfig.VERSION_CODE,
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
        }

        item {
            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(
                        stringResource(R.string.about_description_title),
                        style = MaterialTheme.typography.titleSmall,
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
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.about_copyright),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    val githubUrl = stringResource(R.string.about_url_github)
                    AppButton(
                        text = stringResource(R.string.about_github),
                        variant = AppButtonVariant.Secondary,
                        leadingIcon = Icons.Default.Code,
                        onClick = { uriHandler.openUri(githubUrl) },
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
                        stringResource(R.string.about_legal_credits_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.about_legal_credits_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    AppButton(
                        text = apacheLicenseName,
                        variant = AppButtonVariant.Ghost,
                        size = AppButtonSize.Compact,
                        leadingIcon = Icons.Default.Info,
                        onClick = {
                            selectedLicenseName = apacheLicenseName
                            selectedLicenseText = LicenseTexts.APACHE_2_0
                        },
                    )
                    AppButton(
                        text = bsdLicenseName,
                        variant = AppButtonVariant.Ghost,
                        size = AppButtonSize.Compact,
                        leadingIcon = Icons.Default.Info,
                        onClick = {
                            selectedLicenseName = bsdLicenseName
                            selectedLicenseText = LicenseTexts.BSD_3_CLAUSE
                        },
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
                        stringResource(R.string.about_links_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val homeUrl = stringResource(R.string.about_url_home)
                    AppButton(
                        text = stringResource(R.string.about_home_page),
                        variant = AppButtonVariant.Ghost,
                        leadingIcon = Icons.Default.Public,
                        onClick = { uriHandler.openUri(homeUrl) },
                    )
                    val privacyUrl = stringResource(R.string.about_url_privacy)
                    AppButton(
                        text = stringResource(R.string.about_privacy_policy),
                        variant = AppButtonVariant.Ghost,
                        leadingIcon = Icons.Default.Shield,
                        onClick = { uriHandler.openUri(privacyUrl) },
                    )
                }
            }
        }

        item {
            AppButton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = RepairAppSpacing.screenHorizontal),
                text = stringResource(R.string.about_close),
                variant = AppButtonVariant.Secondary,
                onClick = onBack,
            )
        }
    }

    selectedLicenseName?.let { name ->
        AppDialog(
            onDismissRequest = { selectedLicenseName = null },
            title = name,
            text = "", // We'll use a custom content for the license text
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.about_close),
                    onClick = { selectedLicenseName = null },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                )
            },
            content = {
                Column(
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp)
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = selectedLicenseText,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                            ),
                    )
                }
            },
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
private fun AboutSettingsPreview() {
    com.onlytechtake.repairapp.core.design.RepairAppTheme {
        AboutSettings {}
    }
}
