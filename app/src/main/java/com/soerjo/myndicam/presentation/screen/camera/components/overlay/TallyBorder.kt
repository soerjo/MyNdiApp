package com.soerjo.myndicam.presentation.screen.camera.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TallyBorder(
    isOnProgram: Boolean,
    modifier: Modifier = Modifier
) {
    if (isOnProgram) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .border(width = 12.dp, color = Color(0xFF00FF00))
        )
    }
}
