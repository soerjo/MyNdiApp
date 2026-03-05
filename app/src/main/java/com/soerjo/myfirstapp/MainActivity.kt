package com.soerjo.myfirstapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.soerjo.myfirstapp.ui.theme.MyFirstAppTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class FrameRate(val fps: Int, val displayName: String) {
    FPS_30(30, "30 FPS"),
    FPS_60(60, "60 FPS");

    fun getFrameIntervalNs(): Long = 1_000_000_000L / fps

    companion object {
        fun fromFps(fps: Int): FrameRate = values().find { it.fps == fps } ?: FPS_30
    }
}

class MainActivity : ComponentActivity() {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var ndiSender: NDISender? = null

    companion object {
        private const val PREFS_NAME = "NDIPrefs"
        private const val KEY_SOURCE_NAME = "ndi_source_name"
        private const val KEY_FRAME_RATE = "frame_rate"

        const val DEFAULT_SOURCE_NAME = "Android Camera"
        val DEFAULT_FRAME_RATE = FrameRate.FPS_30

        fun getSavedSourceName(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_SOURCE_NAME, DEFAULT_SOURCE_NAME) ?: DEFAULT_SOURCE_NAME
        }

        fun saveSourceName(context: Context, name: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SOURCE_NAME, name)
                .apply()
        }

        fun getSavedFrameRate(context: Context): FrameRate {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fps = prefs.getInt(KEY_FRAME_RATE, DEFAULT_FRAME_RATE.fps)
            return FrameRate.fromFps(fps)
        }

        fun saveFrameRate(context: Context, frameRate: FrameRate) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_FRAME_RATE, frameRate.fps)
                .apply()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val wifiGranted = permissions[Manifest.permission.ACCESS_WIFI_STATE] ?: true

        if (cameraGranted) {
            Log.d("CameraApp", "Camera permission granted")
            startNDI()
        } else {
            Log.d("CameraApp", "Camera permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_WIFI_STATE)
        }

        if (permissionsToRequest.isEmpty()) {
            Log.d("CameraApp", "All permissions already granted")
            startNDI()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            MyFirstAppTheme {
                CameraScreen(ndiSender, cameraExecutor) { newSourceName ->
                    recreateNDISender(newSourceName)
                }
            }
        }
    }

    private fun startNDI() {
        val sourceName = getSavedSourceName(this)
        createNDISender(sourceName)
    }

    private fun recreateNDISender(newSourceName: String) {
        ndiSender?.release()
        createNDISender(newSourceName)
    }

    private fun createNDISender(sourceName: String) {
        try {
            if (NDIWrapper.initialize()) {
                ndiSender = NDISender(sourceName)
                Log.d("CameraApp", "NDI sender started successfully: $sourceName")
            } else {
                Log.w("CameraApp", "NDI native methods not implemented - running in stub mode")
                ndiSender = NDISender(sourceName)
            }
        } catch (e: Exception) {
            Log.e("CameraApp", "Failed to start NDI: ${e.message}")
            ndiSender = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ndiSender?.release()
        NDIWrapper.cleanup()
        cameraExecutor.shutdown()
    }
}

class NDISender(private val sourceName: String) {
    private var senderHandle: Long = 0
    private var isRunning = false
    private var lastFrameTime = 0L
    private var targetFrameIntervalNs = 1_000_000_000L / 30
    private var droppedFrames = 0

    fun setFrameRate(frameRate: FrameRate) {
        targetFrameIntervalNs = frameRate.getFrameIntervalNs()
        Log.d("NDI", "Frame rate set to ${frameRate.fps} FPS")
    }

    init {
        initializeNDI()
    }

    private fun initializeNDI() {
        try {
            senderHandle = NDIWrapper.createSender(sourceName)
            if (senderHandle != 0L) {
                isRunning = true
                Log.d("NDI", "NDI Sender initialized: $sourceName")
            } else {
                isRunning = true
                Log.w("NDI", "NDI running in stub mode")
            }
        } catch (e: Exception) {
            Log.e("NDI", "NDI initialization failed: ${e.message}")
            isRunning = true
        }
    }

    fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int) {
        if (!isRunning) return

        try {
            val currentTime = System.nanoTime()
            val timeSinceLastFrame = currentTime - lastFrameTime

            if (timeSinceLastFrame < targetFrameIntervalNs) {
                droppedFrames++
                if (droppedFrames % 30 == 0) {
                    Log.d("NDI", "Dropped $droppedFrames frames")
                }
                return
            }

            lastFrameTime = currentTime

            if (senderHandle != 0L) {
                NDIWrapper.sendFrame(data, width, height, stride)
            }
        } catch (e: Exception) {
            Log.e("NDI", "Failed to send frame: ${e.message}")
        }
    }

    fun release() {
        try {
            if (senderHandle != 0L) {
                NDIWrapper.destroySender()
                senderHandle = 0
            }
            isRunning = false
        } catch (e: Exception) {
            Log.e("NDI", "Failed to release NDI: ${e.message}")
        }
    }
}

data class CameraInfo(
    val name: String,
    val type: CameraType,
    val cameraSelectorFilter: CameraSelector
)

enum class CameraType {
    BACK,
    FRONT,
    EXTERNAL
}

fun detectCameras(cameraProvider: ProcessCameraProvider): List<CameraInfo> {
    val cameraList = mutableListOf<CameraInfo>()
    var externalCount = 0

    for (camInfo in cameraProvider.availableCameraInfos) {
        val lensFacing = camInfo.lensFacing

        when (lensFacing) {
            null -> {
                externalCount++
                val name = "External Camera $externalCount"
                val selector = CameraSelector.Builder().build()
                cameraList.add(CameraInfo(name, CameraType.EXTERNAL, selector))
            }
            CameraSelector.LENS_FACING_BACK -> {
                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                cameraList.add(CameraInfo("Back Camera", CameraType.BACK, selector))
            }
            CameraSelector.LENS_FACING_FRONT -> {
                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                cameraList.add(CameraInfo("Front Camera", CameraType.FRONT, selector))
            }
        }
    }

    return cameraList
}

@Composable
fun CameraScreen(
    ndiSender: NDISender?,
    cameraExecutor: ExecutorService,
    onSourceNameChange: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var selectedCamera by remember { mutableStateOf<CameraInfo?>(null) }
    var availableCameras by remember { mutableStateOf<List<CameraInfo>>(emptyList()) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCameraSelector by remember { mutableStateOf(false) }
    var showFrameRateSelector by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(MainActivity.getSavedSourceName(context)) }
    var selectedFrameRate by remember { mutableStateOf(MainActivity.getSavedFrameRate(context)) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val cameras = detectCameras(cameraProvider)
        availableCameras = cameras
        selectedCamera = cameras.find { it.type == CameraType.BACK } ?: cameras.firstOrNull()
    }

    LaunchedEffect(selectedFrameRate) {
        ndiSender?.setFrameRate(selectedFrameRate)
        MainActivity.saveFrameRate(context, selectedFrameRate)
    }

    LaunchedEffect(selectedCamera, isStreaming) {
        val camera = selectedCamera ?: return@LaunchedEffect
        val cameraProvider = cameraProviderFuture.get()

        val targetResolution = Size(1280, 720)

        val preview = Preview.Builder()
            .setTargetResolution(targetResolution)
            .build()
            .also {
                it.setSurfaceProvider(previewView?.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setTargetResolution(targetResolution)
            .setImageQueueDepth(4)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageForNDI(imageProxy, ndiSender, isStreaming)
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
        } catch (exc: Exception) {
            Log.e("CameraApp", "Use case binding failed: ${exc.message}", exc)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Live indicator (only when streaming)
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

        // Status bar (top right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    selectedFrameRate.displayName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "720p • 16:9",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }

        // Main streaming button (center bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    isStreaming = !isStreaming
                    Log.d("CameraApp", "NDI Streaming: ${if (isStreaming) "ON" else "OFF"}")
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

        // Menu button (top left, below live indicator)
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = if (isStreaming) 40.dp else 12.dp, start = 12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Menu",
                tint = Color.White
            )
        }
    }

    // Menu dialog
    if (showMenu) {
        Dialog(onDismissRequest = { showMenu = false }) {
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    MenuItem(
                        icon = Icons.Filled.Star,
                        title = selectedCamera?.name ?: "Select Camera",
                        onClick = {
                            showMenu = false
                            showCameraSelector = true
                        }
                    )
                    MenuItem(
                        icon = Icons.Filled.Settings,
                        title = selectedFrameRate.displayName,
                        subtitle = "Frame Rate",
                        onClick = {
                            showMenu = false
                            showFrameRateSelector = true
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.Black.copy(alpha = 0.1f)
                    )
                    MenuItem(
                        icon = Icons.Filled.Settings,
                        title = "Source Name",
                        onClick = {
                            showMenu = false
                            showSettings = true
                        }
                    )
                }
            }
        }
    }

    // Camera selector
    if (showCameraSelector && availableCameras.isNotEmpty()) {
        Dialog(onDismissRequest = { showCameraSelector = false }) {
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

                    availableCameras.forEach { camera ->
                        CameraOption(
                            name = camera.name,
                            isSelected = selectedCamera?.name == camera.name,
                            onClick = {
                                selectedCamera = camera
                                showCameraSelector = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Frame rate selector
    if (showFrameRateSelector) {
        Dialog(onDismissRequest = { showFrameRateSelector = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Frame Rate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FrameRate.values().forEach { frameRate ->
                        FrameRateOption(
                            frameRate = frameRate,
                            isSelected = selectedFrameRate == frameRate,
                            onClick = {
                                selectedFrameRate = frameRate
                                ndiSender?.setFrameRate(frameRate)
                                MainActivity.saveFrameRate(context, frameRate)
                                showFrameRateSelector = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Settings dialog
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                        value = sourceNameInput,
                        onValueChange = { sourceNameInput = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSettings = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (sourceNameInput.isNotBlank()) {
                                    MainActivity.saveSourceName(context, sourceNameInput)
                                    onSourceNameChange(sourceNameInput)
                                }
                                showSettings = false
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
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
            .clickable(onClick = onClick)
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
private fun FrameRateOption(
    frameRate: FrameRate,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    frameRate.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    when (frameRate) {
                        FrameRate.FPS_30 -> "Standard quality"
                        FrameRate.FPS_60 -> "Smoother motion"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            if (isSelected) {
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

private fun processImageForNDI(
    imageProxy: ImageProxy,
    ndiSender: NDISender?,
    isStreaming: Boolean
) {
    if (!isStreaming || ndiSender == null) {
        imageProxy.close()
        return
    }

    try {
        val width = imageProxy.width
        val height = imageProxy.height
        val planes = imageProxy.planes

        val yBuffer = planes[0].buffer
        val ySize = yBuffer.remaining()
        val yData = ByteArray(ySize)
        yBuffer.get(yData)

        val uBuffer = planes[1].buffer
        val uSize = uBuffer.remaining()
        val uData = ByteArray(uSize)
        uBuffer.get(uData)

        val vBuffer = planes[2].buffer
        val vSize = vBuffer.remaining()
        val vData = ByteArray(vSize)
        vBuffer.get(vData)

        val bgraData = yuvToBgra(yData, uData, vData, width, height,
            planes[0].rowStride, planes[1].rowStride, planes[2].rowStride,
            planes[1].pixelStride, planes[2].pixelStride)

        ndiSender.sendFrame(bgraData, width, height, width * 4)
    } catch (e: Exception) {
        Log.e("CameraApp", "Error processing image for NDI: ${e.message}")
    } finally {
        imageProxy.close()
    }
}

private fun yuvToBgra(
    yData: ByteArray, uData: ByteArray, vData: ByteArray,
    width: Int, height: Int,
    yRowStride: Int, uRowStride: Int, vRowStride: Int,
    uPixelStride: Int, vPixelStride: Int
): ByteArray {
    val bgra = ByteArray(width * height * 4)
    var bgraIndex = 0

    for (y in 0 until height) {
        for (x in 0 until width) {
            val yIndex = y * yRowStride + x
            val yValue = yData[yIndex].toInt() and 0xFF

            val uvY = y / 2
            val uvX = x / 2
            val uIndex = uvY * uRowStride + uvX * uPixelStride
            val vIndex = uvY * vRowStride + uvX * vPixelStride

            val uValue = uData[uIndex].toInt() and 0xFF
            val vValue = vData[vIndex].toInt() and 0xFF

            val c = yValue - 16
            val d = uValue - 128
            val e = vValue - 128

            var r = (c * 298 + 409 * e + 128) shr 8
            var g = (c * 298 - 100 * d - 208 * e + 128) shr 8
            var b = (c * 298 + 516 * d + 128) shr 8

            r = r.coerceIn(0, 255)
            g = g.coerceIn(0, 255)
            b = b.coerceIn(0, 255)

            bgra[bgraIndex++] = b.toByte()
            bgra[bgraIndex++] = g.toByte()
            bgra[bgraIndex++] = r.toByte()
            bgra[bgraIndex++] = 0xFF.toByte()
        }
    }

    return bgra
}
