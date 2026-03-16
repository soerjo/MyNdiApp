package com.soerjo.myndicam.presentation.screen.camera.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.soerjo.myndicam.domain.model.Resolution
import com.soerjo.myndicam.presentation.screen.camera.components.common.SelectableOption

@Composable
fun ResolutionSelectorDialog(
    selectedResolution: Resolution,
    onResolutionSelected: (Resolution) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Resolution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Resolution.entries.forEach { resolution ->
                    SelectableOption(
                        label = resolution.displayName,
                        isSelected = selectedResolution == resolution,
                        onClick = { onResolutionSelected(resolution) }
                    )
                }
            }
        }
    }
}
