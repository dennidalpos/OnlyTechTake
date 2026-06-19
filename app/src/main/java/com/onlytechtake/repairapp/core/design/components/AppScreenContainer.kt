package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppSpacing

@Composable
fun AppScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    maxWidth: Dp = RepairAppSpacing.maxContentWidthMedium,
    horizontalPadding: Dp = RepairAppSpacing.screenHorizontal,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier),
        contentAlignment = Alignment.TopCenter,
    ) {
        val bottomPadding = if (scrollable) RepairAppSpacing.bottomContentPadding else 0.dp
        Column(
            modifier =
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .then(if (!scrollable) Modifier.fillMaxHeight() else Modifier)
                    .padding(horizontal = horizontalPadding)
                    .padding(top = RepairAppSpacing.screenVertical, bottom = bottomPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
