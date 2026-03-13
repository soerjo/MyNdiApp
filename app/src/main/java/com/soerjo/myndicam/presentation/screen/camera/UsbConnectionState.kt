package com.soerjo.myndicam.presentation.screen.camera

/**
 * USB connection state for UI tracking
 */
sealed class UsbConnectionState {
    object Idle : UsbConnectionState()
    object Connecting : UsbConnectionState()
    object Connected : UsbConnectionState()
    data class Error(val message: String) : UsbConnectionState()
}
