package com.soerjo.myndicam.data.repository

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import com.soerjo.myndicam.data.datasource.CameraDataSource
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CameraRepository
 */
@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val cameraDataSource: CameraDataSource
) : CameraRepository {

    private val TAG = "CameraRepository"

    private val _availableCameras = MutableStateFlow<List<CameraInfo>>(emptyList())
    override fun getAvailableCameras(): Flow<List<CameraInfo>> = _availableCameras.asStateFlow()

    override suspend fun detectCameras(): List<CameraInfo> {
        return try {
            val cameras = cameraDataSource.detectAvailableCameras()
            _availableCameras.value = cameras
            Log.d(TAG, "Detected ${cameras.size} cameras")
            cameras
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect cameras", e)
            emptyList()
        }
    }
}
