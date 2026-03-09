package com.soerjo.myndicam.presentation.screen.camera

import android.graphics.ImageFormat
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soerjo.myndicam.core.common.Constants
import com.soerjo.myndicam.core.util.convertYuvToUyvy
import com.soerjo.myndicam.core.util.convertYuvToUyvyCropped
import com.soerjo.myndicam.core.util.formatAspectRatio
import com.soerjo.myndicam.domain.model.FrameRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import androidx.hilt.navigation.compose.hiltViewModel
import com.soerjo.myndicam.presentation.screen.camera.components.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
/**
 * Main Camera Screen composable
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Blinking animation for tally dot
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

    // Tally dot behavior:
    // - isOnProgram (LIVE): Green blinking dot + green border
    // - isOnPreview only: Yellow blinking dot (no border)
    // - neither: Hidden dot
    val dotAlpha = when {
        uiState.isStreaming && uiState.tallyState.isOnProgram -> blinkAlpha  // Green blinking
        uiState.isStreaming && uiState.tallyState.isOnPreview -> blinkAlpha  // Yellow blinking
        else -> 0f                 // Hidden
    }
    val dotColor = when {
        uiState.isStreaming && uiState.tallyState.isOnProgram -> Color(0xFF00FF00)  // Green for LIVE
        uiState.isStreaming && uiState.tallyState.isOnPreview -> Color(0xFFFFFF00)  // Yellow for Preview
        else -> Color.Transparent
    }

    // Detect cameras on first composition
    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        viewModel.detectCameras(cameraProvider)
    }

    // Rebind camera when selected camera or streaming state changes
    LaunchedEffect(uiState.selectedCamera, uiState.isStreaming) {
        val camera = uiState.selectedCamera ?: return@LaunchedEffect
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
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setResolutionSelector(resolutionSelector)
            .setImageQueueDepth(1)  // Lower latency with queue depth of 1
            .build()
            .also {
                it.setAnalyzer(Dispatchers.Default.asExecutor()) { imageProxy ->
                    // Update actual resolution from first frame
                    viewModel.updateActualResolution(imageProxy.width, imageProxy.height)
                    // Pass streaming state directly to avoid StateFlow access per frame
                    processImageForNDI(imageProxy, viewModel, uiState.isStreaming)
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                camera.cameraSelectorFilter,
                preview,
                imageAnalysis
            )
            val resolution = imageAnalysis.resolutionInfo?.resolution
            if (resolution != null) {
                viewModel.updateActualResolution(resolution.width, resolution.height)
            }
        } catch (exc: Exception) {
            Log.e("CameraApp", "Use case binding failed: ${exc.message}", exc)
        }
    }

    // Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview (center crop to match NDI output)
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Green border overlay when on program
        if (uiState.isStreaming && uiState.tallyState.isOnProgram) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 12.dp, color = Color(0xFF00FF00))
            )
        }

        // Live indicator (only when streaming)
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

        // Status bar (top right) with blinking tally dot
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
                // Tally dot: Green=LIVE, Yellow=Preview, Hidden=Off
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
                        uiState.selectedFrameRate.displayName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${uiState.actualResolution.height}p" + " • " + viewModel.getFormattedAspectRatio(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Main streaming button (center bottom)
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

        // Menu button (bottom right)
        var showMenu by remember { mutableStateOf(false) }
        var showCameraSelector by remember { mutableStateOf(false) }
        var showFrameRateSelector by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        var sourceNameInput by remember { mutableStateOf(uiState.sourceName) }

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

        // Menu dialog
        if (showMenu) {
            MenuDialog(
                selectedCameraName = uiState.selectedCamera?.name ?: "Select Camera",
                selectedFrameRate = uiState.selectedFrameRate,
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
                },
                onDismiss = { showMenu = false }
            )
        }

        // Camera selector
        if (showCameraSelector) {
            CameraSelectorDialog(
                cameras = uiState.availableCameras,
                selectedCamera = uiState.selectedCamera,
                onCameraSelected = { camera ->
                    viewModel.selectCamera(camera)
                    showCameraSelector = false
                },
                onDismiss = { showCameraSelector = false }
            )
        }

        // Frame rate selector
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

        // Settings dialog
        if (showSettings) {
            SettingsDialog(
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
    }
}

/**
 * Process image and send to NDI
 * Note: isStreaming is passed directly to avoid StateFlow access per frame
 */
private fun processImageForNDI(
    imageProxy: ImageProxy,
    viewModel: CameraViewModel,
    isStreaming: Boolean
) {
    if (!isStreaming) {
        imageProxy.close()
        return
    }

    try {
        val uyvyData = convertYuvToUyvy(imageProxy.image!!)
        val width = imageProxy.width
        val height = imageProxy.height
        val stride = width * 2  // UYVY has 2 bytes per pixel

        viewModel.sendFrame(uyvyData, width, height, stride)
    } catch (e: Exception) {
        Log.e("CameraApp", "Error processing image for NDI: ${e.message}")
    } finally {
        imageProxy.close()
    }
}
