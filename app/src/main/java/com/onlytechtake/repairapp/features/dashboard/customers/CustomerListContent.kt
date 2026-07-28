package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.LoadingBrandIndicator
import com.onlytechtake.repairapp.core.design.RepairAppIconSizes
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSizes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.domain.models.Customer
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import kotlinx.coroutines.launch

@Composable
fun CustomerListContent(
    viewModel: CustomersViewModel,
    onCustomerClick: (Customer) -> Unit,
    onNewCustomer: () -> Unit,
) {
    val customers by viewModel.customerSuggestions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showAll by viewModel.showAll.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }
    val scope = rememberCoroutineScope()

    AppScreenContainer(scrollable = false) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            AppTextField(
                modifier = Modifier.weight(1f),
                value = searchQuery,
                onValueChange = {
                    viewModel.setSearchQuery(it)
                    if (it.isNotBlank()) viewModel.setShowAll(enabled = false)
                },
                label = stringResource(R.string.label_search_customer),
                singleLine = true,
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                testTag = RepairAppTestTags.CustomerSearchField,
            )
            Surface(
                onClick = onNewCustomer,
                color = MaterialTheme.colorScheme.primary,
                shape = RepairAppShapes.button,
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(RepairAppSizes.buttonDefault)
                        .testTag(RepairAppTestTags.CustomerNewButton),
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.customer_new),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(RepairAppIconSizes.standard),
                    )
                }
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.compactGap))

        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        if (showAll) {
                            stringResource(R.string.label_customers)
                        } else {
                            stringResource(
                                R.string.customer_recent,
                            )
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AppButton(
                    text =
                        if (showAll) {
                            stringResource(R.string.customer_recent)
                        } else {
                            stringResource(
                                R.string.customer_show_all,
                            )
                        },
                    onClick = { viewModel.setShowAll(!showAll) },
                    variant = AppButtonVariant.Ghost,
                    size = AppButtonSize.Compact,
                )
            }
        } else {
            SectionTitle(stringResource(R.string.customer_search_results))
        }

        Box(modifier = Modifier.weight(1f)) {
            val customerList = customers
            if (customerList == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingBrandIndicator(
                        text = stringResource(R.string.order_loading_detail),
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag("customer_list")
                            .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                    contentPadding =
                        PaddingValues(
                            top = RepairAppSpacing.compactGap,
                            bottom = RepairAppSpacing.bottomContentPadding,
                        ),
                ) {
                    if (customerList.isEmpty()) {
                        item {
                            CustomerEmptyState(hasQuery = searchQuery.isNotBlank())
                        }
                    } else {
                        items(customerList, key = { it.id }) { customer ->
                            CustomerCard(customer = customer) { onCustomerClick(customer) }
                        }
                    }
                }
            }

            if (showScrollToTop) {
                AppButton(
                    text = stringResource(R.string.customer_scroll_to_top),
                    leadingIcon = Icons.Default.KeyboardArrowUp,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = RepairAppSpacing.screenVertical)
                            .testTag(RepairAppTestTags.ScrollToTopButton),
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            }
        }
    }
}
