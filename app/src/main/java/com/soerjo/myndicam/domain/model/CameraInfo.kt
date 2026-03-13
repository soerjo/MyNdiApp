package com.soerjo.myndicam.domain.model

import androidx.camera.core.CameraSelector

/**
 * Domain model representing a camera device
 * Sealed class to support both CameraX and USB cameras
 */
sealed class CameraInfo {
    abstract val name: String
    abstract val type: CameraType

    /**
     * CameraX camera (front/back/external)
     */
    data class CameraX(
        override val name: String,
        override val type: CameraType,
        val cameraSelector: CameraSelector
    ) : CameraInfo()

    /**
     * USB camera (UVC device)
     */
    data class Usb(
        val deviceId: Int,
        override val name: String,
        val vendorId: Int,
        val productId: Int
    ) : CameraInfo() {
        override val type: CameraType = CameraType.EXTERNAL
    }
}
