package com.onlytechtake.repairapp.features.auth.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.onlytechtake.repairapp.core.design.LoadingBrandIndicator

@Composable
fun AuthLoadingContent(text: String) {
    Scaffold { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            LoadingBrandIndicator(
                text = text,
                contentDescription = text,
            )
        }
    }
}
