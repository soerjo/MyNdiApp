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
                                    val nv12Data = convertNv21ToNv12(data, width, height)
                                    val stride = width  // NV12 stride = width (12bpp)
                                    onFrameCallback?.invoke(nv12Data, width, height, stride)
                                }
                                IPreviewDataCallBack.DataFormat.RGBA -> {
                                    // RGBA to NV12 conversion would be needed here
                                    // For now, skip this format or use as-is
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
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
            .setAspectRatioShow(true)
            .setCaptureRawImage(false)
            .setRawPreviewData(true)  // Enable to get preview callbacks for NDI
            .create()
    }

    /**
     * Convert NV21 to NV12
     * NV21: Y plane + VU interleaved plane
     * NV12: Y plane + UV interleaved plane (just swap VU to UV!)
     */
    private fun convertNv21ToNv12(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
        val nv12Data = ByteArray(width * height * 3 / 2)  // 12bpp

        // NV21: Y plane followed by interleaved VU plane
        // NV12: Y plane followed by interleaved UV plane

        val ySize = width * height
        val vuOffset = ySize
        val uvOffset = ySize

        // Copy Y plane (no conversion needed)
        System.arraycopy(nv21Data, 0, nv12Data, 0, ySize)

        // Convert VU (NV21) to UV (NV12) - just swap bytes!
        val uvSize = ySize / 4  // Quarter size for 4:2:0 subsampling
        for (i in 0 until uvSize step 2) {
            // NV21 has VU interleaved
            // NV12 needs UV interleaved
            nv12Data[uvOffset + i] = nv21Data[vuOffset + i + 1]  // U
            nv12Data[uvOffset + i + 1] = nv21Data[vuOffset + i]      // V
        }

        return nv12Data
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
