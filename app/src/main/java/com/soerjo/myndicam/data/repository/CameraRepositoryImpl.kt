package com.soerjo.myndicam.data.repository

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import com.soerjo.myndicam.data.datasource.CameraDataSource
import com.soerjo.myndicam.data.datasource.UsbCameraDataSource
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CameraRepository
 * Combines internal (CameraX) and external (USB) cameras
 */
@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val cameraDataSource: CameraDataSource,
    private val usbCameraDataSource: UsbCameraDataSource
) : CameraRepository {

    private val TAG = "CameraRepository"

    private val _availableCameras = MutableStateFlow<List<CameraInfo>>(emptyList())
    override fun getAvailableCameras(): Flow<List<CameraInfo>> = _availableCameras.asStateFlow()

    override suspend fun detectCameras(): List<CameraInfo> {
        return try {
            // Get internal cameras (CameraX)
            val internalCameras = cameraDataSource.detectAvailableCameras()
            
            // Get USB cameras
            val usbCameras = usbCameraDataSource.getDetectedCameras()
            
            // Combine both lists
            val allCameras = internalCameras + usbCameras
            _availableCameras.value = allCameras
            Log.d(TAG, "Detected ${allCameras.size} cameras (${internalCameras.size} internal, ${usbCameras.size} USB)")
            allCameras
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect cameras", e)
            emptyList()
        }
    }
}
