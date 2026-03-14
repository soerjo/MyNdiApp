package com.soerjo.myndicam.presentation.screen.camera

import android.util.Log

/**
 * Unified format conversion function for all camera formats
 * Converts input format to UYVY for NDI streaming
 *
 * Note: Assumes packed data with no stride padding
 */
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

/**
 * Unified format conversion function with plane information support
 * Converts input format to UYVY for NDI streaming
 *
 * This version uses explicit plane information for robust YUV_420_888 handling
 */
fun convertToUyvy(
    frameInfo: FrameInfo
): ByteArray {
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
                Log.w("FrameConverter", "Using legacy packed YUV conversion for YUV_420_888")
                convertYuv420ToUyvy(frameInfo.data, frameInfo.width, frameInfo.height)
            } else {
                throw IllegalArgumentException("YUV_420_888 frame data and planes are null")
            }
        }
    }
}

/**
 * Convert NV21 to UYVY
 * NV21: Y plane followed by VU interleaved plane
 * UYVY: Packed 4:2:2 format [U0 Y0 V0 Y1] [U2 Y2 V2 Y3] ...
 */
private fun convertNv21ToUyvy(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
    val uyvyData = ByteArray(width * height * 2)

    val ySize = width * height
    val vuOffset = ySize

    for (y in 0 until height step 2) {
        for (x in 0 until width step 2) {
            val yIndex00 = y * width + x
            val yIndex01 = y * width + (x + 1)
            val yIndex10 = (y + 1) * width + x
            val yIndex11 = (y + 1) * width + (x + 1)

            val y00 = nv21Data[yIndex00].toInt() and 0xFF
            val y01 = nv21Data[yIndex01].toInt() and 0xFF
            val y10 = nv21Data[yIndex10].toInt() and 0xFF
            val y11 = nv21Data[yIndex11].toInt() and 0xFF

            val vuIndex = vuOffset + (y / 2) * width + x
            val v = nv21Data[vuIndex].toInt() and 0xFF
            val u = nv21Data[vuIndex + 1].toInt() and 0xFF

            uyvyData[y * width * 2 + x * 2] = u.toByte()
            uyvyData[y * width * 2 + x * 2 + 1] = y00.toByte()
            uyvyData[y * width * 2 + (x + 1) * 2] = v.toByte()
            uyvyData[(y + 1) * width * 2 + x * 2] = u.toByte()
            uyvyData[(y + 1) * width * 2 + (x + 1) * 2] = y01.toByte()
            uyvyData[(y + 1) * width * 2 + x * 2 + 1] = y10.toByte()
            uyvyData[(y + 1) * width * 2 + x * 2 + 1] = y11.toByte()
        }
    }

    return uyvyData
}

/**
 * Convert RGBA to UYVY
 * RGBA: Red-Green-Blue-Alpha (4 bytes per pixel)
 * UYVY: Packed 4:2:2 format [U0 Y0 V0 Y1] [U2 Y2 V2 Y3] ...
 */
private fun convertRgbaToUyvy(rgbaData: ByteArray, width: Int, height: Int): ByteArray {
    val uyvyData = ByteArray(width * height * 2)

    for (y in 0 until height) {
        for (x in 0 until width step 2) {
            val idx0 = (y * width + x) * 4
            val idx1 = (y * width + x + 1) * 4

            val r0 = rgbaData[idx0].toInt() and 0xFF
            val g0 = rgbaData[idx0 + 1].toInt() and 0xFF
            val b0 = rgbaData[idx0 + 2].toInt() and 0xFF

            val r1 = rgbaData[idx1].toInt() and 0xFF
            val g1 = rgbaData[idx1 + 1].toInt() and 0xFF
            val b1 = rgbaData[idx1 + 2].toInt() and 0xFF

            val y0 = ((66 * r0 + 129 * g0 + 25 * b0 + 128) shr 8) + 16
            val y1 = ((66 * r1 + 129 * g1 + 25 * b1 + 128) shr 8) + 16
            val u = ((-38 * r0 - 74 * g0 + 112 * b0 + 128) shr 8) + 128
            val v = ((112 * r0 - 94 * g0 - 18 * b1 + 128) shr 8) + 128

            val outIdx = y * width * 2 + x * 2
            uyvyData[outIdx] = u.toByte()
            uyvyData[outIdx + 1] = y0.toByte()
            uyvyData[outIdx + 2] = v.toByte()
            uyvyData[outIdx + 3] = y1.toByte()
        }
    }

    return uyvyData
}

/**
 * Convert YUV_420_888 to UYVY using explicit plane information
 * Input: YuvPlanes with proper stride handling
 * Output: Packed UYVY format for NDI
 */
private fun convertYuv420ToUyvy(yuvPlanes: YuvPlanes, width: Int, height: Int): ByteArray {
    val uyvyData = ByteArray(width * height * 2)
    var uyvyIndex = 0

    val yPlane = yuvPlanes.y
    val uPlane = yuvPlanes.u
    val vPlane = yuvPlanes.v

    for (y in 0 until height) {
        for (x in 0 until width step 2) {
            val yIdx1 = y * yPlane.rowStride + x * yPlane.pixelStride
            val yIdx2 = y * yPlane.rowStride + (x + 1) * yPlane.pixelStride

            val yValue1 = yPlane.buffer[yIdx1].toInt() and 0xFF
            val yValue2 = yPlane.buffer[yIdx2].toInt() and 0xFF

            val uvY = y / 2
            val uvX = x / 2

            val uIdx = uvY * uPlane.rowStride + uvX * uPlane.pixelStride
            val vIdx = uvY * vPlane.rowStride + uvX * vPlane.pixelStride

            val uValue = uPlane.buffer[uIdx].toInt() and 0xFF
            val vValue = vPlane.buffer[vIdx].toInt() and 0xFF

            uyvyData[uyvyIndex++] = uValue.toByte()
            uyvyData[uyvyIndex++] = yValue1.toByte()
            uyvyData[uyvyIndex++] = vValue.toByte()
            uyvyData[uyvyIndex++] = yValue2.toByte()
        }
    }

    return uyvyData
}

/**
 * Convert YUV_420_888 to UYVY (legacy packed format)
 * Input: Packed ByteArray with Y plane, U plane, V plane concatenated (no stride padding)
 * Output: Packed UYVY format for NDI
 */
private fun convertYuv420ToUyvy(yuvData: ByteArray, width: Int, height: Int): ByteArray {
    val ySize = width * height
    val uvSize = ySize / 4

    val uyvyData = ByteArray(width * height * 2)
    var uyvyIndex = 0

    val uvWidth = width / 2

    for (y in 0 until height) {
        for (x in 0 until width step 2) {
            val yIndex1 = y * width + x
            val yIndex2 = y * width + (x + 1)
            val yValue1 = yuvData[yIndex1].toInt() and 0xFF
            val yValue2 = yuvData[yIndex2].toInt() and 0xFF

            val uvY = y / 2
            val uvX = x / 2
            val uIndex = ySize + uvY * uvWidth + uvX
            val vIndex = ySize + uvSize + uvY * uvWidth + uvX

            val uValue = yuvData[uIndex].toInt() and 0xFF
            val vValue = yuvData[vIndex].toInt() and 0xFF

            uyvyData[uyvyIndex++] = uValue.toByte()
            uyvyData[uyvyIndex++] = yValue1.toByte()
            uyvyData[uyvyIndex++] = vValue.toByte()
            uyvyData[uyvyIndex++] = yValue2.toByte()
        }
    }

    return uyvyData
}
