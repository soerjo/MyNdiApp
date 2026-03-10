package com.soerjo.myndicam.presentation.screen.camera

import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.usb.USBMonitor
import com.soerjo.myndicam.core.common.Constants
import com.soerjo.myndicam.data.camera.UsbCameraController
import com.soerjo.myndicam.data.datasource.UsbCameraDataSource
import com.soerjo.myndicam.presentation.fragment.UsbCameraFragment
import com.soerjo.ndi.NDIManager
import com.soerjo.ndi.NDISender
import com.soerjo.ndi.model.TallyState
import kotlinx.coroutines.launch
import androidx.fragment.app.Fragment

@Composable
fun CameraScreen() {
    when (Constants.SCREEN_MODE) {
        0 -> BlackScreen()
        1 -> NewUIScreen()
        2 -> UsbCameraScreen()
        3 -> UsbCameraFragmentScreen()
    }
}

@Composable
fun BlackScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
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
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//    )
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var usbCameraController by remember { mutableStateOf<UsbCameraController?>(null) }
    var usbPreviewTextureView by remember { mutableStateOf<TextureView?>(null) }

    var isStreaming by remember { mutableStateOf(false) }
    var sourceName by remember { mutableStateOf("USB Camera") }
    var currentWidth by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(0) }
    var currentFps by remember { mutableIntStateOf(0) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.currentTimeMillis()) }

    var ndiSender by remember { mutableStateOf<NDISender?>(null) }
    var tallyState by remember { mutableStateOf(TallyState()) }

    var isConnecting by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(sourceName) }

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

    val usbDeviceCallback = remember {
        object : IDeviceConnectCallBack {
            override fun onAttachDev(device: UsbDevice?) {
                Log.d("UsbCameraScreen", "USB device attached: ${device?.deviceName}")
            }

            override fun onDetachDec(device: UsbDevice?) {
                Log.d("UsbCameraScreen", "USB device detached: ${device?.deviceName}")
                if (usbCameraController?.getUsbDevice()?.deviceId == device?.deviceId) {
                    usbCameraController?.cleanup()
                    usbCameraController = null
                    isConnecting = false
                }
            }

            override fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                Log.d("UsbCameraScreen", "USB device connected: ${device?.deviceName}")
                device?.let { usbDevice ->
                    ctrlBlock?.let { block ->
                        val context = usbPreviewTextureView?.context ?: return@let
                        val controller = UsbCameraController(
                            context = context,
                            usbDevice = usbDevice,
                            usbControlBlock = block
                        )

                        controller.onFrameCallback = { data, width, height, stride ->
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
                                    ndiSender?.sendFrame(data, width, height, stride)
                                } catch (e: Exception) {
                                    Log.e("UsbCameraScreen", "Error sending frame: ${e.message}")
                                }
                            }
                        }

                        usbCameraController = controller
                        isConnecting = false

                        usbPreviewTextureView?.let { textureView ->
                            controller.openCamera(textureView)
                        }
                    }
                }
            }

            override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                Log.d("UsbCameraScreen", "USB device disconnected: ${device?.deviceName}")
                if (usbCameraController?.getUsbDevice()?.deviceId == device?.deviceId) {
                    usbCameraController?.closeCamera()
                    usbCameraController = null
                    isConnecting = false
                }
            }

            override fun onCancelDev(device: UsbDevice?) {
                Log.d("UsbCameraScreen", "USB device permission cancelled")
                isConnecting = false
            }
        }
    }

    val context = LocalContext.current
//    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val fragmentManager = activity?.supportFragmentManager

    LaunchedEffect(Unit) {
        try {
            val usbDataSource = UsbCameraDataSource(context)
            usbDataSource.initialize()
            usbDataSource.setDeviceConnectCallBack(usbDeviceCallback)

            val devices = usbDataSource.getDeviceList()
            if (devices.isNotEmpty()) {
                isConnecting = true
                usbDataSource.requestPermission(devices.first())
            }
        } catch (e: Exception) {
            Log.e("UsbCameraScreen", "Failed to setup USB: ${e.message}")
        }
    }

    LaunchedEffect(usbCameraController, usbPreviewTextureView) {
        val controller = usbCameraController
        val textureView = usbPreviewTextureView
        if (controller != null && textureView != null) {
            controller.openCamera(textureView)
        }
    }

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
                    if (fm.findFragmentById(fragmentContainer.id) == null) {
                        fm.beginTransaction()
                            .replace(fragmentContainer.id, UsbCameraFragment())
                            .commit()
                    }
                }
            }
        )
//        AndroidView(
//            factory = { ctx ->
//                TextureView(ctx).apply {
//                    layoutParams = ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
//                        override fun onSurfaceTextureAvailable(
//                            surface: android.graphics.SurfaceTexture,
//                            width: Int,
//                            height: Int
//                        ) {
//                            Log.d("UsbCameraScreen", "TextureView surface available")
//                            usbPreviewTextureView = this@apply
//                        }
//
//                        override fun onSurfaceTextureSizeChanged(
//                            surface: android.graphics.SurfaceTexture,
//                            width: Int,
//                            height: Int
//                        ) {}
//
//                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
//                            usbPreviewTextureView = null
//                            return true
//                        }
//
//                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
//                    }
//                }
//            },
//            modifier = Modifier.fillMaxSize()
//        )

        if (isConnecting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

//        if (!isConnecting && usbCameraController == null && currentHeight == 0) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f))
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Text(
//                        "Waiting for USB Camera...",
//                        color = Color.White.copy(alpha = 0.7f),
//                        fontSize = 16.sp
//                    )
//                }
//            }
//        }

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
