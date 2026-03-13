package com.soerjo.myndicam.data.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.usb.USBMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller for USB camera operations
 * Handles USB camera lifecycle, frame capture, and NDI streaming
 * refined for the "Linkage" pattern where SurfaceTexture is passed to native code
 */
class UsbCameraController(
    private val context: Context,
    private val usbDevice: UsbDevice,
    private val usbControlBlock: USBMonitor.UsbControlBlock
) {
    private val TAG = "UsbCameraController"

    private var cameraUVC: CameraUVC? = null
    private var isPreviewing = false

    // Camera state
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    // Preview size
    private val _previewSize = MutableStateFlow(Size(1920, 1080))
    val previewSize: StateFlow<Size> = _previewSize.asStateFlow()

    // Frame callback for NDI streaming
    var onFrameCallback: ((ByteArray, Int, Int, Int) -> Unit)? = null
    
    // Frame callback for preview rendering (NV21 format)
    var onPreviewFrame: ((ByteArray, Int, Int) -> Unit)? = null

    /**
     * Camera state
     */
    sealed class CameraState {
        object Idle : CameraState()
        object Opening : CameraState()
        object Previewing : CameraState()
        data class Error(val message: String) : CameraState()
    }

    /**
     * Linkage: Connects the Surface to the Native JNI layer
     * @param previewTarget SurfaceView, SurfaceTexture, or Surface for on-screen preview
     */
    open fun openCamera(previewTarget: Any? = null) {
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

                // Add preview data callback for NDI streaming
                addPreviewDataCallBack(object : IPreviewDataCallBack {
                    override fun onPreviewData(
                        data: ByteArray?,
                        width: Int,
                        height: Int,
                        format: IPreviewDataCallBack.DataFormat
                    ) {
                        if (data == null) return

                        Log.v(TAG, "Preview data: ${width}x${height}, format=$format")

                        // Process frame for NDI streaming
                        try {
                            when (format) {
                                IPreviewDataCallBack.DataFormat.NV21 -> {
                                    val uyvyData = convertNv21ToUyvy(data, width, height)
                                    val stride = width * 2
                                    onFrameCallback?.invoke(uyvyData, width, height, stride)
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

                // Link the SurfaceView/TextureView to the Native rendering engine
                // Using OPENGL mode to render automatically to the surface
                Log.d(TAG, "Calling openCamera with previewTarget: $previewTarget and request")
                openCamera(previewTarget, createDefaultRequest())
                Log.d(TAG, "openCamera returned")
            }
        } catch (e: Exception) {
            _cameraState.value = CameraState.Error("Failed to open camera: ${e.message}")
            Log.e(TAG, "Error opening USB camera", e)
        }
    }

    /**
     * Close the USB camera
     */
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

    /**
     * Update camera resolution
     */
    fun updateResolution(width: Int, height: Int) {
        try {
            cameraUVC?.updateResolution(width, height)
            Log.d(TAG, "Resolution updated: ${width}x${height}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating resolution", e)
        }
    }

    /**
     * Get all supported preview sizes
     */
    fun getSupportedPreviewSizes(): List<Size> {
        return cameraUVC?.getAllPreviewSizes()?.map { Size(it.width, it.height) } ?: emptyList()
    }

    /**
     * Create default camera request
     * Use OPENGL mode to get raw preview callbacks for NDI streaming
     */
    private fun createDefaultRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(1920)
            .setPreviewHeight(1080)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .setAudioSource(CameraRequest.AudioSource.NONE)
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_YUYV)
            .setAspectRatioShow(true)
            .setCaptureRawImage(false)
            .setRawPreviewData(true)  // Enable to get preview callbacks for NDI
            .create()
    }

    /**
     * Convert NV21 to UYVY
     */
    private fun convertNv21ToUyvy(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
        val uyvyData = ByteArray(width * height * 2)
        
        // NV21: Y plane followed by interleaved VU plane
        val ySize = width * height
        val vuOffset = ySize

        for (y in 0 until height step 2) {
            for (x in 0 until width step 2) {
                // Get Y values for 2x2 block
                val yIndex00 = y * width + x
                val yIndex01 = y * width + (x + 1)
                val yIndex10 = (y + 1) * width + x
                val yIndex11 = (y + 1) * width + (x + 1)

                val y00 = nv21Data[yIndex00].toInt() and 0xFF
                val y01 = nv21Data[yIndex01].toInt() and 0xFF
                val y10 = nv21Data[yIndex10].toInt() and 0xFF
                val y11 = nv21Data[yIndex11].toInt() and 0xFF

                // Get VU values (common for 2x2 block in 4:2:0)
                // In NV21, the UV plane is interleaved V U V U
                val vuIndex = vuOffset + (y / 2) * width + x
                val v = nv21Data[vuIndex].toInt() and 0xFF
                val u = nv21Data[vuIndex + 1].toInt() and 0xFF

                // Pack as UYVY: [U Y0 V Y1] for first row
                uyvyData[y * width * 2 + x * 2] = u.toByte()
                uyvyData[y * width * 2 + x * 2 + 1] = y00.toByte()
                uyvyData[y * width * 2 + (x + 1) * 2] = v.toByte()
                uyvyData[y * width * 2 + (x + 1) * 2 + 1] = y01.toByte()

                // Pack as UYVY for second row
                uyvyData[(y + 1) * width * 2 + x * 2] = u.toByte()
                uyvyData[(y + 1) * width * 2 + x * 2 + 1] = y10.toByte()
                uyvyData[(y + 1) * width * 2 + (x + 1) * 2] = v.toByte()
                uyvyData[(y + 1) * width * 2 + (x + 1) * 2 + 1] = y11.toByte()
            }
        }

        return uyvyData
    }

    /**
     * Get USB device
     */
    fun getUsbDevice(): UsbDevice {
        return usbDevice
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        closeCamera()
        cameraUVC = null
        onFrameCallback = null
    }
}
