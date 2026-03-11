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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.soerjo.myndicam.core.common.Constants
import com.soerjo.myndicam.presentation.fragment.UsbCameraFragment
import com.soerjo.ndi.NDIManager
import com.soerjo.ndi.NDISender
import com.soerjo.ndi.model.TallyState
import kotlinx.coroutines.launch

@Composable
fun CameraScreen() {
    when (Constants.SCREEN_MODE) {
        2 -> UsbCameraScreen()
    }
}

@Composable
fun UsbCameraFragmentScreen() {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val fragmentManager = activity?.supportFragmentManager
    
    AndroidView(
        factory = { ctx ->
            androidx.fragment.app.FragmentContainerView(ctx).apply {
                id = android.R.id.custom
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { fragmentContainer ->
            fragmentManager?.let { fm ->
                if (fm.findFragmentById(fragmentContainer.id) == null) {
                    fm.beginTransaction()
                        .replace(fragmentContainer.id, UsbCameraFragment())
                        .commit()
                }
            }
        }
    )
}

@Composable
fun NewUIScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isStreaming by remember { mutableStateOf(false) }
    var sourceName by remember { mutableStateOf("USB Camera") }
    var tallyState by remember { mutableStateOf(TallyState()) }
    var currentFps by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(1080) }
    
    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(sourceName) }
    
    val ndiSender = remember { 
        try {
            if (!NDIManager.isInitialized()) {
                NDIManager.initialize()
            }
            NDIManager.createSender(sourceName)
        } catch (e: Exception) {
            Log.e("NewUIScreen", "Failed to initialize NDI: ${e.message}")
            null
        }
    }
    
    LaunchedEffect(Unit) {
        ndiSender?.let { sender ->
            lifecycleOwner.lifecycleScope.launch {
                sender.tallyState.collect { state ->
                    tallyState = state
                }
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
        isStreaming && tallyState.isOnProgram -> blinkAlpha
        isStreaming && tallyState.isOnPreview -> blinkAlpha
        else -> 0f
    }
    val dotColor = when {
        isStreaming && tallyState.isOnProgram -> Color(0xFF00FF00)
        isStreaming && tallyState.isOnPreview -> Color(0xFFFFFF00)
        else -> Color.Transparent
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isStreaming && tallyState.isOnProgram) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 12.dp, color = Color(0xFF00FF00))
            )
        }
        
        if (isStreaming) {
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
                if (tallyState.isOnPreview || tallyState.isOnProgram) {
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
                onClick = {
                    isStreaming = !isStreaming
                    Log.d("NewUIScreen", "Streaming: $isStreaming")
                },
                modifier = Modifier.size(72.dp),
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
                sourceName = sourceName,
                onSettingsClick = {
                    showMenu = false
                    showSettings = true
                    sourceNameInput = sourceName
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
                        sourceName = sourceNameInput
                    }
                    showSettings = false
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
fun UsbCameraScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current

    var isStreaming by remember { mutableStateOf(false) }
    var sourceName by remember { mutableStateOf("USB Camera") }
    var currentWidth by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(0) }
    var currentFps by remember { mutableIntStateOf(0) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.currentTimeMillis()) }

    var ndiSender by remember { mutableStateOf<NDISender?>(null) }
    var tallyState by remember { mutableStateOf(TallyState()) }

    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(sourceName) }

    var usbFragment by remember { mutableStateOf<UsbCameraFragment?>(null) }

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
        isStreaming && tallyState.isOnProgram -> blinkAlpha
        isStreaming && tallyState.isOnPreview -> blinkAlpha
        else -> 0f
    }
    val dotColor = when {
        isStreaming && tallyState.isOnProgram -> Color(0xFF00FF00)
        isStreaming && tallyState.isOnPreview -> Color(0xFFFFFF00)
        else -> Color.Transparent
    }

    LaunchedEffect(Unit) {
        try {
            if (!NDIManager.isInitialized()) {
                NDIManager.initialize()
            }
            ndiSender = NDIManager.createSender(sourceName)

            ndiSender?.let { sender ->
                lifecycleOwner.lifecycleScope.launch {
                    sender.tallyState.collect { state ->
                        tallyState = state
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UsbCameraScreen", "Failed to initialize NDI: ${e.message}")
        }
    }

    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val fragmentManager = activity?.supportFragmentManager

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            factory = { ctx ->
                androidx.fragment.app.FragmentContainerView(ctx).apply {
                    id = android.R.id.custom
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { fragmentContainer ->
                fragmentManager?.let { fm ->
                    val existingFragment = fm.findFragmentById(fragmentContainer.id)
                    if (existingFragment == null) {
                        fm.beginTransaction()
                            .replace(fragmentContainer.id, UsbCameraFragment())
                            .commit()
                    } else {
                        val fragment = existingFragment as? UsbCameraFragment
                        if (fragment != null && fragment != usbFragment) {
                            usbFragment = fragment
                            fragment.setFrameCallback(object : IPreviewDataCallBack {
                                override fun onPreviewData(
                                    data: ByteArray?,
                                    width: Int,
                                    height: Int,
                                    format: IPreviewDataCallBack.DataFormat
                                ) {
                                    if (width != currentWidth || height != currentHeight) {
                                        currentWidth = width
                                        currentHeight = height
                                    }

                                    frameCount++
                                    val now = System.currentTimeMillis()
                                    if (now - lastFpsTime >= 1000) {
                                        currentFps = frameCount
                                        frameCount = 0
                                        lastFpsTime = now
                                    }

                                    if (data != null && isStreaming) {
                                        try {
                                            val uyvyData = convertToUyvy(data, width, height, format)
                                            ndiSender?.sendFrame(uyvyData, width, height, width * 2)
                                        } catch (e: Exception) {
                                            Log.e("UsbCameraScreen", "Error sending frame: ${e.message}")
                                        }
                                    }
                                }
                            })
                        }
                    }
                }
            }
        )

        if (isStreaming && tallyState.isOnProgram) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 12.dp, color = Color(0xFF00FF00))
            )
        }

        if (isStreaming) {
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
                if (tallyState.isOnPreview || tallyState.isOnProgram) {
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
                onClick = {
                    isStreaming = !isStreaming
                    Log.d("UsbCameraScreen", "Streaming: $isStreaming")
                },
                modifier = Modifier.size(72.dp),
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
                sourceName = sourceName,
                onSettingsClick = {
                    showMenu = false
                    showSettings = true
                    sourceNameInput = sourceName
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
                        sourceName = sourceNameInput
                        try {
                            // Release old sender and create new one with updated name
                            ndiSender?.release()
                            ndiSender = NDIManager.createSender(sourceName)
                            
                            // Re-collect tally state from new sender
                            lifecycleOwner.lifecycleScope.launch {
                                ndiSender?.tallyState?.collect { state ->
                                    tallyState = state
                                }
                            }
                            Log.d("UsbCameraScreen", "NDI source name updated to: $sourceName")
                        } catch (e: Exception) {
                            Log.e("UsbCameraScreen", "Failed to update source name: ${e.message}")
                        }
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

private fun convertToUyvy(data: ByteArray, width: Int, height: Int, format: IPreviewDataCallBack.DataFormat): ByteArray {
    return when (format) {
        IPreviewDataCallBack.DataFormat.NV21 -> convertNv21ToUyvy(data, width, height)
        IPreviewDataCallBack.DataFormat.RGBA -> convertRgbaToUyvy(data, width, height)
    }
}

private fun convertNv21ToUyvy(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
    val uyvyData = ByteArray(width * height * 2)

    val ySize = width * height
    val vuOffset = ySize

    for (y in 0 until height step 2) {
        for (x in 0 until width step 2) {
            val yIndex00 = y * width + x
            val yIndex01 = y * width + (x + 1)
            val yIndex10 = (y + 1) * width + x
            val yIndex11 = (y + 1) * width + (x + 1)

            val y00 = nv21Data[yIndex00].toInt() and 0xFF
            val y01 = nv21Data[yIndex01].toInt() and 0xFF
            val y10 = nv21Data[yIndex10].toInt() and 0xFF
            val y11 = nv21Data[yIndex11].toInt() and 0xFF

            val vuIndex = vuOffset + (y / 2) * width + x
            val v = nv21Data[vuIndex].toInt() and 0xFF
            val u = nv21Data[vuIndex + 1].toInt() and 0xFF

            uyvyData[y * width * 2 + x * 2] = u.toByte()
            uyvyData[y * width * 2 + x * 2 + 1] = y00.toByte()
            uyvyData[y * width * 2 + (x + 1) * 2] = v.toByte()
            uyvyData[(y + 1) * width * 2 + x * 2] = u.toByte()
            uyvyData[(y + 1) * width * 2 + (x + 1) * 2] = y01.toByte()
            uyvyData[(y + 1) * width * 2 + x * 2 + 1] = y10.toByte()
            uyvyData[(y + 1) * width * 2 + x * 2 + 1] = y11.toByte()
        }
    }

    return uyvyData
}

private fun convertRgbaToUyvy(rgbaData: ByteArray, width: Int, height: Int): ByteArray {
    val uyvyData = ByteArray(width * height * 2)

    for (y in 0 until height) {
        for (x in 0 until width step 2) {
            val idx0 = (y * width + x) * 4
            val idx1 = (y * width + x + 1) * 4

            val r0 = rgbaData[idx0].toInt() and 0xFF
            val g0 = rgbaData[idx0 + 1].toInt() and 0xFF
            val b0 = rgbaData[idx0 + 2].toInt() and 0xFF

            val r1 = rgbaData[idx1].toInt() and 0xFF
            val g1 = rgbaData[idx1 + 1].toInt() and 0xFF
            val b1 = rgbaData[idx1 + 2].toInt() and 0xFF

            val y0 = ((66 * r0 + 129 * g0 + 25 * b0 + 128) shr 8) + 16
            val y1 = ((66 * r1 + 129 * g1 + 25 * b1 + 128) shr 8) + 16
            val u = ((-38 * r0 - 74 * g0 + 112 * b0 + 128) shr 8) + 128
            val v = ((112 * r0 - 94 * g0 - 18 * b1 + 128) shr 8) + 128

            val outIdx = y * width * 2 + x * 2
            uyvyData[outIdx] = u.toByte()
            uyvyData[outIdx + 1] = y0.toByte()
            uyvyData[outIdx + 2] = v.toByte()
            uyvyData[outIdx + 3] = y1.toByte()
        }
    }

    return uyvyData
}
