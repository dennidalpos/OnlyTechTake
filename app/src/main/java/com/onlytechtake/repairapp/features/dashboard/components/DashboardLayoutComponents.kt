package com.onlytechtake.repairapp.features.dashboard.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppStatusBadge
import com.onlytechtake.repairapp.core.design.components.AppTopBar
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.features.dashboard.DashboardNavRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    title: String,
    showLogo: Boolean,
    onBack: () -> Unit,
    currentUser: AuthenticatedUser?,
    onLogout: () -> Unit,
    isHome: Boolean,
    isCloseIcon: Boolean,
) {
    AppTopBar(
        title = title,
        showLogo = showLogo,
        navigationIcon = {
            if (!isHome) {
                AppIconButton(
                    modifier = Modifier.testTag(RepairAppTestTags.ConfigBackButton),
                    imageVector = if (isCloseIcon) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    tint = if (isCloseIcon) Color.Unspecified else MaterialTheme.colorScheme.primary,
                    contentDescription =
                        stringResource(
                            if (isCloseIcon) R.string.action_close else R.string.action_back,
                        ),
                    onClick = onBack,
                )
            }
        },
        actions = {
            currentUser?.let { user ->
                val isAdminUser = PermissionService.isAdmin(user)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = RepairAppSpacing.compactGap),
                ) {
                    androidx.compose.foundation.Image(
                        painter =
                            androidx.compose.ui.res
                                .painterResource(id = R.drawable.ic_brand_avatar),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.size(RepairAppSpacing.compactGap))
                    AppStatusBadge(
                        text =
                            stringResource(
                                if (isAdminUser) R.string.dashboard_role_admin else R.string.dashboard_role_operator,
                            ),
                        containerColor = if (isAdminUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isAdminUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.basicMarquee(),
                    )
                }
            }
            AppButton(
                text = stringResource(R.string.logout),
                onClick = onLogout,
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Compact,
                testTag = RepairAppTestTags.LogoutButton,
            )
        },
    )
}

data class DashboardNavigationItem(
    val route: DashboardNavRoute,
    val icon: ImageVector,
    val labelRes: Int,
    val testTag: String,
)

@Composable
fun DashboardBottomBar(
    items: List<DashboardNavigationItem>,
    isTabSelected: (DashboardNavRoute) -> Boolean,
    onItemClick: (DashboardNavigationItem) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.testTag(RepairAppTestTags.DashboardTabs),
    ) {
        items.forEach { item ->
            val selected = isTabSelected(item.route)
            NavigationBarItem(
                modifier = Modifier.testTag(item.testTag),
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                selected = selected,
                onClick = { onItemClick(item) },
            )
        }
    }
}

@Composable
fun DashboardNavRail(
    items: List<DashboardNavigationItem>,
    isTabSelected: (DashboardNavRoute) -> Boolean,
    onItemClick: (DashboardNavigationItem) -> Unit,
) {
    NavigationRail(
        modifier =
            Modifier
                .fillMaxHeight()
                .testTag(RepairAppTestTags.DashboardTabs),
    ) {
        Spacer(Modifier.weight(1f))
        items.forEach { item ->
            val selected = isTabSelected(item.route)
            NavigationRailItem(
                modifier = Modifier.testTag(item.testTag),
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.labelRes),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                selected = selected,
                onClick = { onItemClick(item) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
