package com.soerjo.myndicam.presentation.screen.camera

enum class FrameFormat {
    NV21,       // USB camera format
    RGBA,       // USB camera format
    YUV_420_888 // Internal camera format (CameraX)
}
