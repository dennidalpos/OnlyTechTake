package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.onlytechtake.repairapp.core.design.RepairAppSpacing

@Composable
fun AppScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    maxWidth: Dp = RepairAppSpacing.maxContentWidthMedium,
    horizontalPadding: Dp = RepairAppSpacing.screenHorizontal,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .then(if (!scrollable) Modifier.fillMaxHeight() else Modifier)
                .padding(horizontal = horizontalPadding)
                .padding(top = RepairAppSpacing.screenVertical, bottom = RepairAppSpacing.bottomContentPadding),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}
