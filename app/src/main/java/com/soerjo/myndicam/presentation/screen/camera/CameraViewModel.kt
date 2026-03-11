package com.soerjo.myndicam.presentation.screen.camera

import android.hardware.usb.UsbDevice
import android.util.Log
import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soerjo.myndicam.core.common.Constants
import com.soerjo.myndicam.core.util.formatAspectRatio
import com.soerjo.myndicam.data.camera.UsbCameraController
import com.soerjo.myndicam.data.datasource.CameraDataSource
import com.soerjo.myndicam.data.datasource.UsbCameraDataSource
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.model.CameraType
import com.soerjo.myndicam.domain.model.FrameRate
import com.soerjo.myndicam.domain.usecase.ObserveSettingsUseCase
import com.soerjo.myndicam.domain.usecase.SaveSettingsUseCase
import com.soerjo.ndi.NDISender
import com.soerjo.ndi.NDIManager
import com.soerjo.ndi.model.TallyState
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.usb.USBMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Camera Screen
 */
data class CameraUiState(
    val isStreaming: Boolean = false,
    val availableCameras: List<CameraInfo> = emptyList(),
    val selectedCamera: CameraInfo? = null,
    val selectedFrameRate: FrameRate = FrameRate.FPS_30,
    val actualResolution: Size = Size(Constants.TARGET_WIDTH, Constants.TARGET_HEIGHT),
    val tallyState: TallyState = TallyState(),
    val sourceName: String = Constants.DEFAULT_SOURCE_NAME,
    val isLoading: Boolean = true,
    val usbConnectionState: UsbConnectionState = UsbConnectionState.Idle,
    val errorMessage: String? = null
)

/**
 * ViewModel for Camera Screen
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val cameraDataSource: CameraDataSource,
    private val usbCameraDataSource: UsbCameraDataSource
) : ViewModel() {

    private val TAG = "CameraViewModel"

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // Cached streaming state for NDI
    @Volatile
    private var isStreaming = false

    private var ndiSender: NDISender? = null

    // USB camera management
    private val usbCameraControllers = mutableMapOf<Int, UsbCameraController>()
    private var activeUsbCameraController: UsbCameraController? = null

    // Internal cameras (detected once)
    private var internalCameras: List<CameraInfo.CameraX> = emptyList()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                // NDI initialization
                if (!NDIManager.isInitialized()) {
                    val initialized = NDIManager.initialize()
                    if (!initialized) {
                        Log.e(TAG, "Failed to initialize NDI")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to initialize NDI engine"
                        )
                    }
                }

                // Detect internal cameras first
                internalCameras = cameraDataSource.detectAvailableCameras()
                Log.d(TAG, "Internal cameras detected: ${internalCameras.size}")

                // Initialize USB camera monitoring
                usbCameraDataSource.initialize()

                // Detect all cameras and auto-select
                detectCameras()

                // Observe settings
                combine(
                    observeSettingsUseCase.getSourceName(),
                    observeSettingsUseCase.getFrameRate()
                ) { sourceName, frameRate ->
                    Pair(sourceName, frameRate)
                }
                .catch { e ->
                    Log.e(TAG, "Error observing settings: ${e.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { (sourceName, frameRate) ->
                    _uiState.value = _uiState.value.copy(
                        sourceName = sourceName,
                        selectedFrameRate = frameRate,
                        isLoading = false
                    )

                    // Recreate NDI sender when source name changes
                    createNDISender(sourceName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during initialization: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }

        // Listen for detected USB cameras changes
        viewModelScope.launch {
            usbCameraDataSource.detectedUsbCameras.collect {
                updateCameraListWithUsbChange()
            }
        }
    }

    private fun createNDISender(sourceName: String) {
        try {
            // Release old sender if exists
            ndiSender?.release()

            // Create new sender
            ndiSender = NDIManager.createSender(sourceName)
            Log.d(TAG, "NDI sender created: $sourceName")

            // Observe tally state
            viewModelScope.launch {
                ndiSender?.tallyState?.collect { tallyState ->
                    _uiState.value = _uiState.value.copy(tallyState = tallyState)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create NDI sender: ${e.message}", e)
        }
    }

    /**
     * Detect available cameras - combines internal and USB cameras
     * Auto-selects: USB if available, else back camera
     */
    fun detectCameras() {
        viewModelScope.launch {
            try {
                // Get USB cameras
                val usbCameras = usbCameraDataSource.getDetectedCameras()
                
                // Combine internal + USB cameras
                val allCameras = internalCameras + usbCameras

                // Auto-select logic: USB if available, else back camera
                if (_uiState.value.selectedCamera == null) {
                    val selectedCamera = when {
                        usbCameras.isNotEmpty() -> usbCameras.first()
                        else -> internalCameras.find { it.type == CameraType.BACK } 
                            ?: internalCameras.firstOrNull()
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        availableCameras = allCameras,
                        selectedCamera = selectedCamera
                    )
                    Log.d(TAG, "Initial camera selected: ${selectedCamera?.name}")
                } else {
                    // Update list but keep current selection if still valid
                    _uiState.value = _uiState.value.copy(availableCameras = allCameras)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error detecting cameras: ${e.message}")
            }
        }
    }

    /**
     * Update camera list when USB cameras change (connect/disconnect)
     */
    private fun updateCameraListWithUsbChange() {
        val usbCameras = usbCameraDataSource.getDetectedCameras()
        val allCameras = internalCameras + usbCameras

        val currentSelected = _uiState.value.selectedCamera
        
        // Auto-switch logic
        val newSelectedCamera = when {
            // USB connected - switch to USB if current is not USB
            usbCameras.isNotEmpty() && currentSelected !is CameraInfo.Usb -> {
                Log.d(TAG, "USB camera connected, switching to USB")
                usbCameras.first()
            }
            // USB disconnected - switch to back camera if current was USB
            usbCameras.isEmpty() && currentSelected is CameraInfo.Usb -> {
                Log.d(TAG, "USB camera disconnected, switching to back camera")
                internalCameras.find { it.type == CameraType.BACK } ?: internalCameras.firstOrNull()
            }
            // Keep current selection
            else -> currentSelected
        }

        _uiState.value = _uiState.value.copy(
            availableCameras = allCameras,
            selectedCamera = newSelectedCamera
        )
        Log.d(TAG, "Cameras updated: ${allCameras.size} (${internalCameras.size} internal, ${usbCameras.size} USB)")
    }

    /**
     * Update USB camera list (for backward compatibility)
     */
    fun updateUsbCameraList(autoSelect: Boolean = false) {
        val usbCameras = usbCameraDataSource.getDetectedCameras()
        val allCameras = internalCameras + usbCameras

        _uiState.value = _uiState.value.copy(availableCameras = allCameras)
        Log.d(TAG, "USB cameras updated: ${usbCameras.size} detected")
        
        // Auto-select the first USB camera if requested and one exists
        if (autoSelect && usbCameras.isNotEmpty()) {
            val firstUsb = usbCameras.first()
            if (_uiState.value.selectedCamera !is CameraInfo.Usb || (_uiState.value.selectedCamera as? CameraInfo.Usb)?.deviceId != firstUsb.deviceId) {
                selectCamera(firstUsb)
                Log.d(TAG, "Auto-selected USB camera: ${firstUsb.name}")
            }
        }
    }

    /**
     * Select a camera
     */
    fun selectCamera(camera: CameraInfo) {
        // Reset connection state when changing camera
        _uiState.value = _uiState.value.copy(usbConnectionState = UsbConnectionState.Idle)

        // Close current USB camera if it's different from the one being selected
        if (activeUsbCameraController != null) {
            if (camera !is CameraInfo.Usb || activeUsbCameraController?.getUsbDevice()?.deviceId != camera.deviceId) {
                activeUsbCameraController?.closeCamera()
                activeUsbCameraController = null
            }
        }

        _uiState.value = _uiState.value.copy(selectedCamera = camera)
        Log.d(TAG, "Camera selected: ${camera.name}")
    }

    /**
     * Set listener for USB camera connect events
     */
    fun setUsbDeviceConnectListener(callback: IDeviceConnectCallBack) {
        usbCameraDataSource.setDeviceConnectCallBack(callback)
    }

    /**
     * Set USB connection status
     */
    fun setUsbConnectionState(state: UsbConnectionState) {
        _uiState.value = _uiState.value.copy(usbConnectionState = state)
    }

    /**
     * Request permission for USB device
     * If permission already granted, triggers connection flow
     */
    fun requestUsbPermission(device: UsbDevice) {
        // Don't reset state if already connected
        if (_uiState.value.usbConnectionState == UsbConnectionState.Connected) {
            Log.d(TAG, "Already connected, skipping permission request")
            return
        }
        
        _uiState.value = _uiState.value.copy(usbConnectionState = UsbConnectionState.Connecting)
        Log.d(TAG, "Requesting USB permission for ${device.deviceName}")
        
        // Check if already has permission - if so, still need to trigger connection
        if (usbCameraDataSource.hasPermission(device)) {
            Log.d(TAG, "Permission already granted")
            // Permission is already granted, but we need to request again to trigger onConnectDev
            // The USB monitor will call onConnectDev when permission is granted
            usbCameraDataSource.requestPermission(device)
        } else {
            usbCameraDataSource.requestPermission(device)
        }
    }

    /**
     * Create USB camera controller for a device
     */
    fun createUsbCameraController(
        device: UsbDevice,
        usbControlBlock: USBMonitor.UsbControlBlock
    ): UsbCameraController {
        val controller = UsbCameraController(
            context = usbCameraDataSource.getContext(),
            usbDevice = device,
            usbControlBlock = usbControlBlock
        )

        // Set frame callback for NDI streaming
        controller.onFrameCallback = { data, width, height, stride ->
            sendFrame(data, width, height, stride)
        }

        usbCameraControllers[device.deviceId] = controller
        activeUsbCameraController = controller
        
        _uiState.value = _uiState.value.copy(usbConnectionState = UsbConnectionState.Connected)
        Log.d(TAG, "USB camera controller created, state=Connected")

        return controller
    }

    /**
     * Get USB camera controller
     */
    fun getUsbCameraController(deviceId: Int): UsbCameraController? {
        return usbCameraControllers[deviceId]
    }

    /**
     * Select a frame rate
     */
    fun selectFrameRate(frameRate: FrameRate) {
        viewModelScope.launch {
            saveSettingsUseCase.saveFrameRate(frameRate)
            _uiState.value = _uiState.value.copy(selectedFrameRate = frameRate)
        }
    }

    fun toggleStreaming() {
        isStreaming = !isStreaming
        _uiState.value = _uiState.value.copy(isStreaming = isStreaming)
        Log.d(TAG, "NDI Streaming: ${if (isStreaming) "ON" else "OFF"}")
    }

    /**
     * Update actual resolution from camera
     */
    fun updateActualResolution(width: Int, height: Int) {
        if (_uiState.value.actualResolution.width != width ||
            _uiState.value.actualResolution.height != height) {
            _uiState.value = _uiState.value.copy(actualResolution = Size(width, height))
            Log.d(TAG, "Actual resolution: ${width}x${height}")
        }
    }

    fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int) {
        // Fast path: use cached volatile state to avoid StateFlow read barrier
        if (!isStreaming) return

        try {
            ndiSender?.sendFrame(data, width, height, stride)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send frame: ${e.message}", e)
        }
    }

    /**
     * Save source name
     */
    fun saveSourceName(name: String) {
        viewModelScope.launch {
            saveSettingsUseCase.saveSourceName(name)
        }
    }

    /**
     * Get internal cameras list
     */
    fun getInternalCameras(): List<CameraInfo.CameraX> = internalCameras

    /**
     * Get all available cameras
     */
    fun getAllCameras(): List<CameraInfo> {
        val usbCameras = usbCameraDataSource.getDetectedCameras()
        return internalCameras + usbCameras
    }

    /**
     * Refresh camera list - called when camera binding needs to be redone
     */
    fun refreshCameraList() {
        updateCameraListWithUsbChange()
    }

    /**
     * Get context for CameraX binding
     */
    fun getContext(): android.content.Context = cameraDataSource.getContext()

    /**
     * Get formatted aspect ratio
     */
    fun getFormattedAspectRatio(): String {
        val r = _uiState.value.actualResolution
        return formatAspectRatio(r.width, r.height)
    }

    /**
     * Get USB device list
     */
    fun getUsbDeviceList(): List<UsbDevice> {
        return usbCameraDataSource.getDeviceList()
    }

    override fun onCleared() {
        super.onCleared()
        usbCameraControllers.values.forEach { it.cleanup() }
        usbCameraControllers.clear()
        usbCameraDataSource.cleanup()
    }
}
