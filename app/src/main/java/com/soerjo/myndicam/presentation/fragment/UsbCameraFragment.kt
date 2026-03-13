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
    private var fragmentId: Int = 0

    fun setFrameCallback(callback: IPreviewDataCallBack) {
        frameCallback = callback
        Log.d(TAG, "[FRAGMENT_CALLBACK_SET] Frame callback set - callback=${callback != null}")
    }

    fun clearFrameCallback() {
        frameCallback = null
        Log.d(TAG, "[FRAGMENT_CALLBACK_CLEAR] Frame callback cleared")
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        Log.d(TAG, "[FRAGMENT_VIEW_CREATE] Creating root view")
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
                Log.d(TAG, "[FRAGMENT_CAMERA_OPENED] USB Camera opened - Resolution: ${request?.previewWidth}x${request?.previewHeight}")

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
                Log.d(TAG, "[FRAGMENT_CAMERA_CLOSED] Camera closed")
            }
            ICameraStateCallBack.State.ERROR -> {
                Log.e(TAG, "[FRAGMENT_CAMERA_ERROR] Camera error: $msg")
            }
            else -> {
                Log.d(TAG, "[FRAGMENT_STATE] Camera state: $code")
            }
        }
    }

    override fun initView() {
        super.initView()
        Log.d(TAG, "[FRAGMENT_INIT] Fragment view initialized")
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        fragmentId = this.hashCode()
        Log.d(TAG, "[FRAGMENT_ATTACH] Fragment attached - id=$fragmentId")
    }

    override fun onDetach() {
        Log.d(TAG, "[FRAGMENT_DETACH] Fragment detached - id=$fragmentId")
        frameCallback = null
        super.onDetach()
    }

    override fun onDestroyView() {
        Log.d(TAG, "[FRAGMENT_VIEW_DESTROY] Fragment view destroyed - id=$fragmentId")
        frameCallback = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "UsbCameraFragment"
    }
}
