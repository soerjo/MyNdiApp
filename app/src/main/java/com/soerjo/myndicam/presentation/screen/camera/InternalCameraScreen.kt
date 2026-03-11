package com.soerjo.myndicam.presentation.screen.camera

import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soerjo.myndicam.core.common.Constants
import com.soerjo.myndicam.core.util.UyvyBufferPool
import com.soerjo.myndicam.data.camera.InternalCameraController
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.model.FrameRate
import java.util.concurrent.Executors

@Composable
fun InternalCameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentWidth by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(0) }
    var currentFps by remember { mutableIntStateOf(0) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.currentTimeMillis()) }


    var showMenu by remember { mutableStateOf(false) }
    var showCameraSelector by remember { mutableStateOf(false) }
    var showFrameRateSelector by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(uiState.sourceName) }

    LaunchedEffect(uiState.sourceName) {
        sourceNameInput = uiState.sourceName
    }

    // Initialize internal camera if none selected
    LaunchedEffect(Unit) {
        if (uiState.selectedCamera !is CameraInfo.CameraX) {
            val internalCameras = viewModel.getInternalCameras()
            if (internalCameras.isNotEmpty()) {
                viewModel.selectCamera(internalCameras.first())
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tally blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val dotAlpha = when {
        uiState.isStreaming && uiState.tallyState.isOnProgram -> blinkAlpha
        uiState.isStreaming && uiState.tallyState.isOnPreview -> blinkAlpha
        else -> 0f
    }
    val dotColor = when {
        uiState.isStreaming && uiState.tallyState.isOnProgram -> Color(0xFF00FF00)
        uiState.isStreaming && uiState.tallyState.isOnPreview -> Color(0xFFFFFF00)
        else -> Color.Transparent
    }

    // Store previewView reference
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // ProcessCameraProvider reference
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Background executor for frame processing to avoid blocking main thread
    val frameProcessingExecutor = remember { Executors.newSingleThreadExecutor { r -> Thread(r, "NDI-Frame-Processor") } }

    // Cleanup executor when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            frameProcessingExecutor.shutdown()
        }
    }

    // Camera binding effect
    LaunchedEffect(uiState.selectedCamera) {
        val selectedCamera = uiState.selectedCamera
        if (selectedCamera !is CameraInfo.CameraX) return@LaunchedEffect

        val cameraSelector = selectedCamera.cameraSelector
        Log.d("InternalCameraScreen", "Binding camera: $cameraSelector")

        try {
            val cameraProvider = cameraProviderFuture.get()

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(Constants.TARGET_WIDTH, Constants.TARGET_HEIGHT),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_16_9,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.setSurfaceProvider(previewView?.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setImageQueueDepth(4)
                .build()
                .apply {
                    setAnalyzer(frameProcessingExecutor) { imageProxy ->

                        if (imageProxy.width != currentWidth || imageProxy.height != currentHeight) {
                            currentWidth = imageProxy.width
                            currentHeight = imageProxy.height
                            viewModel.updateActualResolution(imageProxy.width, imageProxy.height)
                        }

                        frameCount++
                        val now = System.currentTimeMillis()
                        if (now - lastFpsTime >= 1000) {
                            currentFps = frameCount
                            frameCount = 0
                            lastFpsTime = now
                        }

                        viewModel.updateActualResolution(imageProxy.width, imageProxy.height)
                        processImageForNDI(imageProxy, viewModel)
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            Log.d("InternalCameraScreen", "Camera bound successfully")

        } catch (e: Exception) {
            Log.e("InternalCameraScreen", "Camera binding failed: ${e.message}", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.isStreaming && uiState.tallyState.isOnProgram) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 12.dp, color = Color(0xFF00FF00))
            )
        }

        if (uiState.isStreaming) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
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

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = dotAlpha))
                )
                if (uiState.tallyState.isOnPreview || uiState.tallyState.isOnProgram) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${currentFps}fps",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (currentHeight > 0) "${currentHeight}p" else "---",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.toggleStreaming() },
                modifier = Modifier.size(72.dp),
                containerColor = if (uiState.isStreaming)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (uiState.isStreaming) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = if (uiState.isStreaming) "Stop" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        IconButton(
            onClick = {
                val internalCameras = viewModel.getInternalCameras()
                val currentCamera = uiState.selectedCamera as? CameraInfo.CameraX
                val currentIndex = internalCameras.indexOf(currentCamera)
                if (internalCameras.isNotEmpty()) {
                    val nextIndex = (currentIndex + 1) % internalCameras.size
                    viewModel.selectCamera(internalCameras[nextIndex])
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Switch Camera",
                tint = Color.White
            )
        }

        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Menu",
                tint = Color.White
            )
        }

        if (showMenu) {
            SimpleMenuDialog(
                cameraName = uiState.selectedCamera?.name ?: "Select Camera",
                frameRateName = uiState.selectedFrameRate.displayName,
                onCameraClick = {
                    showMenu = false
                    showCameraSelector = true
                },
                onFrameRateClick = {
                    showMenu = false
                    showFrameRateSelector = true
                },
                onSettingsClick = {
                    showMenu = false
                    showSettings = true
                    sourceNameInput = uiState.sourceName
                },
                onDismiss = { showMenu = false }
            )
        }

        if (showCameraSelector) {
            CameraSelectorDialog(
                cameras = viewModel.getInternalCameras(),
                selectedCamera = uiState.selectedCamera as? CameraInfo.CameraX,
                onCameraSelected = { camera ->
                    viewModel.selectCamera(camera)
                    showCameraSelector = false
                },
                onDismiss = { showCameraSelector = false }
            )
        }

        if (showSettings) {
            SimpleSettingsDialog(
                sourceName = sourceNameInput,
                onSourceNameChange = { sourceNameInput = it },
                onSave = {
                    if (sourceNameInput.isNotBlank()) {
                        viewModel.saveSourceName(sourceNameInput)
                    }
                    showSettings = false
                },
                onDismiss = { showSettings = false }
            )
        }

        if (showFrameRateSelector) {
            FrameRateSelectorDialog(
                selectedFrameRate = uiState.selectedFrameRate,
                onFrameRateSelected = { frameRate ->
                    viewModel.selectFrameRate(frameRate)
                    showFrameRateSelector = false
                },
                onDismiss = { showFrameRateSelector = false }
            )
        }
    }
}

private fun processImageForNDI(
    imageProxy: ImageProxy,
    viewModel: CameraViewModel
) {
    try {
        val width = imageProxy.width
        val height = imageProxy.height
        val planes = imageProxy.planes

        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val yRowStride = planes[0].rowStride
        val uRowStride = planes[1].rowStride
        val vRowStride = planes[2].rowStride
        val uPixelStride = planes[1].pixelStride
        val vPixelStride = planes[2].pixelStride

        val uyvyData = yuvToUyvy(
            yBuffer, uBuffer, vBuffer,
            width, height,
            yRowStride, uRowStride, vRowStride,
            uPixelStride, vPixelStride
        )

        val stride = width * 2
        viewModel.sendFrame(uyvyData, width, height, stride)

    } catch (e: Exception) {
        Log.e("InternalCameraScreen", "Error processing image for NDI: ${e.message}")
    } finally {
        imageProxy.close()
    }
}

/**
 * Optimized YUV to UYVY conversion with direct array access
 */
private fun yuvToUyvy(
    yBuffer: java.nio.ByteBuffer,
    uBuffer: java.nio.ByteBuffer,
    vBuffer: java.nio.ByteBuffer,
    width: Int,
    height: Int,
    yRowStride: Int,
    uRowStride: Int,
    vRowStride: Int,
    uPixelStride: Int,
    vPixelStride: Int
): ByteArray {
    val uyvySize = width * height * 2
    val uyvy = UyvyBufferPool.obtain(uyvySize)

    val yData = if (yBuffer.hasArray()) yBuffer.array() else ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }
    val uData = if (uBuffer.hasArray()) uBuffer.array() else ByteArray(uBuffer.remaining()).also { uBuffer.get(it) }
    val vData = if (vBuffer.hasArray()) vBuffer.array() else ByteArray(vBuffer.remaining()).also { vBuffer.get(it) }

    var uyvyIndex = 0

    for (y in 0 until height) {
        for (x in 0 until width step 2) {
            val yIndex1 = y * yRowStride + x
            val yIndex2 = y * yRowStride + (x + 1)
            val yValue1 = yData[yIndex1].toInt() and 0xFF
            val yValue2 = if (x + 1 < width) {
                yData[yIndex2].toInt() and 0xFF
            } else {
                yValue1
            }

            val uvY = y / 2
            val uvX = x / 2
            val uIndex = uvY * uRowStride + uvX * uPixelStride
            val vIndex = uvY * vRowStride + uvX * vPixelStride
            val uValue = uData[uIndex].toInt() and 0xFF
            val vValue = vData[vIndex].toInt() and 0xFF

            uyvy[uyvyIndex++] = uValue.toByte()
            uyvy[uyvyIndex++] = yValue1.toByte()
            uyvy[uyvyIndex++] = vValue.toByte()
            uyvy[uyvyIndex++] = yValue2.toByte()
        }
    }

    return uyvy
}

@Composable
private fun FrameRateSelectorDialog(
    selectedFrameRate: FrameRate,
    onFrameRateSelected: (FrameRate) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Frame Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                FrameRate.values().forEach { frameRate ->
                    FrameRateOption(
                        name = frameRate.displayName,
                        isSelected = selectedFrameRate == frameRate,
                        onClick = { onFrameRateSelected(frameRate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameRateOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SimpleMenuDialog(
    cameraName: String,
    frameRateName: String,
    onCameraClick: () -> Unit,
    onFrameRateClick: () -> Unit,
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
                    icon = Icons.Filled.Settings,
                    title = frameRateName,
                    subtitle = "Frame Rate",
                    onClick = onFrameRateClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Black.copy(alpha = 0.1f)
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

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun CameraSelectorDialog(
    cameras: List<CameraInfo.CameraX>,
    selectedCamera: CameraInfo.CameraX?,
    onCameraSelected: (CameraInfo.CameraX) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Camera",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                cameras.forEach { camera ->
                    CameraOption(
                        name = camera.name,
                        isSelected = selectedCamera?.name == camera.name,
                        onClick = { onCameraSelected(camera) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SimpleSettingsDialog(
    sourceName: String,
    onSourceNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "NDI Source Name",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sourceName,
                    onValueChange = onSourceNameChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSave) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
