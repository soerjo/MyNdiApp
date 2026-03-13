package com.soerjo.myndicam.core.util

/**
 * Extension functions for USB camera image format conversions
 */

/**
 * Convert YUYV to UYVY format for NDI
 * YUYV format: [Y0 U0 Y1 V0] [Y2 U2 Y3 V2] ...
 * UYVY format: [U0 Y0 V0 Y1] [U2 Y2 V2 Y3] ...
 */
fun convertYuyvToUyvy(yuyvData: ByteArray, width: Int, height: Int): ByteArray {
    val uyvyData = ByteArray(width * height * 2)
    var yuyvIndex = 0
    var uyvyIndex = 0

    for (i in 0 until (width * height * 2) step 4) {
        val y0 = yuyvData[yuyvIndex++].toInt() and 0xFF
        val u = yuyvData[yuyvIndex++].toInt() and 0xFF
        val y1 = yuyvData[yuyvIndex++].toInt() and 0xFF
        val v = yuyvData[yuyvIndex++].toInt() and 0xFF

        uyvyData[uyvyIndex++] = u.toByte()
        uyvyData[uyvyIndex++] = y0.toByte()
        uyvyData[uyvyIndex++] = v.toByte()
        uyvyData[uyvyIndex++] = y1.toByte()
    }
    return uyvyData
}

/**
 * Convert NV21 to UYVY
 */
fun convertNv21ToUyvy(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
    val uyvyData = ByteArray(width * height * 2)
    val ySize = width * height
    val vuOffset = ySize

    for (y in 0 until height step 2) {
        for (x in 0 until width step 2) {
            // Get Y values for 2x2 block
            val yIndex00 = y * width + x
            val yIndex01 = y * width + (x + 1)
            val yIndex10 = (y + 1) * width + x
            val yIndex11 = (y + 1) * width + (x + 1)

            val y00 = nv21Data[yIndex00].toInt() and 0xFF
            val y01 = nv21Data[yIndex01].toInt() and 0xFF
            val y10 = nv21Data[yIndex10].toInt() and 0xFF
            val y11 = nv21Data[yIndex11].toInt() and 0xFF

            // Get VU values (common for 2x2 block in 4:2:0)
            val vuIndex = vuOffset + (y / 2) * width + x
            val v = nv21Data[vuIndex].toInt() and 0xFF
            val u = nv21Data[vuIndex + 1].toInt() and 0xFF

            // Pack as UYVY: [U Y0 V Y1] for first row
            uyvyData[y * width * 2 + x * 2] = u.toByte()
            uyvyData[y * width * 2 + x * 2 + 1] = y00.toByte()
            uyvyData[y * width * 2 + (x + 1) * 2] = v.toByte()
            uyvyData[y * width * 2 + (x + 1) * 2 + 1] = y01.toByte()

            // Pack as UYVY for second row
            uyvyData[(y + 1) * width * 2 + x * 2] = u.toByte()
            uyvyData[(y + 1) * width * 2 + x * 2 + 1] = y10.toByte()
            uyvyData[(y + 1) * width * 2 + (x + 1) * 2] = v.toByte()
            uyvyData[(y + 1) * width * 2 + (x + 1) * 2 + 1] = y11.toByte()
        }
    }
    return uyvyData
}
