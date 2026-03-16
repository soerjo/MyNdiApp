package com.soerjo.myndicam.presentation.screen.camera.model

import java.nio.ByteBuffer

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
    val directBuffers: Triple<ByteBuffer, ByteBuffer, ByteBuffer>? = null,
    val strides: Triple<Int, Int, Int>? = null,
    val pixelStrides: Triple<Int, Int, Int>? = null,
    val width: Int,
    val height: Int,
    val format: FrameFormat,
    val fps: Int
)
