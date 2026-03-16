package com.soerjo.myndicam.data.camera.internal

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.soerjo.ndi.NDISender
import com.soerjo.myndicam.data.camera.CameraState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InternalCameraController(
    private val context: Context,
    private val cameraSelector: CameraSelector,
    private val lifecycleOwner: LifecycleOwner
) {
    private val TAG = "InternalCameraController"

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _previewSize = MutableStateFlow(Size(1920, 1080))
    val previewSize: StateFlow<Size> = _previewSize.asStateFlow()

    var onFrameCallback: ((ByteArray, Int, Int, Int) -> Unit)? = null

    @SuppressLint("UnsafeOptInUsageError")
    fun startCamera(previewView: PreviewView) {
        _cameraState.value = CameraState.Opening
        Log.d(TAG, "Starting internal camera: $cameraSelector")

        cameraExecutor.execute {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "ProcessCameraProvider obtained")

                preview = CameraHelper.createPreview()
                Log.d(TAG, "Preview use case built")

                imageAnalysis = CameraHelper.createImageAnalysis(1920, 1080, 60)
                Log.d(TAG, "ImageAnalysis use case built")

                imageAnalysis?.setAnalyzer(analysisExecutor) { imageProxy ->
                    processImage(imageProxy)
                    imageProxy.close()
                }
                Log.d(TAG, "Analyzer set")

                preview?.setSurfaceProvider(previewView.surfaceProvider)
                Log.d(TAG, "Surface provider set")

                cameraProvider?.unbindAll()
                Log.d(TAG, "Unbound all use cases")

                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                Log.d(TAG, "Bound to lifecycle")

                ContextCompat.getMainExecutor(context).execute {
                    _cameraState.value = CameraState.Previewing
                    Log.i(TAG, "Camera started successfully: $cameraSelector")
                }

            } catch (e: Exception) {
                ContextCompat.getMainExecutor(context).execute {
                    _cameraState.value = CameraState.Error("Failed to start camera: ${e.message}")
                }
                Log.e(TAG, "Error starting camera", e)
            }
        }
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            _cameraState.value = CameraState.Idle
            Log.d(TAG, "Camera stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
    }

    fun switchCamera(newCameraSelector: CameraSelector) {
        Log.d(TAG, "Switch camera to: $newCameraSelector")
    }

    private fun processImage(imageProxy: ImageProxy) {
        try {
            val image = imageProxy.image ?: return
            val width = image.width
            val height = image.height

            val yPlane = image.planes[0].buffer
            val uPlane = image.planes[1].buffer
            val vPlane = image.planes[2].buffer

            val yRowStride = image.planes[0].rowStride
            val yPixelStride = image.planes[0].pixelStride
            val uRowStride = image.planes[1].rowStride
            val uPixelStride = image.planes[1].pixelStride
            val vRowStride = image.planes[2].rowStride
            val vPixelStride = image.planes[2].pixelStride

            val nv12Data = NDISender.convertYuv420ToNv12(
                yPlane, yRowStride, yPixelStride,
                uPlane, uRowStride, uPixelStride,
                vPlane, vRowStride, vPixelStride,
                width, height
            )
            val stride = width

            onFrameCallback?.invoke(nv12Data, width, height, stride)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}", e)
        }
    }

    fun getCameraSelector(): CameraSelector = cameraSelector

    fun cleanup() {
        stopCamera()
        cameraExecutor.shutdown()
        analysisExecutor.shutdown()
        onFrameCallback = null
        cameraProvider = null
        preview = null
        imageAnalysis = null
    }
}
