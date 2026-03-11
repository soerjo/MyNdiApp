package com.soerjo.myndicam.presentation.screen.camera

data class YuvPlaneInfo(
    val buffer: ByteArray,
    val rowStride: Int,
    val pixelStride: Int
)

data class YuvPlanes(
    val y: YuvPlaneInfo,
    val u: YuvPlaneInfo,
    val v: YuvPlaneInfo
)

data class FrameInfo(
    val data: ByteArray?,
    val yuvPlanes: YuvPlanes?,
    val width: Int,
    val height: Int,
    val format: FrameFormat,
    val fps: Int
)
