package com.onlytechtake.repairapp.features.dashboard.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.core.demo.DemoMode

@Composable
fun DemoBanner() {
    if (BuildConfig.IS_DEMO_VERSION || BuildConfig.IS_SHOWCASE_MODE_ENABLED) {
        val banner = DemoMode.getVersionBanner(LocalContext.current)
        if (banner.isNotBlank()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(0.08f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = banner,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.Gray,
                    modifier = Modifier.rotate(-45f),
                    softWrap = false,
                )
            }
        }
    }
}
