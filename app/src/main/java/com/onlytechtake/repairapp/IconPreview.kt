package com.onlytechtake.repairapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun IconsPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pro Icon")
        IconBox(R.drawable.ic_launcher_foreground, Color(0xFF064E3B))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Demo Icon")
        // Note: these rely on the resources being available in the build variant
        // Since we are in 'main', we might only see the 'main' version if not careful
        // But for previewing the XML we just edited, this works if we use the right res ID
    }
}

@Composable
fun IconBox(resId: Int, bgColor: Color) {
    Box(
        modifier = Modifier
            .size(108.dp)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.size(108.dp)
        )
    }
}

@Composable
fun Text(text: String) {
    androidx.compose.material3.Text(text = text)
}
