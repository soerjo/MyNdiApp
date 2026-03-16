package com.soerjo.myndicam.domain.model

enum class Resolution(val width: Int, val height: Int, val displayName: String) {
    HD(1280, 720, "HD (720p)"),
    FULL_HD(1920, 1080, "Full HD (1080p)");

    companion object {
        fun fromDimensions(width: Int, height: Int): Resolution = 
            values().find { it.width == width && it.height == height } ?: FULL_HD
    }
}
