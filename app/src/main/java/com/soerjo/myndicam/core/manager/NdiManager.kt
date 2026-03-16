package com.soerjo.myndicam.core.manager

import android.util.Log
import com.soerjo.myndicam.presentation.screen.camera.model.NDIConnectionState
import com.soerjo.ndi.NDIManager
import com.soerjo.ndi.NDISender
import com.soerjo.ndi.model.TallyState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NdiManager(
    private val sourceName: String,
    private val onConnectionStateChanged: (NDIConnectionState) -> Unit
) {
    private val TAG = "NdiManager"

    private var ndiSender: NDISender? = null
    @Volatile private var isStreaming = false

    private val _connectionState = MutableStateFlow<NDIConnectionState>(NDIConnectionState.NotInitialized)
    val connectionState: StateFlow<NDIConnectionState> = _connectionState.asStateFlow()

    fun initialize() {
        try {
            _connectionState.value = NDIConnectionState.Initializing

            if (!NDIManager.isInitialized()) {
                val initialized = NDIManager.initialize()
                if (initialized) {
                    _connectionState.value = NDIConnectionState.Ready
                    Log.d(TAG, "NDI initialized successfully")
                } else {
                    _connectionState.value = NDIConnectionState.Error("Failed to initialize NDI engine")
                    Log.e(TAG, "Failed to initialize NDI")
                }
            } else {
                _connectionState.value = NDIConnectionState.Ready
            }
        } catch (e: Exception) {
            _connectionState.value = NDIConnectionState.Error(e.message ?: "Unknown error")
            Log.e(TAG, "Error initializing NDI", e)
        }
    }

    fun createSender() {
        try {
            release()

            ndiSender = NDIManager.createSender(sourceName)
            _connectionState.value = NDIConnectionState.Ready
            Log.d(TAG, "NDI sender created: $sourceName")

            onConnectionStateChanged(NDIConnectionState.Ready)
        } catch (e: Exception) {
            _connectionState.value = NDIConnectionState.Error(e.message ?: "Failed to create sender")
            Log.e(TAG, "Failed to create NDI sender", e)
        }
    }

    fun observeTally(onTallyChanged: (TallyState) -> Unit): StateFlow<TallyState>? {
        return ndiSender?.tallyState
    }

    fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int) {
        if (!isStreaming) return

        try {
            ndiSender?.sendFrame(data, width, height, stride)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send frame", e)
        }
    }

    fun setStreaming(streaming: Boolean) {
        isStreaming = streaming
        Log.d(TAG, "Streaming: ${if (isStreaming) "ON" else "OFF"}")
    }

    fun isStreaming(): Boolean = isStreaming

    fun release() {
        try {
            isStreaming = false
            ndiSender?.release()
            ndiSender = null
            Log.d(TAG, "NDI released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing NDI", e)
        }
    }
}
