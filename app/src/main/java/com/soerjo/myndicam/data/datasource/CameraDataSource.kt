package com.soerjo.myndicam.data.datasource

import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import com.soerjo.myndicam.domain.model.CameraInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for camera operations
 * Refactored to only support USB cameras as per requirement
 */
@Singleton
class CameraDataSource @Inject constructor() {

    private val TAG = "CameraDataSource"

    /**
     * Detect available cameras
     * Returns empty list for internal cameras as we now only use USB cameras
     */
    suspend fun detectAvailableCameras(): List<CameraInfo> {
        return emptyList()
    }

    /**
     * Detect cameras from a given ProcessCameraProvider
     * Returns empty list as internal cameras are disabled
     */
    fun detectCameras(cameraProvider: ProcessCameraProvider): List<CameraInfo.CameraX> {
        Log.d(TAG, "Internal camera detection disabled")
        return emptyList()
    }
}
