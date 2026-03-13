package com.soerjo.myndicam.presentation.screen.camera

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soerjo.myndicam.core.common.Constants
import com.soerjo.myndicam.presentation.screen.camera.FrameInfo
import com.soerjo.myndicam.presentation.screen.camera.FrameFormat
import com.soerjo.myndicam.presentation.screen.camera.convertToUyvy

@Composable
fun CameraScreen() {
    when (Constants.SCREEN_MODE) {
        1 -> UsbCameraScreen()
        2 -> InternalCameraScreen()
    }
}

@Composable
fun UsbCameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // FPS and resolution from preview component
    var currentFps by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(0) }

    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(uiState.sourceName) }

    LaunchedEffect(uiState.sourceName) {
        sourceNameInput = uiState.sourceName
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

    val activity = context as? androidx.fragment.app.FragmentActivity
    val fragmentManager = activity?.supportFragmentManager

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        UsbCameraPreview(
            fragmentManager = fragmentManager!!,
            onFrameData = { frameInfo ->
                // Update UI tracking
                currentFps = frameInfo.fps
                currentHeight = frameInfo.height

                if (frameInfo.data != null && uiState.isStreaming) {
                    try {
                        val uyvyData = convertToUyvy(
                            frameInfo.data,
                            frameInfo.width,
                            frameInfo.height,
                            frameInfo.format
                        )
                        viewModel.sendFrame(uyvyData, frameInfo.width, frameInfo.height, frameInfo.width * 2)
                    } catch (e: Exception) {
                        Log.e("UsbCameraScreen", "Error sending frame: ${e.message}")
                    }
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
                sourceName = uiState.sourceName,
                onSettingsClick = {
                    showMenu = false
                    showSettings = true
                    sourceNameInput = uiState.sourceName
                },
                onDismiss = { showMenu = false }
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
    }
}

@Composable
private fun SimpleMenuDialog(
    sourceName: String,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(280.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                SimpleMenuItem(
                    title = sourceName,
                    subtitle = "Camera",
                    onClick = onDismiss
                )
                SimpleMenuItem(
                    title = "Settings",
                    subtitle = "NDI Source Name",
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun SimpleMenuItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
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

