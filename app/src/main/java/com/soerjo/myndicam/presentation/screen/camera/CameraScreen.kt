package com.soerjo.myndicam.presentation.screen.camera

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soerjo.myndicam.presentation.screen.camera.model.FrameFormat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soerjo.myndicam.core.common.Constants
import com.soerjo.myndicam.core.util.conversion.convertToUyvy
import com.soerjo.myndicam.domain.model.CameraInfo
import com.soerjo.myndicam.domain.model.Resolution
import com.soerjo.myndicam.domain.model.ScreenMode
import com.soerjo.myndicam.presentation.screen.camera.components.controls.CircularIconButton
import com.soerjo.myndicam.presentation.screen.camera.components.controls.StreamToggleFAB
import com.soerjo.myndicam.presentation.screen.camera.components.dialogs.CameraMenuDialog
import com.soerjo.myndicam.presentation.screen.camera.components.dialogs.CameraSelectorDialog
import com.soerjo.myndicam.presentation.screen.camera.components.dialogs.FrameRateSelectorDialog
import com.soerjo.myndicam.presentation.screen.camera.components.dialogs.ResolutionSelectorDialog
import com.soerjo.myndicam.presentation.screen.camera.components.dialogs.SettingsDialog
import com.soerjo.myndicam.presentation.screen.camera.components.overlay.FpsResolutionInfoBox
import com.soerjo.myndicam.presentation.screen.camera.components.overlay.LiveBadge
import com.soerjo.myndicam.presentation.screen.camera.components.overlay.NoUsbCameraOverlay
import com.soerjo.myndicam.presentation.screen.camera.components.overlay.TallyBorder
import com.soerjo.myndicam.presentation.screen.camera.components.preview.InternalCameraPreview
import com.soerjo.myndicam.presentation.screen.camera.components.preview.UsbCameraPreview
import com.soerjo.myndicam.presentation.screen.camera.model.UsbConnectionState

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.screenMode) {
        ScreenMode.USB -> UsbCameraContent(viewModel, uiState)
        ScreenMode.INTERNAL -> InternalCameraContent(viewModel, uiState)
    }
}

@Composable
private fun UsbCameraContent(
    viewModel: CameraViewModel,
    uiState: CameraUiState
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val fragmentManager = activity?.supportFragmentManager

    var showMenu by remember { mutableStateOf(false) }
    var showCameraSelector by remember { mutableStateOf(false) }
    var showResolutionSelector by remember { mutableStateOf(false) }
    var showFrameRateSelector by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(uiState.sourceName) }

    var currentFps by remember { mutableIntStateOf(0) }
    var currentWidth by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.sourceName) {
        sourceNameInput = uiState.sourceName
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (fragmentManager != null) {
            UsbCameraPreview(
                fragmentManager = fragmentManager,
                onFrameData = { frameInfo ->
                    currentFps = frameInfo.fps
                    currentWidth = frameInfo.width
                    currentHeight = frameInfo.height

                    if (frameInfo.data != null && uiState.isStreaming) {
                        try {
                            when (frameInfo.format) {
                                FrameFormat.NV21 -> {
                                    val nv12Data = com.soerjo.ndi.NDISender.convertNv21ToNv12(
                                        frameInfo.data,
                                        frameInfo.width,
                                        frameInfo.height
                                    )
                                    viewModel.sendFrame(nv12Data, frameInfo.width, frameInfo.height, frameInfo.width, uiState.selectedFrameRate.fps)
                                }
                                FrameFormat.RGBA -> {
                                    val uyvyData = convertToUyvy(
                                        frameInfo.data,
                                        frameInfo.width,
                                        frameInfo.height,
                                        frameInfo.format
                                    )
                                    viewModel.sendFrame(uyvyData, frameInfo.width, frameInfo.height, frameInfo.width * 2, uiState.selectedFrameRate.fps)
                                }
                                FrameFormat.YUV_420_888 -> {
                                    viewModel.sendFrame(frameInfo.data, frameInfo.width, frameInfo.height, frameInfo.width, uiState.selectedFrameRate.fps)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("UsbCameraScreen", "Error sending frame: ${e.message}")
                        }
                    }
                },
                resolution = uiState.selectedResolution,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (uiState.availableCameras.none { it is CameraInfo.Usb }) {
            NoUsbCameraOverlay(modifier = Modifier.fillMaxSize())
        }

        if (uiState.usbConnectionState is UsbConnectionState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (uiState.usbConnectionState as UsbConnectionState.Error).message,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        TallyBorder(
            isOnProgram = uiState.isStreaming && uiState.tallyState.isOnProgram,
            modifier = Modifier.matchParentSize()
        )

        LiveBadge(
            isStreaming = uiState.isStreaming,
            modifier = Modifier.align(Alignment.TopStart)
        )

        FpsResolutionInfoBox(
            fps = currentFps,
            width = currentWidth,
            height = currentHeight,
            tallyState = uiState.tallyState,
            isStreaming = uiState.isStreaming,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        StreamToggleFAB(
            isStreaming = uiState.isStreaming,
            onToggle = { viewModel.toggleStreaming() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        CircularIconButton(
            icon = Icons.Filled.MoreVert,
            onClick = { showMenu = true },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        if (showMenu) {
            CameraMenuDialog(
                cameraName = uiState.selectedCamera?.name ?: "USB Camera",
                resolutionName = uiState.selectedResolution.displayName,
                frameRateName = uiState.selectedFrameRate.displayName,
                isUsbMode = true,
                onCameraClick = {
                    showMenu = false
                    showCameraSelector = true
                },
                onResolutionClick = {
                    showMenu = false
                    showResolutionSelector = true
                },
                onFrameRateClick = {
                    showMenu = false
                    showFrameRateSelector = true
                },
                onSwitchToUsbClick = {},
                onSwitchToInternalClick = {
                    viewModel.switchToScreenMode(ScreenMode.INTERNAL)
                    showMenu = false
                },
                onSettingsClick = {
                    showMenu = false
                    showSettings = true
                    sourceNameInput = uiState.sourceName
                },
                onDismiss = { showMenu = false }
            )
        }

        if (showCameraSelector) {
            CameraSelectorDialog(
                cameras = viewModel.getAllCameras(),
                selectedCamera = uiState.selectedCamera,
                onCameraSelected = { camera ->
                    viewModel.selectCamera(camera)
                    showCameraSelector = false
                },
                onDismiss = { showCameraSelector = false }
            )
        }

        if (showResolutionSelector) {
            ResolutionSelectorDialog(
                selectedResolution = uiState.selectedResolution,
                onResolutionSelected = { resolution ->
                    viewModel.selectResolution(resolution)
                    showResolutionSelector = false
                },
                onDismiss = { showResolutionSelector = false }
            )
        }

        if (showFrameRateSelector) {
            FrameRateSelectorDialog(
                selectedFrameRate = uiState.selectedFrameRate,
                onFrameRateSelected = { frameRate ->
                    viewModel.selectFrameRate(frameRate)
                    showFrameRateSelector = false
                },
                onDismiss = { showFrameRateSelector = false }
            )
        }

        if (showSettings) {
            SettingsDialog(
                sourceName = sourceNameInput,
                onSourceNameChange = { sourceNameInput = it },
                onSave = {
                    if (sourceNameInput.isNotBlank()) {
                        viewModel.saveSourceName(sourceNameInput)
                    }
                    showSettings = false
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun InternalCameraContent(
    viewModel: CameraViewModel,
    uiState: CameraUiState
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var showMenu by remember { mutableStateOf(false) }
    var showZoomSlider by remember { mutableStateOf(false) }
    var showCameraSelector by remember { mutableStateOf(false) }
    var showResolutionSelector by remember { mutableStateOf(false) }
    var showFrameRateSelector by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sourceNameInput by remember { mutableStateOf(uiState.sourceName) }

    var currentFps by remember { mutableIntStateOf(0) }
    var currentHeight by remember { mutableIntStateOf(0) }
    var currentWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.sourceName) {
        sourceNameInput = uiState.sourceName
    }

    LaunchedEffect(uiState.selectedResolution) {
        currentHeight = uiState.selectedResolution.height
        currentWidth = uiState.selectedResolution.width
    }

    LaunchedEffect(Unit) {
        if (uiState.selectedCamera !is CameraInfo.CameraX) {
            val internalCameras = viewModel.getInternalCameras()
            if (internalCameras.isNotEmpty()) {
                viewModel.selectCamera(internalCameras.first())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val cameraSelector = (uiState.selectedCamera as? CameraInfo.CameraX)?.cameraSelector
            ?: androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

        InternalCameraPreview(
            lifecycleOwner = lifecycleOwner,
            cameraSelector = cameraSelector,
            onFrameData = { frameInfo ->
                currentFps = frameInfo.fps
                currentWidth = frameInfo.width
                currentHeight = frameInfo.height

                if (uiState.isStreaming) {
                    if (frameInfo.directBuffers != null) {
                        val (yPlane, uPlane, vPlane) = frameInfo.directBuffers
                        val (yRowStride, uRowStride, vRowStride) = frameInfo.strides!!
                        val (yPixelStride, uPixelStride, vPixelStride) = frameInfo.pixelStrides!!

                        val nv12Data = com.soerjo.ndi.NDISender.convertYuv420ToNv12(
                            yPlane, yRowStride, yPixelStride,
                            uPlane, uRowStride, uPixelStride,
                            vPlane, vRowStride, vPixelStride,
                            frameInfo.width, frameInfo.height
                        )

                        val directBuffer = java.nio.ByteBuffer.allocateDirect(nv12Data.size)
                        directBuffer.put(nv12Data)
                        directBuffer.flip()

                        viewModel.sendFrameDirect(directBuffer, frameInfo.width, frameInfo.height, frameInfo.width, uiState.selectedFrameRate.fps)
                    } else if (frameInfo.data != null) {
                        viewModel.sendFrame(frameInfo.data, frameInfo.width, frameInfo.height, frameInfo.width, uiState.selectedFrameRate.fps)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            targetWidth = uiState.selectedResolution.width,
            targetHeight = uiState.selectedResolution.height,
            showZoomSlider = showZoomSlider,
            onZoomToggle = { showZoomSlider = !showZoomSlider }
        )

        TallyBorder(
            isOnProgram = uiState.isStreaming && uiState.tallyState.isOnProgram,
            modifier = Modifier.matchParentSize()
        )

        LiveBadge(
            isStreaming = uiState.isStreaming,
            modifier = Modifier.align(Alignment.TopStart)
        )

        FpsResolutionInfoBox(
            fps = currentFps,
            width = currentWidth,
            height = currentHeight,
            tallyState = uiState.tallyState,
            isStreaming = uiState.isStreaming,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        StreamToggleFAB(
            isStreaming = uiState.isStreaming,
            onToggle = { viewModel.toggleStreaming() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

//        CircularIconButton(
//            icon = androidx.compose.material.icons.Icons.Filled.Refresh,
//            onClick = {
//                val allCameras = viewModel.getAllCameras()
//                val currentIndex = allCameras.indexOf(uiState.selectedCamera)
//                if (allCameras.isNotEmpty()) {
//                    val nextIndex = (currentIndex + 1) % allCameras.size
//                    viewModel.selectCamera(allCameras[nextIndex])
//                }
//            },
//            modifier = Modifier.align(Alignment.BottomStart)
//        )

        CircularIconButton(
            icon = Icons.Filled.MoreVert,
            onClick = { showMenu = true },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        if (showMenu) {
            CameraMenuDialog(
                cameraName = uiState.selectedCamera?.name ?: "Internal Camera",
                resolutionName = uiState.selectedResolution.displayName,
                frameRateName = uiState.selectedFrameRate.displayName,
                isUsbMode = false,
                onCameraClick = {
                    showMenu = false
                    showCameraSelector = true
                },
                onResolutionClick = {
                    showMenu = false
                    showResolutionSelector = true
                },
                onFrameRateClick = {
                    showMenu = false
                    showFrameRateSelector = true
                },
                onSwitchToUsbClick = {
                    viewModel.switchToScreenMode(ScreenMode.USB)
                    showMenu = false
                },
                onSwitchToInternalClick = {},
                onSettingsClick = {
                    showMenu = false
                    showSettings = true
                    sourceNameInput = uiState.sourceName
                },
                onDismiss = { showMenu = false }
            )
        }

        if (showCameraSelector) {
            CameraSelectorDialog(
                cameras = viewModel.getAllCameras(),
                selectedCamera = uiState.selectedCamera,
                onCameraSelected = { camera ->
                    viewModel.selectCamera(camera)
                    showCameraSelector = false
                },
                onDismiss = { showCameraSelector = false }
            )
        }

        if (showResolutionSelector) {
            ResolutionSelectorDialog(
                selectedResolution = uiState.selectedResolution,
                onResolutionSelected = { resolution ->
                    viewModel.selectResolution(resolution)
                    showResolutionSelector = false
                },
                onDismiss = { showResolutionSelector = false }
            )
        }

        if (showFrameRateSelector) {
            FrameRateSelectorDialog(
                selectedFrameRate = uiState.selectedFrameRate,
                onFrameRateSelected = { frameRate ->
                    viewModel.selectFrameRate(frameRate)
                    showFrameRateSelector = false
                },
                onDismiss = { showFrameRateSelector = false }
            )
        }

        if (showSettings) {
            SettingsDialog(
                sourceName = sourceNameInput,
                onSourceNameChange = { sourceNameInput = it },
                onSave = {
                    if (sourceNameInput.isNotBlank()) {
                        viewModel.saveSourceName(sourceNameInput)
                    }
                    showSettings = false
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}
