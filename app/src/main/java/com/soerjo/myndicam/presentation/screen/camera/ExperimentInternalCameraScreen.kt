package com.soerjo.myndicam.presentation.screen.camera

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
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.camera2.interop.Camera2Interop
import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.model.Resolution
import com.soerjo.myndicam.domain.model.ScreenMode

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun ExperimentInternalCameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showCameraSelector by remember { mutableStateOf(false) }
    var showResolutionSelector by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(uiState.sourceName) }

    var currentFps by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(0) }
    var currentWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.sourceName) {
        sourceNameInput = uiState.sourceName
    }

    LaunchedEffect(uiState.selectedResolution) {
        currentHeight = uiState.selectedResolution.height
        currentWidth = uiState.selectedResolution.width
        currentFps = 60
    }

    val cameraProvider = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView.apply { scaleType = PreviewView.ScaleType.FIT_CENTER } },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(uiState.selectedCamera, uiState.selectedResolution) {
            val provider = cameraProvider.get()
            provider.unbindAll()

            val cameraSelector = (uiState.selectedCamera as? CameraInfo.CameraX)?.cameraSelector
                ?: CameraSelector.DEFAULT_BACK_CAMERA

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(uiState.selectedResolution.width, uiState.selectedResolution.height),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
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
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .apply {
                    Camera2Interop.Extender(this).setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        android.util.Range(60, 60)
                    )
                }
                .build()

            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        }

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
                         if (currentHeight > 0) "${currentHeight}x${currentWidth}" else "---",
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
                resolutionName = uiState.selectedResolution.displayName,
                onSwitchToUsbClick = {
                    viewModel.switchToScreenMode(ScreenMode.USB)
                    showMenu = false
                },
                onSwitchToNormalClick = {
                    viewModel.switchToScreenMode(ScreenMode.INTERNAL)
                    showMenu = false
                },
                onCameraClick = {
                    showMenu = false
                    showCameraSelector = true
                },
                onResolutionClick = {
                    showMenu = false
                    showResolutionSelector = true
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

        if (showResolutionSelector) {
            ResolutionSelectorDialog(
                selectedResolution = uiState.selectedResolution,
                onResolutionSelected = { resolution ->
                    viewModel.selectResolution(resolution)
                    showResolutionSelector = false
                },
                onDismiss = { showResolutionSelector = false }
            )
        }
    }
}

@Composable
private fun ResolutionSelectorDialog(
    selectedResolution: Resolution,
    onResolutionSelected: (Resolution) -> Unit,
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
                    "Select Resolution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Resolution.values().forEach { resolution ->
                    ResolutionOption(
                        name = resolution.displayName,
                        isSelected = selectedResolution == resolution,
                        onClick = { onResolutionSelected(resolution) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResolutionOption(
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
private fun SimpleMenuDialog(
    cameraName: String,
    resolutionName: String,
    onSwitchToUsbClick: () -> Unit,
    onSwitchToNormalClick: () -> Unit,
    onCameraClick: () -> Unit,
    onResolutionClick: () -> Unit,
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
                    title = "Switch to USB Camera",
                    subtitle = "Change camera type",
                    onClick = onSwitchToUsbClick
                )
                MenuItem(
                    icon = Icons.Filled.Refresh,
                    title = "Switch to Normal Camera",
                    subtitle = "Change camera type",
                    onClick = onSwitchToNormalClick
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
