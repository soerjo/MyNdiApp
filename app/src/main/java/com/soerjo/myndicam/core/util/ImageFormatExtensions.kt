package com.soerjo.myndicam.core.util

import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer

/**
 * Extension functions for image format conversions
 */

/**
 * Convert YUV_420_888 image to UYVY format for NDI
 * UYVY is a packed format (4:2:2) with 2 bytes per pixel: [U0 Y0 V0 Y1] [U2 Y2 V2 Y3] ...
 */
fun convertYuvToUyvy(image: Image): ByteArray {
    val width = image.width
    val height = image.height
    val planes = image.planes

    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val yData = ByteArray(yBuffer.remaining())
    val uData = ByteArray(uBuffer.remaining())
    val vData = ByteArray(vBuffer.remaining())

    yBuffer.get(yData)
    uBuffer.get(uData)
    vBuffer.get(vData)

    return yuvToUyvy(
        yData, uData, vData, width, height,
        planes[0].rowStride, planes[1].rowStride, planes[2].rowStride,
        planes[1].pixelStride, planes[2].pixelStride
    )
}

/**
 * Optimized YUV to UYVY conversion for NDI.
 * UYVY is a packed format (4:2:2) with 2 bytes per pixel: [U0 Y0 V0 Y1] [U2 Y2 V2 Y3] ...
 * Much faster than BGRA conversion and NDI-native.
 */
fun yuvToUyvy(
    yData: ByteArray, uData: ByteArray, vData: ByteArray,
    width: Int, height: Int,
    yRowStride: Int, uRowStride: Int, vRowStride: Int,
    uPixelStride: Int, vPixelStride: Int
): ByteArray {
    // UYVY format: 2 bytes per pixel, packed as [U0 Y0 V0 Y1] [U2 Y2 V2 Y3] ...
    val uyvy = ByteArray(width * height * 2)
    var uyvyIndex = 0

    // Process 2 pixels at a time (UYVY packs 2 pixels into 4 bytes)
    for (y in 0 until height) {
        for (x in 0 until width step 2) {
            // Get Y values for both pixels
            val yIndex1 = y * yRowStride + x
            val yIndex2 = y * yRowStride + (x + 1)
            val yValue1 = yData[yIndex1].toInt() and 0xFF
            val yValue2 = if (x + 1 < width) {
                yData[yIndex2].toInt() and 0xFF
            } else {
                yValue1 // Duplicate last pixel for odd width
            }

            // Get UV values (shared for both pixels in 4:2:2 subsampling)
            val uvY = y / 2
            val uvX = x / 2
            val uIndex = uvY * uRowStride + uvX * uPixelStride
            val vIndex = uvY * vRowStride + uvX * vPixelStride
            val uValue = uData[uIndex].toInt() and 0xFF
            val vValue = vData[vIndex].toInt() and 0xFF

            // Pack as UYVY: [U Y0 V Y1]
            uyvy[uyvyIndex++] = uValue.toByte()     // U
            uyvy[uyvyIndex++] = yValue1.toByte()    // Y0
            uyvy[uyvyIndex++] = vValue.toByte()     // V
            uyvy[uyvyIndex++] = yValue2.toByte()    // Y1
        }
    }

    return uyvy
}
