package com.soerjo.myndicam.core.util.conversion

import com.soerjo.myndicam.presentation.screen.camera.FrameFormat
import com.soerjo.myndicam.presentation.screen.camera.FrameInfo

fun convertToUyvy(
    data: ByteArray,
    width: Int,
    height: Int,
    format: FrameFormat
): ByteArray {
    return when (format) {
        FrameFormat.NV21 -> convertNv21ToUyvy(data, width, height)
        FrameFormat.RGBA -> convertRgbaToUyvy(data, width, height)
        FrameFormat.YUV_420_888 -> convertYuv420ToUyvy(data, width, height)
    }
}

fun convertToUyvy(frameInfo: FrameInfo): ByteArray {
    return when (frameInfo.format) {
        FrameFormat.NV21 -> {
            if (frameInfo.data != null) {
                convertNv21ToUyvy(frameInfo.data, frameInfo.width, frameInfo.height)
            } else {
                throw IllegalArgumentException("NV21 frame data is null")
            }
        }
        FrameFormat.RGBA -> {
            if (frameInfo.data != null) {
                convertRgbaToUyvy(frameInfo.data, frameInfo.width, frameInfo.height)
            } else {
                throw IllegalArgumentException("RGBA frame data is null")
            }
        }
        FrameFormat.YUV_420_888 -> {
            if (frameInfo.yuvPlanes != null) {
                convertYuv420ToUyvy(frameInfo.yuvPlanes, frameInfo.width, frameInfo.height)
            } else if (frameInfo.data != null) {
                convertYuv420ToUyvy(frameInfo.data, frameInfo.width, frameInfo.height)
            } else {
                throw IllegalArgumentException("YUV_420_888 frame data and planes are null")
            }
        }
    }
}
