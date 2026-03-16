package com.soerjo.myndicam.presentation.screen.camera.components.controls

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CameraSwitchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CircularIconButton(
        icon = Icons.Filled.Refresh,
        onClick = onClick,
        modifier = modifier.size(48.dp)
    )
}
