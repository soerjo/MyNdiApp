package com.soerjo.myndicam.presentation.screen.camera

/**
 * NDI connection state for UI tracking
 */
sealed class NDIConnectionState {
    object NotInitialized : NDIConnectionState()
    object Initializing : NDIConnectionState()
    object Ready : NDIConnectionState()
    data class Error(val message: String) : NDIConnectionState()
}
