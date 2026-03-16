package com.soerjo.myndicam.core.util.conversion

import com.soerjo.myndicam.presentation.screen.camera.YuvPlanes

internal fun convertYuv420ToUyvy(yuvPlanes: YuvPlanes, width: Int, height: Int): ByteArray {
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

internal fun convertYuv420ToUyvy(yuvData: ByteArray, width: Int, height: Int): ByteArray {
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
