package com.soerjo.myndicam.core.util.conversion

internal fun convertRgbaToUyvy(rgbaData: ByteArray, width: Int, height: Int): ByteArray {
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
