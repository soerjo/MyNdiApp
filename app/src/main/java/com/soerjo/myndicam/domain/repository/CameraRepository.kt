package com.soerjo.myndicam.domain.repository

import com.soerjo.myndicam.domain.model.CameraInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for camera operations
 */
interface CameraRepository {
    /**
     * Get available cameras
     */
    fun getAvailableCameras(): Flow<List<CameraInfo>>

    /**
     * Detect all available cameras
     */
    suspend fun detectCameras(): List<CameraInfo>
}
