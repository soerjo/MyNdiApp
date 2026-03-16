package com.soerjo.myndicam.presentation.screen.camera.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiveBadge(
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    if (isStreaming) {
        Box(
            modifier = modifier
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Red.copy(alpha = 0.9f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "LIVE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
