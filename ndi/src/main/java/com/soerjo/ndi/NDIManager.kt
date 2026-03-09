package com.soerjo.ndi

import android.util.Log
import com.soerjo.ndi.internal.NDIWrapper

/**
 * NDI Manager - Singleton for managing global NDI resources
 *
 * Provides a high-level API for managing NDI library lifecycle.
 * Use this to initialize and cleanup the NDI library.
 *
 * Usage:
 * ```kotlin
 * // Initialize once at app startup
 * NDIManager.initialize()
 *
 * // Create senders as needed
 * val sender = NDIManager.createSender("My Camera")
 *
 * // Cleanup when app is shutting down
 * NDIManager.cleanup()
 * ```
 */
object NDIManager {
    private const val TAG = "NDIManager"
    private var isInitialized = false

    /**
     * Initialize the NDI library
     *
     * This should be called once at application startup.
     * Multiple calls are safe - subsequent calls will be no-ops.
     *
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.d(TAG, "NDI already initialized")
            return true
        }

        return try {
            val success = NDIWrapper.initialize()
            if (success) {
                isInitialized = true
                Log.d(TAG, "NDI Manager initialized successfully")
            } else {
                Log.e(TAG, "NDI Manager initialization failed")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NDI Manager", e)
            false
        }
    }

    /**
     * Create a new NDI sender
     *
     * @param sourceName The name of the NDI source
     * @return A new NDISender instance
     * @throws IllegalStateException if NDI is not initialized or sender creation fails
     */
    fun createSender(sourceName: String): NDISender {
        if (!isInitialized) {
            throw IllegalStateException("NDI is not initialized. Call initialize() first.")
        }
        return NDISender(sourceName)
    }

    /**
     * Cleanup NDI resources
     *
     * This should be called once at application shutdown.
     * All senders should be released before calling this.
     */
    fun cleanup() {
        try {
            NDIWrapper.cleanup()
            isInitialized = false
            Log.d(TAG, "NDI Manager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup NDI Manager", e)
        }
    }

    /**
     * Check if NDI is initialized
     */
    fun isInitialized(): Boolean = isInitialized
}
