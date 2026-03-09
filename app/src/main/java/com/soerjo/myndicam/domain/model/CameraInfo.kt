package com.soerjo.myndicam.domain.model

import androidx.camera.core.CameraSelector

/**
 * Domain model representing a camera device
 */
data class CameraInfo(
    val name: String,
    val type: CameraType,
    val cameraSelectorFilter: CameraSelector
)
