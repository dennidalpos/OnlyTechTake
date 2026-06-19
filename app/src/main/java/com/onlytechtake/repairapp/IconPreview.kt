package com.onlytechtake.repairapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun IconsPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.preview_pro_icon),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        IconBox(R.drawable.ic_launcher_foreground, Color(0xFF006A6A))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.preview_demo_icon),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        IconBox(R.drawable.ic_launcher_foreground, Color(0xFF006A6A))

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_motto),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        )
    }
}

@Composable
fun IconBox(
    resId: Int,
    bgColor: Color,
) {
    Box(
        modifier =
            Modifier
                .size(108.dp)
                .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.size(108.dp),
        )
    }
}
