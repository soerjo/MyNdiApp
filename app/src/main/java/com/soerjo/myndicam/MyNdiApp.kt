package com.soerjo.myndicam

import android.app.Application
import android.util.Log
import com.soerjo.ndi.NDIManager
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for MyNdiCam
 * Enables Hilt dependency injection and initializes NDI
 */
@HiltAndroidApp
class MyNdiApp : Application() {

    private val TAG = "MyNdiApp"

    override fun onCreate() {
        super.onCreate()

        // Initialize NDI module
        try {
            val initialized = NDIManager.initialize()
            if (initialized) {
                Log.d(TAG, "NDI module initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize NDI module")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NDI module", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Cleanup NDI module
        try {
            NDIManager.cleanup()
            Log.d(TAG, "NDI module cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up NDI module", e)
        }
    }
}
