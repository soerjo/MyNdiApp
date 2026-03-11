package com.soerjo.myndicam.presentation.fragment

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

class UsbCameraFragment : CameraFragment() {

    private var frameCallback: IPreviewDataCallBack? = null

    fun setFrameCallback(callback: IPreviewDataCallBack) {
        frameCallback = callback
    }

    fun clearFrameCallback() {
        frameCallback = null
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(com.soerjo.myndicam.R.layout.fragment_camera, container, false)
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return view?.findViewById(com.soerjo.myndicam.R.id.camera_container)!!
    }

    override fun getGravity(): Int = Gravity.CENTER

    override fun getDefaultCamera(): android.hardware.usb.UsbDevice? {
        return null
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                val request = self.getCameraRequest()
                Log.d(TAG, "USB Camera opened: ${request?.previewWidth}x${request?.previewHeight}")

                self.addPreviewDataCallBack(object : IPreviewDataCallBack {
                    override fun onPreviewData(
                        data: ByteArray?,
                        width: Int,
                        height: Int,
                        format: IPreviewDataCallBack.DataFormat
                    ) {
                        frameCallback?.onPreviewData(data, width, height, format)
                    }
                })
            }
            ICameraStateCallBack.State.CLOSED -> {
                Log.d(TAG, "Camera closed")
            }
            ICameraStateCallBack.State.ERROR -> {
                Log.e(TAG, "Camera error: $msg")
            }
            else -> {
                Log.d(TAG, "Camera state: $code")
            }
        }
    }

    override fun initView() {
        super.initView()
    }

    companion object {
        private const val TAG = "UsbCameraFragment"
    }
}
