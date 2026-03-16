package com.soerjo.myndicam.presentation.screen.camera.components.preview

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentManager
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.soerjo.myndicam.domain.model.Resolution
import com.soerjo.myndicam.presentation.fragment.UsbCameraFragment
import com.soerjo.myndicam.presentation.screen.camera.model.FrameFormat
import com.soerjo.myndicam.presentation.screen.camera.model.FrameInfo
import java.util.UUID

private const val TAG = "UsbCameraPreview"

private var usbFrameCount = 0
private var usbLastFpsTime = System.currentTimeMillis()
private var usbCurrentFps = 0
private var usbCurrentWidth = 0
private var usbCurrentHeight = 0

@Composable
fun UsbCameraPreview(
    fragmentManager: FragmentManager,
    onFrameData: (FrameInfo) -> Unit,
    resolution: Resolution = Resolution.HD,
    modifier: Modifier = Modifier,
    fragmentId: Int = android.R.id.custom
) {
    Log.d(TAG, "[PREVIEW_INIT] Starting preview with resolution: ${resolution.displayName}")

    var isFragmentInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(resolution) {
        Log.d(TAG, "[PREVIEW_CLEANUP] Starting cleanup for resolution change")

        try {
            val existingFragment = fragmentManager.findFragmentById(fragmentId)
            if (existingFragment != null) {
                fragmentManager.beginTransaction()
                    .remove(existingFragment)
                    .commitAllowingStateLoss()
                fragmentManager.executePendingTransactions()
                Log.d(TAG, "[PREVIEW_CLEANUP] Removed old fragment")
            } else {
                Log.d(TAG, "[PREVIEW_CLEANUP] No existing fragment found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[PREVIEW_CLEANUP] Error removing fragment: ${e.message}")
        }

        isFragmentInitialized = false
    }

    AndroidView(
        factory = { ctx ->
            Log.d(TAG, "[FACTORY] Creating FragmentContainerView with id=$fragmentId")

            androidx.fragment.app.FragmentContainerView(ctx).apply {
                id = fragmentId
            }
        },
        modifier = modifier,
        update = { fragmentContainer ->
            if (!isFragmentInitialized) {
                Log.d(TAG, "[UPDATE] Creating new fragment, containerId=${fragmentContainer.id}")

                val newFragment = UsbCameraFragment()
                newFragment.setResolution(resolution)
                fragmentManager.beginTransaction()
                    .replace(fragmentContainer.id, newFragment)
                    .commitAllowingStateLoss()
                fragmentManager.executePendingTransactions()

                usbFragment = newFragment
                newFragment.setFrameCallback(createFrameCallback(onFrameData))
                isFragmentInitialized = true
                Log.d(TAG, "[UPDATE] Fragment created with ${resolution.displayName}, callback set")
            }
        }
    )
}

private fun createFrameCallback(onFrameData: (FrameInfo) -> Unit): IPreviewDataCallBack {
    return object : IPreviewDataCallBack {
        override fun onPreviewData(
            data: ByteArray?,
            width: Int,
            height: Int,
            format: IPreviewDataCallBack.DataFormat
        ) {
            if (width != usbCurrentWidth || height != usbCurrentHeight) {
                usbCurrentWidth = width
                usbCurrentHeight = height
            }

            usbFrameCount++
            val now = System.currentTimeMillis()
            if (now - usbLastFpsTime >= 1000) {
                usbCurrentFps = usbFrameCount
                usbFrameCount = 0
                usbLastFpsTime = now
            }

            val frameFormat = when (format) {
                IPreviewDataCallBack.DataFormat.NV21 -> FrameFormat.NV21
                IPreviewDataCallBack.DataFormat.RGBA -> FrameFormat.RGBA
            }

            val frameInfo = FrameInfo(
                data = data,
                yuvPlanes = null,
                width = width,
                height = height,
                format = frameFormat,
                fps = usbCurrentFps
            )

            onFrameData(frameInfo)
        }
    }
}

private var usbFragment: UsbCameraFragment? = null
