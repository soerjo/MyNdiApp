package com.soerjo.ndi

import android.util.Log
import com.soerjo.ndi.internal.NDIWrapper
import com.soerjo.ndi.model.TallyState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NDI Sender - Public API for NDI streaming
 *
 * Manages an NDI sender instance and handles tally state updates.
 * This is the main entry point for using NDI functionality.
 *
 * Usage:
 * ```kotlin
 * val sender = NDISender("My Camera")
 * sender.sendFrame(uyvyData, width, height, stride)
 * sender.tallyState.collect { state ->
 *     // Handle tally state changes
 * }
 * sender.release() // When done
 * ```
 *
 * @param sourceName The name of the NDI source (will be visible to NDI receivers)
 */
class NDISender(private val sourceName: String) : NDIWrapper.TallyCallback {
    private var senderHandle: Long = 0
    private var isRunning = false
    private var tallyCallbackRegistered = false

    private val _tallyState = MutableStateFlow(TallyState())
    val tallyState: StateFlow<TallyState> = _tallyState.asStateFlow()

    init {
        initializeNDI()
    }

    private fun initializeNDI() {
        try {
            if (!NDIWrapper.initialize()) {
                throw IllegalStateException("NDI initialization failed")
            }

            senderHandle = NDIWrapper.createSender(sourceName)
            if (senderHandle != 0L) {
                isRunning = true
                // Register this instance as the tally callback with native code
                try {
                    NDIWrapper.nativeSetTallyCallback(senderHandle, this)
                    tallyCallbackRegistered = true
                    Log.d(TAG, "Tally callback registered")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to register tally callback: ${e.message}")
                }
                Log.d(TAG, "NDI Sender initialized: $sourceName")
            } else {
                throw IllegalStateException("Failed to create NDI sender")
            }
        } catch (e: Exception) {
            Log.e(TAG, "NDI initialization failed: ${e.message}", e)
            isRunning = false
            throw e
        }
    }

    // NDIWrapper.TallyCallback implementation
    // Called from native code via JNI when tally state changes (polled at 10Hz)
    override fun onTallyStateChange(isOnPreview: Boolean, isOnProgram: Boolean) {
        _tallyState.value = TallyState(isOnPreview, isOnProgram)
        Log.d(TAG, "Tally state changed - Preview: $isOnPreview, Program (LIVE): $isOnProgram")
    }

    /**
     * Send a video frame via NDI
     *
     * @param data Frame data in UYVY format (2 bytes per pixel)
     * @param width Frame width
     * @param height Frame height
     * @param stride Frame stride (typically width * 2 for UYVY)
     * @param fps Frame rate (30 or 60)
     * @return true if frame was sent successfully, false otherwise
     */
    fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int, fps: Int = 30): Boolean {
        if (!isRunning) {
            Log.w(TAG, "Cannot send frame: sender is not running")
            return false
        }

        return try {
            NDIWrapper.sendFrame(data, width, height, stride, fps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send frame: ${e.message}", e)
            false
        }
    }

    /**
     * Send a video frame via NDI using direct ByteBuffer (optimized, zero-copy)
     *
     * This is the most efficient method for sending frames. Use direct ByteBuffers
     * allocated with ByteBuffer.allocateDirect() for best performance.
     *
     * @param buffer Direct ByteBuffer containing frame data in UYVY format (2 bytes per pixel)
     * @param width Frame width
     * @param height Frame height
     * @param stride Frame stride (typically width * 2 for UYVY)
     * @param fps Frame rate (30 or 60)
     * @return true if frame was sent successfully, false otherwise
     */
    fun sendFrameDirect(buffer: java.nio.ByteBuffer, width: Int, height: Int, stride: Int, fps: Int = 30): Boolean {
        if (!isRunning) {
            Log.w(TAG, "Cannot send frame: sender is not running")
            return false
        }

        if (!buffer.isDirect) {
            Log.w(TAG, "Buffer is not direct - this method requires a direct ByteBuffer")
            return false
        }

        return try {
            NDIWrapper.sendFrameDirect(buffer, width, height, stride, fps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send frame (direct): ${e.message}", e)
            false
        }
    }

    /**
     * Check if the sender is currently running
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Get the source name
     */
    fun getSourceName(): String = sourceName

    /**
     * Release NDI resources
     *
     * This should be called when you're done with the sender to properly
     * clean up native resources and stop the tally polling thread.
     */
    fun release() {
        try {
            isRunning = false
            if (senderHandle != 0L) {
                NDIWrapper.destroySender()
                senderHandle = 0
            }
            tallyCallbackRegistered = false
            _tallyState.value = TallyState()
            Log.d(TAG, "NDI Sender released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release NDI: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "NDISender"

        @JvmStatic
        fun convertYuv420ToNv12(
            yPlane: java.nio.ByteBuffer,
            yRowStride: Int,
            yPixelStride: Int,
            uPlane: java.nio.ByteBuffer,
            uRowStride: Int,
            uPixelStride: Int,
            vPlane: java.nio.ByteBuffer,
            vRowStride: Int,
            vPixelStride: Int,
            width: Int,
            height: Int
        ): ByteArray = NDIWrapper.nativeConvertYuv420ToNv12(
            yPlane, yRowStride, yPixelStride,
            uPlane, uRowStride, uPixelStride,
            vPlane, vRowStride, vPixelStride,
            width, height
        )

        @JvmStatic
        fun convertNv21ToNv12(
            nv21Data: ByteArray,
            width: Int,
            height: Int
        ): ByteArray = NDIWrapper.nativeConvertNv21ToNv12(
            nv21Data, width, height
        )
    }
}
