package com.soerjo.myndicam.presentation.screen.camera

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soerjo.myndicam.core.common.Constants
import com.soerjo.myndicam.core.util.formatAspectRatio
import com.soerjo.myndicam.data.datasource.CameraDataSource
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.model.CameraType
import com.soerjo.myndicam.domain.model.FrameRate
import com.soerjo.myndicam.domain.usecase.DetectCamerasUseCase
import com.soerjo.myndicam.domain.usecase.ObserveSettingsUseCase
import com.soerjo.myndicam.domain.usecase.SaveSettingsUseCase
import com.soerjo.ndi.NDISender
import com.soerjo.ndi.NDIManager
import com.soerjo.ndi.model.TallyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val isLoading: Boolean = true
)

/**
 * ViewModel for Camera Screen
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val detectCamerasUseCase: DetectCamerasUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val cameraDataSource: CameraDataSource
) : ViewModel() {

    private val TAG = "CameraViewModel"

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var ndiSender: NDISender? = null

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // Initialize NDI module
            if (!NDIManager.isInitialized()) {
                val initialized = NDIManager.initialize()
                if (!initialized) {
                    Log.e(TAG, "Failed to initialize NDI")
                    return@launch
                }
            }

            // Observe settings
            combine(
                observeSettingsUseCase.getSourceName(),
                observeSettingsUseCase.getFrameRate()
            ) { sourceName, frameRate ->
                Pair(sourceName, frameRate)
            }.collect { (sourceName, frameRate) ->
                _uiState.value = _uiState.value.copy(
                    sourceName = sourceName,
                    selectedFrameRate = frameRate,
                    isLoading = false
                )

                // Recreate NDI sender when source name changes
                createNDISender(sourceName)
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
     * Detect available cameras
     */
    fun detectCameras(cameraProvider: ProcessCameraProvider) {
        viewModelScope.launch {
            val cameras = cameraDataSource.detectCameras(cameraProvider)
            _uiState.value = _uiState.value.copy(
                availableCameras = cameras,
                selectedCamera = cameras.find { it.type == CameraType.BACK } ?: cameras.firstOrNull()
            )
        }
    }

    /**
     * Select a camera
     */
    fun selectCamera(camera: CameraInfo) {
        _uiState.value = _uiState.value.copy(selectedCamera = camera)
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

    /**
     * Toggle streaming state
     */
    fun toggleStreaming() {
        _uiState.value = _uiState.value.copy(isStreaming = !_uiState.value.isStreaming)
        Log.d(TAG, "NDI Streaming: ${if (_uiState.value.isStreaming) "ON" else "OFF"}")
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

    /**
     * Send a video frame via NDI
     */
    fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int) {
        if (!_uiState.value.isStreaming) return

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
     * Get formatted aspect ratio
     */
    fun getFormattedAspectRatio(): String {
        val r = _uiState.value.actualResolution
        return formatAspectRatio(r.width, r.height)
    }

    override fun onCleared() {
        super.onCleared()
        ndiSender?.release()
    }
}
