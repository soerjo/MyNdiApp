package com.soerjo.myndicam.presentation.screen.camera.model

sealed class NDIConnectionState {
    object NotInitialized : NDIConnectionState()
    object Initializing : NDIConnectionState()
    object Ready : NDIConnectionState()
    data class Error(val message: String) : NDIConnectionState()
}
