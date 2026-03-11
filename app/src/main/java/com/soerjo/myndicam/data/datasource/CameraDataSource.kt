package com.soerjo.myndicam.data.datasource

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.model.CameraType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for CameraX (internal) camera operations
 */
@Singleton
class CameraDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TAG = "CameraDataSource"
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    /**
     * Detect available internal cameras (back and front)
     */
    suspend fun detectAvailableCameras(): List<CameraInfo.CameraX> = withContext(Dispatchers.IO) {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            detectCameras(cameraProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect cameras: ${e.message}")
            emptyList()
        }
    }

    /**
     * Detect cameras from a given ProcessCameraProvider
     */
    fun detectCameras(cameraProvider: ProcessCameraProvider): List<CameraInfo.CameraX> {
        val cameras = mutableListOf<CameraInfo.CameraX>()

        try {
            // Check for back camera
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                cameras.add(
                    CameraInfo.CameraX(
                        name = "Back Camera",
                        type = CameraType.BACK,
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    )
                )
                Log.d(TAG, "Back camera detected")
            }

            // Check for front camera
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                cameras.add(
                    CameraInfo.CameraX(
                        name = "Front Camera",
                        type = CameraType.FRONT,
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    )
                )
                Log.d(TAG, "Front camera detected")
            }

            Log.d(TAG, "Detected ${cameras.size} internal cameras")
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting cameras: ${e.message}")
        }

        return cameras
    }

    /**
     * Get context
     */
    fun getContext(): Context = context
}
