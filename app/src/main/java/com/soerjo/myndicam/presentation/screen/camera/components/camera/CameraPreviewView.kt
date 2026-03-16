package com.soerjo.myndicam.presentation.screen.camera.components.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreviewView(
    previewView: PreviewView,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { previewView.apply { scaleType = PreviewView.ScaleType.FIT_CENTER } },
        modifier = modifier
    )
}
