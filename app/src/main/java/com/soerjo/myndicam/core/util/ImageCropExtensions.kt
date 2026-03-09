package com.soerjo.myndicam.core.util

import android.media.Image
import java.nio.ByteBuffer

/**
 * Calculate center crop region for converting to target aspect ratio
 */
data class CropRegion(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

/**
 * Calculate center crop region for converting to target aspect ratio
 */
fun calculateCenterCrop(
    sourceWidth: Int,
    sourceHeight: Int,
    targetAspectRatio: Float = 16f / 9f
): CropRegion {
    val sourceAspectRatio = sourceWidth.toFloat() / sourceHeight.toFloat()

    return if (sourceAspectRatio > targetAspectRatio) {
        // Source is wider - crop sides
        val targetWidth = (sourceHeight * targetAspectRatio).toInt()
        val cropWidth = sourceWidth - targetWidth
        val left = cropWidth / 2
        CropRegion(left, 0, targetWidth, sourceHeight)
    } else {
        // Source is taller - crop top/bottom (our case: 4:3 -> 16:9)
        val targetHeight = (sourceWidth / targetAspectRatio).toInt()
        val cropHeight = sourceHeight - targetHeight
        val top = cropHeight / 2
        CropRegion(0, top, sourceWidth, targetHeight)
    }
}

/**
 * Crop a YUV plane buffer to the specified region
 */
private fun cropPlaneBuffer(
    buffer: ByteBuffer,
    rowStride: Int,
    pixelStride: Int,
    region: CropRegion
): ByteArray {
    val cropped = ByteArray(region.width * region.height)

    for (y in 0 until region.height) {
        val srcRowStart = (region.top + y) * rowStride + region.left * pixelStride
        val dstRowStart = y * region.width

        for (x in 0 until region.width) {
            val srcPos = srcRowStart + x * pixelStride
            val dstPos = dstRowStart + x
            if (srcPos < buffer.capacity()) {
                cropped[dstPos] = buffer.get(srcPos)
            }
        }
    }

    return cropped
}

/**
 * Convert YUV_420_888 image to UYVY with center crop to 16:9
 */
fun convertYuvToUyvyCropped(image: Image, targetAspectRatio: Float = 16f / 9f): Pair<ByteArray, CropRegion> {
    val cropRegion = calculateCenterCrop(image.width, image.height, targetAspectRatio)
    val planes = image.planes

    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    // Crop Y plane
    val croppedY = cropPlaneBuffer(
        yBuffer, planes[0].rowStride, planes[0].pixelStride, cropRegion
    )

    // Crop U/V planes (1/2 resolution for 4:2:0 subsampling)
    val uvRegion = CropRegion(
        left = cropRegion.left / 2,
        top = cropRegion.top / 2,
        width = cropRegion.width / 2,
        height = cropRegion.height / 2
    )

    val croppedU = cropPlaneBuffer(
        uBuffer, planes[1].rowStride, planes[1].pixelStride, uvRegion
    )

    val croppedV = cropPlaneBuffer(
        vBuffer, planes[2].rowStride, planes[2].pixelStride, uvRegion
    )

    // Convert cropped YUV to UYVY
    val uyvyData = yuvToUyvy(
        croppedY, croppedU, croppedV,
        cropRegion.width, cropRegion.height,
        cropRegion.width, // No padding after crop
        cropRegion.width / 2,
        cropRegion.width / 2,
        1, 1 // Pixel stride is 1 after cropping
    )

    return Pair(uyvyData, cropRegion)
}
