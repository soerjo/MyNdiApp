package com.soerjo.myndicam.presentation.screen.camera.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soerjo.myndicam.presentation.screen.camera.NDIConnectionState

@Composable
fun StatusBadge(
    ndiConnectionState: NDIConnectionState,
    modifier: Modifier = Modifier
) {
    val statusInfo = when (ndiConnectionState) {
        is NDIConnectionState.NotInitialized -> StatusInfo(
            backgroundColor = Color(0xFFFFA000),
            textColor = Color.White,
            text = "NDI Not Ready"
        )
        is NDIConnectionState.Initializing -> StatusInfo(
            backgroundColor = Color(0xFFFFA000),
            textColor = Color.White,
            text = "NDI Initializing"
        )
        is NDIConnectionState.Ready -> StatusInfo(
            backgroundColor = Color.Transparent,
            textColor = Color.Transparent,
            text = ""
        )
        is NDIConnectionState.Error -> StatusInfo(
            backgroundColor = Color(0xFFFF5252),
            textColor = Color.White,
            text = "NDI Error"
        )
    }

    if (statusInfo.text.isNotEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(statusInfo.backgroundColor.copy(alpha = 0.9f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = statusInfo.textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    statusInfo.text,
                    color = statusInfo.textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

private data class StatusInfo(
    val backgroundColor: Color,
    val textColor: Color,
    val text: String
)
