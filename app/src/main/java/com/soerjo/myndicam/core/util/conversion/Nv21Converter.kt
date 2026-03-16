package com.soerjo.myndicam.core.util.conversion

internal fun convertNv21ToUyvy(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
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

internal fun convertNv21ToNv12(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
    val nv12Data = ByteArray(width * height * 3 / 2)
    val ySize = width * height
    val vuOffset = ySize
    val uvOffset = ySize

    System.arraycopy(nv21Data, 0, nv12Data, 0, ySize)

    val uvSize = ySize / 4
    for (i in 0 until uvSize step 2) {
        nv12Data[uvOffset + i] = nv21Data[vuOffset + i + 1]
        nv12Data[uvOffset + i + 1] = nv21Data[vuOffset + i]
    }

    return nv12Data
}
