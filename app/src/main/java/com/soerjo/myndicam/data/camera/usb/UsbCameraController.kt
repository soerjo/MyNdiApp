package com.soerjo.myndicam.data.camera.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import android.util.Size
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.usb.USBMonitor
import com.soerjo.myndicam.data.camera.CameraState
import com.soerjo.myndicam.core.util.conversion.convertNv21ToNv12
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsbCameraController(
    private val context: Context,
    private val usbDevice: UsbDevice,
    private val usbControlBlock: USBMonitor.UsbControlBlock
) {
    private val TAG = "UsbCameraController"

    private var cameraUVC: CameraUVC? = null
    private var isPreviewing = false

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _previewSize = MutableStateFlow(Size(1920, 1080))
    val previewSize: StateFlow<Size> = _previewSize.asStateFlow()

    var onFrameCallback: ((ByteArray, Int, Int, Int) -> Unit)? = null

    var onPreviewFrame: ((ByteArray, Int, Int) -> Unit)? = null

    fun openCamera(previewTarget: Any? = null) {
        if (isPreviewing) {
            Log.w(TAG, "Camera already previewing")
            return
        }

        _cameraState.value = CameraState.Opening
        Log.d(TAG, "Opening USB camera: ${usbDevice.deviceName}, vendor=${usbDevice.vendorId}, product=${usbDevice.productId}, previewTarget=${previewTarget?.let { it::class.simpleName }}")

        try {
            cameraUVC = CameraUVC(context, usbDevice).apply {
                Log.d(TAG, "Created CameraUVC instance")
                setUsbControlBlock(usbControlBlock)
                Log.d(TAG, "Set USB control block")

                addPreviewDataCallBack(object : IPreviewDataCallBack {
                    override fun onPreviewData(
                        data: ByteArray?,
                        width: Int,
                        height: Int,
                        format: IPreviewDataCallBack.DataFormat
                    ) {
                        if (data == null) return

                        Log.v(TAG, "Preview data: ${width}x${height}, format=$format")

                        try {
                            when (format) {
                                IPreviewDataCallBack.DataFormat.NV21 -> {
                                    val nv12Data = convertNv21ToNv12(data, width, height)
                                    val stride = width
                                    onFrameCallback?.invoke(nv12Data, width, height, stride)
                                }
                                IPreviewDataCallBack.DataFormat.RGBA -> {
                                    onFrameCallback?.invoke(data, width, height, width * 4)
                                }
                                else -> {
                                    Log.w(TAG, "Unsupported preview format: $format")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing preview frame: ${e.message}")
                        }
                    }
                })

                setCameraStateCallBack(object : ICameraStateCallBack {
                    override fun onCameraState(
                        self: MultiCameraClient.ICamera,
                        code: ICameraStateCallBack.State,
                        msg: String?
                    ) {
                        Log.d(TAG, "Camera state changed: $code, msg: $msg")
                        when (code) {
                            ICameraStateCallBack.State.OPENED -> {
                                isPreviewing = true
                                _cameraState.value = CameraState.Previewing
                                Log.i(TAG, "Camera opened successfully: ${usbDevice.deviceName}")
                            }
                            ICameraStateCallBack.State.CLOSED -> {
                                isPreviewing = false
                                _cameraState.value = CameraState.Idle
                                Log.d(TAG, "Camera closed: ${usbDevice.deviceName}")
                            }
                            ICameraStateCallBack.State.ERROR -> {
                                isPreviewing = false
                                _cameraState.value = CameraState.Error(msg ?: "Unknown error")
                                Log.e(TAG, "Camera error: $msg")
                            }
                            else -> {
                                Log.d(TAG, "Camera state: $code")
                            }
                        }
                    }
                })

                Log.d(TAG, "Calling openCamera with previewTarget: $previewTarget and request")
                openCamera(previewTarget, createDefaultRequest())
                Log.d(TAG, "openCamera returned")
            }
        } catch (e: Exception) {
            _cameraState.value = CameraState.Error("Failed to open camera: ${e.message}")
            Log.e(TAG, "Error opening USB camera", e)
        }
    }

    fun closeCamera() {
        try {
            cameraUVC?.closeCamera()
            isPreviewing = false
            _cameraState.value = CameraState.Idle
            Log.d(TAG, "Camera closed: ${usbDevice.deviceName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing USB camera", e)
        }
    }

    fun updateResolution(width: Int, height: Int) {
        try {
            cameraUVC?.updateResolution(width, height)
            Log.d(TAG, "Resolution updated: ${width}x${height}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating resolution", e)
        }
    }

    fun getSupportedPreviewSizes(): List<Size> {
        return cameraUVC?.getAllPreviewSizes()?.map { Size(it.width, it.height) } ?: emptyList()
    }

    private fun createDefaultRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(1920)
            .setPreviewHeight(1080)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .setAudioSource(CameraRequest.AudioSource.NONE)
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
            .setAspectRatioShow(true)
            .setCaptureRawImage(false)
            .setRawPreviewData(true)
            .create()
    }

    fun getUsbDevice(): UsbDevice {
        return usbDevice
    }

    fun cleanup() {
        closeCamera()
        cameraUVC = null
        onFrameCallback = null
    }
}
