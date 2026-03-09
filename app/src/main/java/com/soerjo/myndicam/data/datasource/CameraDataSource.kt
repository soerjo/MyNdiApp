package com.soerjo.myndicam.data.datasource

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.model.CameraType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for camera operations
 */
@Singleton
class CameraDataSource @Inject constructor() {

    private val TAG = "CameraDataSource"

    /**
     * Detect available cameras from the camera provider
     */
    suspend fun detectAvailableCameras(): List<CameraInfo> {
        // This will be called with a valid camera provider from the ViewModel
        // For now, return empty list - the actual detection happens in the ViewModel
        // where we have access to ProcessCameraProvider
        return emptyList()
    }

    /**
     * Detect cameras from a given ProcessCameraProvider
     */
    fun detectCameras(cameraProvider: ProcessCameraProvider): List<CameraInfo> {
        val cameraList = mutableListOf<CameraInfo>()
        var externalCount = 0

        for (camInfo in cameraProvider.availableCameraInfos) {
            val lensFacing = camInfo.lensFacing

            when {
                lensFacing == null -> {
                    externalCount++
                    val name = "External Camera $externalCount"
                    val selector = CameraSelector.Builder().build()
                    cameraList.add(CameraInfo(name, CameraType.EXTERNAL, selector))
                }
                lensFacing == CameraSelector.LENS_FACING_BACK -> {
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                    cameraList.add(CameraInfo("Back Camera", CameraType.BACK, selector))
                }
                lensFacing == CameraSelector.LENS_FACING_FRONT -> {
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()
                    cameraList.add(CameraInfo("Front Camera", CameraType.FRONT, selector))
                }
            }
        }

        Log.d(TAG, "Detected ${cameraList.size} cameras")
        return cameraList
    }
}
