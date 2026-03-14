package com.soerjo.myndicam.presentation.screen.camera

import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.soerjo.myndicam.presentation.screen.camera.FrameFormat
import com.soerjo.myndicam.presentation.screen.camera.FrameInfo
import com.soerjo.myndicam.presentation.screen.camera.YuvPlanes
import com.soerjo.myndicam.presentation.screen.camera.YuvPlaneInfo
import com.soerjo.myndicam.presentation.screen.camera.convertToUyvy

@Composable
fun InternalCameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraSelector: CameraSelector,
    onFrameData: (FrameInfo) -> Unit,
    modifier: Modifier = Modifier,
    targetWidth: Int = 1920,
    targetHeight: Int = 1080
) {
    val context = LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val frameProcessingExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "NDI-Frame-Processor") } }

    // FPS tracking
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var currentFps by remember { mutableIntStateOf(0) }

    // Resolution tracking
    var currentWidth by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            frameProcessingExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewView = this
            }
        },
        modifier = modifier
    )

    DisposableEffect(cameraSelector, targetWidth, targetHeight) {
        val selectedCamera = cameraSelector
        Log.d("InternalCameraPreview", "Binding camera: $selectedCamera")

        try {
            val cameraProvider = cameraProviderFuture.get()

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(targetWidth, targetHeight),
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
                        val width = imageProxy.width
                        val height = imageProxy.height

                        // Update resolution tracking
                        if (width != currentWidth || height != currentHeight) {
                            currentWidth = width
                            currentHeight = height
                        }

                        // Update FPS tracking
                        frameCount++
                        val now = System.currentTimeMillis()
                        if (now - lastFpsTime >= 1000) {
                            currentFps = frameCount
                            frameCount = 0
                            lastFpsTime = now
                        }

                        // Extract YUV data from ImageProxy
                        val planes = imageProxy.planes
                        val yBuffer = planes[0].buffer
                        val uBuffer = planes[1].buffer
                        val vBuffer = planes[2].buffer

                        val yRowStride = planes[0].rowStride
                        val uRowStride = planes[1].rowStride
                        val vRowStride = planes[2].rowStride

                        val yPixelStride = planes[0].pixelStride
                        val uPixelStride = planes[1].pixelStride
                        val vPixelStride = planes[2].pixelStride

                        // Extract to ByteArray
                        val yData = if (yBuffer.hasArray()) yBuffer.array() else ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }
                        val uData = if (uBuffer.hasArray()) uBuffer.array() else ByteArray(uBuffer.remaining()).also { uBuffer.get(it) }
                        val vData = if (vBuffer.hasArray()) vBuffer.array() else ByteArray(vBuffer.remaining()).also { vBuffer.get(it) }

                        val yPlaneInfo = YuvPlaneInfo(yData, yRowStride, yPixelStride)
                        val uPlaneInfo = YuvPlaneInfo(uData, uRowStride, uPixelStride)
                        val vPlaneInfo = YuvPlaneInfo(vData, vRowStride, vPixelStride)

                        val frameInfo = FrameInfo(
                            data = null,
                            yuvPlanes = YuvPlanes(yPlaneInfo, uPlaneInfo, vPlaneInfo),
                            width = width,
                            height = height,
                            format = FrameFormat.YUV_420_888,
                            fps = currentFps
                        )

                        onFrameData(frameInfo)
                        imageProxy.close()
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selectedCamera,
                preview,
                imageAnalysis
            )

            Log.d("InternalCameraPreview", "Camera bound successfully")

        } catch (e: Exception) {
            Log.e("InternalCameraPreview", "Camera binding failed: ${e.message}", e)
        }

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Log.e("InternalCameraPreview", "Failed to unbind camera: ${e.message}")
            }
        }
    }
}


