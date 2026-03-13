package com.soerjo.myndicam.core.util

import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer

/**
 * Simple buffer pool for reusing ByteArrays to reduce GC pressure
 * Thread-safe for single-producer, single-consumer (camera frame processing)
 */
object UyvyBufferPool {
    private const val MAX_POOL_SIZE = 3  // Keep a few buffers to handle async sending
    private val pool = mutableListOf<ByteArray>()
    private var currentBuffer: ByteArray? = null
    private var currentSize = 0

    @Synchronized
    fun obtain(size: Int): ByteArray {
        // Return existing buffer if it's the right size
        if (currentBuffer != null && currentSize == size) {
            return currentBuffer!!
        }

        // Try to find a matching buffer in the pool
        val pooled = pool.findFirst { it.size >= size }
        if (pooled != null) {
            pool.remove(pooled)
            currentBuffer = pooled
            currentSize = size
            return pooled
        }

        // Create new buffer
        val newBuffer = ByteArray(size)
        currentBuffer = newBuffer
        currentSize = size
        return newBuffer
    }

    @Synchronized
    fun recycle(buffer: ByteArray) {
        if (pool.size < MAX_POOL_SIZE && !pool.contains(buffer)) {
            pool.add(buffer)
        }
    }

    private inline fun <T> List<T>.findFirst(predicate: (T) -> Boolean): T? {
        for (item in this) {
            if (predicate(item)) return item
        }
        return null
    }
}

/**
 * Extension functions for image format conversions
 */

/**
 * Original YUV to UYVY conversion (kept for reference/crop variant)
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
