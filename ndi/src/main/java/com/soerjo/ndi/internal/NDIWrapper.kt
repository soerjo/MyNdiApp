package com.soerjo.ndi.internal

import android.util.Log
import java.nio.ByteBuffer

/**
 * NDI Native Wrapper (Internal)
 *
 * This class provides JNI bindings to the native NDI library (libndi.so).
 * The native library must be present in jniLibs/<abi>/libndi.so
 *
 * NDI TALLY SUPPORT:
 * The wrapper includes infrastructure for NDI tally callbacks. When OBS (or any NDI receiver)
 * puts the source on air/preview, it sends tally information back to the source.
 *
 * The native implementation polls for tally state changes at 10Hz and calls the registered
 * callback when the state changes.
 *
 * This is an internal API and should not be used directly. Use NDISender instead.
 */
object NDIWrapper {
    private const val TAG = "NDIWrapper"
    private var isInitialized = false
    private var nativeHandle: Long = 0

    init {
        try {
            // Load our JNI wrapper library
            System.loadLibrary("ndi_wrapper")
            Log.d(TAG, "Successfully loaded libndi_wrapper.so")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "libndi_wrapper.so not found or JNI methods not implemented", e)
        }
    }

    /**
     * Interface for NDI tally state changes.
     * Called when OBS or any NDI receiver changes the tally state.
     *
     * The native implementation polls for tally state changes at 10Hz and calls
     * this method when either the preview or program state changes.
     */
    interface TallyCallback {
        /**
         * Called when tally state changes.
         * @param isOnPreview true when source is in preview (e.g., OBS preview window)
         * @param isOnProgram true when source is live/on-air (e.g., OBS program output)
         */
        fun onTallyStateChange(isOnPreview: Boolean, isOnProgram: Boolean)
    }

    /**
     * Set the tally callback to receive state changes from NDI receivers.
     * This should be called after creating a sender.
     *
     * The native implementation starts a polling thread at 10Hz that monitors
     * the tally state and calls the registered callback when either state changes.
     *
     * @param handle Native handle to the sender
     * @param callback Object implementing TallyCallback interface
     */
    external fun nativeSetTallyCallback(handle: Long, callback: TallyCallback)

    /**
     * Initialize the NDI library
     * @return true if successful, false otherwise
     */
    external fun nativeInitialize(): Boolean

    /**
     * Create an NDI sender
     * @param sourceName The name of the NDI source
     * @return Native handle to the sender (0 if failed)
     */
    external fun nativeCreateSender(sourceName: String): Long

    /**
     * Send a video frame via NDI (optimized with async send and buffer pooling)
     * @param handle Native handle to the sender
     * @param data Frame data (YUV format)
     * @param width Frame width
     * @param height Frame height
     * @param stride Frame stride
     * @return true if successful, false otherwise
     */
    external fun nativeSendFrame(handle: Long, data: ByteArray, width: Int, height: Int, stride: Int, fps: Int): Boolean

    /**
     * Send a video frame via NDI using direct ByteBuffer (zero-copy)
     * This is the most efficient method for sending frames as it avoids any memory copying.
     *
     * @param handle Native handle to the sender
     * @param buffer Direct ByteBuffer containing frame data
     * @param width Frame width
     * @param height Frame height
     * @param stride Frame stride
     * @return true if successful, false otherwise
     */
    external fun nativeSendFrameDirect(handle: Long, buffer: java.nio.ByteBuffer, width: Int, height: Int, stride: Int, fps: Int): Boolean

    /**
     * Convert YUV_420_888 format to NV12 format
     * Uses native NEON-optimized conversion for maximum performance
     *
     * @param yPlane Y plane ByteBuffer
     * @param yRowStride Y plane row stride in bytes
     * @param yPixelStride Y plane pixel stride
     * @param uPlane U plane ByteBuffer
     * @param uRowStride U plane row stride in bytes
     * @param uPixelStride U plane pixel stride
     * @param vPlane V plane ByteBuffer
     * @param vRowStride V plane row stride in bytes
     * @param vPixelStride V plane pixel stride
     * @param width Frame width
     * @param height Frame height
     * @return NV12 format ByteArray (Y plane + interleaved UV plane)
     */
    @JvmStatic
    external fun nativeConvertYuv420ToNv12(
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
    ): ByteArray

    /**
     * Convert NV21 format to NV12 format (native optimized)
     * NV21 has VUVU ordering, NV12 has UVUV ordering
     *
     * @param nv21Data NV21 format ByteArray
     * @param width Frame width
     * @param height Frame height
     * @return NV12 format ByteArray
     */
    @JvmStatic
    external fun nativeConvertNv21ToNv12(
        nv21Data: ByteArray,
        width: Int,
        height: Int
    ): ByteArray

    /**
     * Destroy an NDI sender
     * @param handle Native handle to the sender
     */
    external fun nativeDestroySender(handle: Long)

    /**
     * Cleanup NDI resources
     */
    external fun nativeCleanup()

    // Kotlin wrapper methods
    fun initialize(): Boolean {
        if (!isInitialized) {
            isInitialized = nativeInitialize()
            if (isInitialized) {
                Log.d(TAG, "NDI initialized successfully")
            } else {
                Log.w(TAG, "NDI initialization failed")
            }
        }
        return isInitialized
    }

    fun createSender(sourceName: String): Long {
        if (!isInitialized) {
            Log.w(TAG, "NDI not initialized")
        }
        val handle = nativeCreateSender(sourceName)
        if (handle != 0L) {
            nativeHandle = handle
            Log.d(TAG, "NDI sender created: $sourceName (handle: $handle)")
        } else {
            Log.e(TAG, "NDI sender creation failed")
        }
        return handle
    }

    fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int, fps: Int): Boolean {
        if (nativeHandle == 0L) {
            Log.w(TAG, "No valid sender handle")
            return false
        }
        return nativeSendFrame(nativeHandle, data, width, height, stride, fps)
    }

    /**
     * Send a video frame using direct ByteBuffer (zero-copy, optimized)
     * This is most efficient method - use this when possible.
     *
     * @param buffer Direct ByteBuffer containing frame data
     * @param width Frame width
     * @param height Frame height
     * @param stride Frame stride
     * @param fps Frame rate (30 or 60)
     * @return true if successful, false otherwise
      */
    fun sendFrameDirect(buffer: java.nio.ByteBuffer, width: Int, height: Int, stride: Int, fps: Int): Boolean {
        if (nativeHandle == 0L) {
            Log.w(TAG, "No valid sender handle")
            return false
        }
        return nativeSendFrameDirect(nativeHandle, buffer, width, height, stride, fps)
    }

    fun destroySender() {
        if (nativeHandle != 0L) {
            nativeDestroySender(nativeHandle)
            nativeHandle = 0
            Log.d(TAG, "NDI sender destroyed")
        }
    }

    fun cleanup() {
        destroySender()
        nativeCleanup()
        isInitialized = false
        Log.d(TAG, "NDI cleanup complete")
    }

    /**
     * Get the current native handle
     */
    fun getNativeHandle(): Long = nativeHandle
}
