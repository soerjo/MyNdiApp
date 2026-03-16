package com.soerjo.myndicam.presentation.screen.camera.components.preview

import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
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
import com.soerjo.myndicam.presentation.screen.camera.model.FrameFormat
import com.soerjo.myndicam.presentation.screen.camera.model.FrameInfo
import com.soerjo.myndicam.presentation.screen.camera.model.YuvPlanes
import com.soerjo.myndicam.presentation.screen.camera.model.YuvPlaneInfo
import com.soerjo.myndicam.core.util.conversion.convertToUyvy
import java.util.concurrent.Executors

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun InternalCameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraSelector: CameraSelector,
    onFrameData: (FrameInfo) -> Unit,
    modifier: Modifier = Modifier,
    targetWidth: Int = 1280,
    targetHeight: Int = 720
) {
    val context = LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val frameProcessingExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "NDI-Frame-Processor") } }

    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var currentFps by remember { mutableIntStateOf(0) }

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
                scaleType = PreviewView.ScaleType.FIT_CENTER
                previewView = this
            }
        },
        modifier = modifier
    )

    DisposableEffect(cameraSelector, targetWidth, targetHeight) {
        val selectedCamera = cameraSelector
        Log.d("InternalCameraPreview", "Binding camera: $selectedCamera")

        try {
            val provider = cameraProviderFuture.get()
            provider.unbindAll()

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
                .build();
            preview.setSurfaceProvider(previewView?.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .apply {
                    Camera2Interop.Extender(this).setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        android.util.Range(60, 60)
                    )
                }
                .build()

            imageAnalysis.setAnalyzer(
                frameProcessingExecutor
            ) { imageProxy ->
                val width = imageProxy.width
                val height = imageProxy.height

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

                val yPlane = imageProxy.planes[0].buffer
                val uPlane = imageProxy.planes[1].buffer
                val vPlane = imageProxy.planes[2].buffer

                val yRowStride = imageProxy.planes[0].rowStride
                val uRowStride = imageProxy.planes[1].rowStride
                val vRowStride = imageProxy.planes[2].rowStride

                val yPixelStride = imageProxy.planes[0].pixelStride
                val uPixelStride = imageProxy.planes[1].pixelStride
                val vPixelStride = imageProxy.planes[2].pixelStride

                val yPlaneInfo = YuvPlaneInfo(byteArrayOf(), yRowStride, yPixelStride)
                val uPlaneInfo = YuvPlaneInfo(byteArrayOf(), uRowStride, uPixelStride)
                val vPlaneInfo = YuvPlaneInfo(byteArrayOf(), vRowStride, vPixelStride)

                val frameInfo = FrameInfo(
                    data = null,
                    yuvPlanes = YuvPlanes(yPlaneInfo, uPlaneInfo, vPlaneInfo),
                    directBuffers = Triple(yPlane, uPlane, vPlane),
                    strides = Triple(yRowStride, uRowStride, vRowStride),
                    pixelStrides = Triple(yPixelStride, uPixelStride, vPixelStride),
                    width = width,
                    height = height,
                    format = FrameFormat.YUV_420_888,
                    fps = currentFps
                )

                onFrameData(frameInfo)
                imageProxy.close()
            }

            provider.bindToLifecycle(
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
