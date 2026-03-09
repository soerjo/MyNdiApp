package com.soerjo.myndicam.domain.repository

import com.soerjo.myndicam.domain.model.FrameRate
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app settings
 */
interface SettingsRepository {
    /**
     * Get the saved NDI source name
     */
    fun getSourceName(): Flow<String>

    /**
     * Save the NDI source name
     */
    suspend fun saveSourceName(name: String)

    /**
     * Get the saved frame rate
     */
    fun getFrameRate(): Flow<FrameRate>

    /**
     * Save the frame rate
     */
    suspend fun saveFrameRate(frameRate: FrameRate)
}
