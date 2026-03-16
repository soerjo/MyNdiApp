package com.soerjo.myndicam.data.camera

sealed class CameraState {
    object Idle : CameraState()
    object Opening : CameraState()
    object Previewing : CameraState()
    data class Error(val message: String) : CameraState()
}
