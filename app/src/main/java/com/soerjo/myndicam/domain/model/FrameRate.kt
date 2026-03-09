package com.soerjo.myndicam.domain.model

/**
 * Supported frame rates for streaming
 */
enum class FrameRate(val fps: Int, val displayName: String) {
    FPS_30(30, "30 FPS"),
    FPS_60(60, "60 FPS");

    fun getFrameIntervalNs(): Long = 1_000_000_000L / fps

    companion object {
        fun fromFps(fps: Int): FrameRate = values().find { it.fps == fps } ?: FPS_30
    }
}
