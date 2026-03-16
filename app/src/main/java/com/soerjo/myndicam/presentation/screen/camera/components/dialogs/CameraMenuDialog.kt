package com.soerjo.myndicam.presentation.screen.camera.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.soerjo.myndicam.presentation.screen.camera.components.common.MenuItem

@Composable
fun CameraMenuDialog(
    cameraName: String,
    resolutionName: String,
    isUsbMode: Boolean,
    onCameraClick: () -> Unit,
    onResolutionClick: () -> Unit,
    onSwitchToUsbClick: () -> Unit,
    onSwitchToInternalClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(280.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                MenuItem(
                    icon = Icons.Filled.Star,
                    title = cameraName,
                    onClick = onCameraClick
                )
                MenuItem(
                    icon = Icons.Filled.Refresh,
                    title = resolutionName,
                    subtitle = "Resolution",
                    onClick = onResolutionClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Black.copy(alpha = 0.1f)
                )
                MenuItem(
                    icon = Icons.Filled.Refresh,
                    title = if (isUsbMode) "Switch to Internal Camera" else "Switch to USB Camera",
                    subtitle = "Change camera type",
                    onClick = if (isUsbMode) onSwitchToInternalClick else onSwitchToUsbClick
                )
                MenuItem(
                    icon = Icons.Filled.Settings,
                    title = "Settings",
                    subtitle = "NDI Source Name",
                    onClick = onSettingsClick
                )
            }
        }
    }
}
