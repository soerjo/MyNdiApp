package com.soerjo.myndicam.presentation.screen.camera.model

sealed class UsbConnectionState {
    object Idle : UsbConnectionState()
    object Connecting : UsbConnectionState()
    object Connected : UsbConnectionState()
    data class Error(val message: String) : UsbConnectionState()
}
