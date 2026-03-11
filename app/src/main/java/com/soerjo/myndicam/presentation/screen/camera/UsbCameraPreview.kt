package com.soerjo.myndicam.presentation.screen.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentManager
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.soerjo.myndicam.presentation.fragment.UsbCameraFragment

// FPS and resolution tracking for USB camera preview
private var usbFrameCount = 0
private var usbLastFpsTime = System.currentTimeMillis()
private var usbCurrentFps = 0
private var usbCurrentWidth = 0
private var usbCurrentHeight = 0

@Composable
fun UsbCameraPreview(
    fragmentManager: FragmentManager,
    onFrameData: (FrameInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            androidx.fragment.app.FragmentContainerView(ctx).apply {
                id = android.R.id.custom
            }
        },
        modifier = modifier,
        update = { fragmentContainer ->
            val existingFragment = fragmentManager.findFragmentById(fragmentContainer.id)
            if (existingFragment == null) {
                fragmentManager.beginTransaction()
                    .replace(fragmentContainer.id, UsbCameraFragment())
                    .commit()
            } else {
                val fragment = existingFragment as? UsbCameraFragment
                if (fragment != null && fragment != usbFragment) {
                    usbFragment = fragment
                    fragment.setFrameCallback(object : IPreviewDataCallBack {
                        override fun onPreviewData(
                            data: ByteArray?,
                            width: Int,
                            height: Int,
                            format: IPreviewDataCallBack.DataFormat
                        ) {
                            // Update resolution tracking
                            if (width != usbCurrentWidth || height != usbCurrentHeight) {
                                usbCurrentWidth = width
                                usbCurrentHeight = height
                            }

                            // Update FPS tracking
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
                    })
                }
            }
        }
    )
}

private var usbFragment: UsbCameraFragment? = null
