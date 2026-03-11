package com.soerjo.myndicam.core.common

/**
 * App constants
 */
object Constants {
    // Screen mode - change this to switch cameras:
    // 1 = USB camera preview (from UsbCameraScreen)
    // 2 = Internal camera preview (from InternalCameraScreen)
    const val SCREEN_MODE = 1

    // Camera settings
    const val TARGET_WIDTH = 1280
    const val TARGET_HEIGHT = 720
//    const val TARGET_WIDTH = 1920
//    const val TARGET_HEIGHT = 1080
    // NDI settings
    const val DEFAULT_SOURCE_NAME = "Android Camera"

    // Aspect ratio settings
    const val TARGET_ASPECT_RATIO_NUMERATOR = 16
    const val TARGET_ASPECT_RATIO_DENOMINATOR = 9
    val TARGET_ASPECT_RATIO = TARGET_ASPECT_RATIO_NUMERATOR.toFloat() / TARGET_ASPECT_RATIO_DENOMINATOR

    // Logging
    const val TAG_NDI = "NDI"
    const val TAG_CAMERA = "Camera"
    const val TAG_APP = "MyNdiCam"
}
