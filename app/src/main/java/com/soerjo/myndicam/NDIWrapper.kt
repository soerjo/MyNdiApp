package com.soerjo.myndicam

import android.util.Log

/**
 * NDI Native Wrapper
 *
 * This class provides JNI bindings to the native NDI library (libndi.so).
 * The native library must be present in app/src/main/jniLibs/<abi>/libndi.so
 *
 * CURRENT STATUS: Stub implementation
 * The native libndi.so is present but JNI bindings need to be implemented.
 * This wrapper runs in "stub mode" and will not crash the app.
 *
 * To enable actual NDI streaming, you need to:
 * OPTION 1: Implement the native JNI methods (see ndi_wrapper.cpp template)
 * OPTION 2: Use the official NDI SDK for Android which includes pre-built Java bindings
 * Download from: https://ndi.video/download/
 */
object NDIWrapper {
    private const val TAG = "NDIWrapper"
    private var isInitialized = false
    private var nativeHandle: Long = 0
    private var libraryLoaded = false

    init {
        try {
            // Load our JNI wrapper library
            System.loadLibrary("ndi_wrapper")
            libraryLoaded = true
            Log.d(TAG, "Successfully loaded libndi_wrapper.so")
        } catch (e: UnsatisfiedLinkError) {
            libraryLoaded = false
            Log.w(TAG, "libndi_wrapper.so not found or JNI methods not implemented - running in stub mode", e)
        }
    }

    /**
     * Initialize the NDI library
     * @return true if successful, false otherwise
     */
    private fun nativeInitialize(): Boolean {
        return if (libraryLoaded) {
            try {
                // This will call the native JNI method if implemented
                _nativeInitialize()
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "nativeInitialize() not implemented - stub mode")
                false
            }
        } else {
            false
        }
    }

    /**
     * Create an NDI sender
     * @param sourceName The name of the NDI source
     * @return Native handle to the sender (0 if failed)
     */
    private fun nativeCreateSender(sourceName: String): Long {
        return if (libraryLoaded) {
            try {
                _nativeCreateSender(sourceName)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "nativeCreateSender() not implemented - stub mode")
                0L
            }
        } else {
            0L
        }
    }

    /**
     * Send a video frame via NDI
     * @param handle Native handle to the sender
     * @param data Frame data (YUV format)
     * @param width Frame width
     * @param height Frame height
     * @param stride Frame stride
     * @return true if successful, false otherwise
     */
    private fun nativeSendFrame(handle: Long, data: ByteArray, width: Int, height: Int, stride: Int): Boolean {
        return if (libraryLoaded) {
            try {
                _nativeSendFrame(handle, data, width, height, stride)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "nativeSendFrame() not implemented - stub mode")
                false
            }
        } else {
            false
        }
    }

    /**
     * Destroy an NDI sender
     * @param handle Native handle to the sender
     */
    private fun nativeDestroySender(handle: Long) {
        if (libraryLoaded) {
            try {
                _nativeDestroySender(handle)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "nativeDestroySender() not implemented - stub mode")
            }
        }
    }

    /**
     * Cleanup NDI resources
     */
    private fun nativeCleanup() {
        if (libraryLoaded) {
            try {
                _nativeCleanup()
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "nativeCleanup() not implemented - stub mode")
            }
        }
    }

    // External native method declarations (implemented in C/C++ via JNI)
    private external fun _nativeInitialize(): Boolean
    private external fun _nativeCreateSender(sourceName: String): Long
    private external fun _nativeSendFrame(handle: Long, data: ByteArray, width: Int, height: Int, stride: Int): Boolean
    private external fun _nativeDestroySender(handle: Long)
    private external fun _nativeCleanup()

    // Kotlin wrapper methods
    fun initialize(): Boolean {
        if (!isInitialized) {
            isInitialized = nativeInitialize()
            if (isInitialized) {
                Log.d(TAG, "NDI initialized successfully")
            } else {
                Log.w(TAG, "NDI running in stub mode - JNI methods not implemented")
            }
        }
        return isInitialized
    }

    fun createSender(sourceName: String): Long {
        if (!isInitialized) {
            Log.w(TAG, "NDI not initialized - running in stub mode")
        }
        val handle = nativeCreateSender(sourceName)
        if (handle != 0L) {
            nativeHandle = handle
            Log.d(TAG, "NDI sender created: $sourceName (handle: $handle)")
        } else {
            Log.w(TAG, "NDI sender running in stub mode")
        }
        return handle
    }

    fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int): Boolean {
        if (nativeHandle == 0L) {
            // Stub mode - silently skip
            return false
        }
        return nativeSendFrame(nativeHandle, data, width, height, stride)
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
}
