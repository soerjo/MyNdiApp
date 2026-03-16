package com.soerjo.myndicam.presentation.screen.camera.components.controls

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StreamToggleFAB(
    isStreaming: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onToggle,
        modifier = modifier.size(72.dp),
        containerColor = if (isStreaming)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primary,
        shape = CircleShape
    ) {
        Icon(
            imageVector = if (isStreaming) Icons.Filled.Close else Icons.Filled.PlayArrow,
            contentDescription = if (isStreaming) "Stop" else "Start",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}
