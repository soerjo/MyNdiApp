# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MyNdiCam (package: `com.soerjo.myndicam`) - Android NDI Camera Streamer that sends live camera video over NDI to receivers like OBS Studio, vMix, etc. Supports both CameraX (built-in cameras) and USB UVC cameras.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug

# Clean build outputs
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Build specific module (useful for iterative development)
./gradlew :app:assembleDebug
./gradlew :ndi:assembleDebug
./gradlew :libuvc:assembleDebug
```

## Architecture

### Module Structure

The project is organized into **five modules**:

1. **`app`** - Main application (camera + UI)
2. **`ndi`** - Standalone NDI library module (reusable across projects)
3. **`libuvc`** - Native JNI library for USB UVC camera communication (ndk-build)
4. **`libnative`** - Native library with YUV utilities (CMake)
5. **`libausbc`** - Android USB Camera library (Kotlin wrapper around libuvc)

### Technology Stack
- **UI**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture (presentation/domain/data layers)
- **DI**: Hilt with KSP annotation processing
- **Camera**: CameraX (built-in cameras) + libausbc (USB UVC cameras)
- **Native**: C++ JNI with NDI SDK (Processing.NDI v6.3 via dynamic loading)
- **Build**: Gradle with Kotlin DSL, CMake for native code
- **Java**: 11

### App Module Structure (Clean Architecture)

```
app/src/main/java/com/soerjo/myndicam/
├── presentation/                    # UI layer
│   ├── MainActivity.kt             # Activity entry point (permissions, fullscreen)
│   ├── fragment/
│   │   └── UsbCameraFragment.kt   # Camera preview (libausbc base class)
│   └── screen/camera/
│       ├── CameraScreen.kt        # Main camera screen composable
│       └── CameraViewModel.kt     # ViewModel with StateFlow state management
├── domain/                         # Business logic layer
│   ├── model/                     # Domain models (pure data classes)
│   │   ├── CameraInfo.kt          # Sealed class for CameraX and USB cameras
│   │   ├── CameraType.kt
│   │   └── FrameRate.kt
│   ├── repository/                # Repository interfaces
│   │   ├── CameraRepository.kt
│   │   └── SettingsRepository.kt
│   └── usecase/                   # Use cases (business logic)
│       ├── DetectCamerasUseCase.kt
│       ├── ObserveSettingsUseCase.kt
│       └── SaveSettingsUseCase.kt
├── data/                           # Data layer
│   ├── repository/                 # Repository implementations
│   │   ├── CameraRepositoryImpl.kt
│   │   └── SettingsRepositoryImpl.kt
│   ├── datasource/
│   │   ├── CameraDataSource.kt    # CameraX operations
│   │   └── UsbCameraDataSource.kt # USB camera detection via USBMonitor
│   └── camera/
│       └── UsbCameraController.kt # USB camera lifecycle & frame capture
├── core/                           # Core utilities
│   ├── di/
│   │   └── AppModule.kt          # Hilt DI module
│   ├── util/                      # Extension functions
│   │   ├── ImageFormatExtensions.kt
│   │   └── MathExtensions.kt
│   └── common/
│       └── Constants.kt           # App constants
└── MyNdiApp.kt                    # Application class with @HiltAndroidApp
```

### NDI Module Structure

```
ndi/src/main/
├── java/com/soerjo/ndi/
│   ├── model/
│   │   └── TallyState.kt          # NDI domain model
│   ├── internal/
│   │   └── NDIWrapper.kt          # JNI bindings (private API)
│   ├── NDIManager.kt              # Public API - lifecycle singleton
│   └── NDISender.kt               # Public API - sender instances
├── cpp/
│   ├── ndi_wrapper.cpp            # JNI implementation
│   ├── Include/                   # NDI SDK headers
│   └── CMakeLists.txt             # Native build config
└── jniLibs/
    ├── arm64-v8a/libndi.so       # NDI native library (NOT in repo - manual setup)
    └── armeabi-v7a/libndi.so     # NDI native library (NOT in repo - manual setup)
```

### USB Camera Modules Structure

```
libuvc/
├── src/main/jni/                  # Native JNI implementation (ndk-build)
│   ├── UVCCamera.cpp/h           # UVC camera implementation
│   ├── UVCPreview.cpp/h           # Preview handling
│   └── libjpeg-turbo-1.5.0/      # JPEG codec
└── jniLibs/                      # Pre-built native libraries
    ├── arm64-v8a/libUVCCamera.so
    ├── armeabi-v7a/libUVCCamera.so
    └── ...

libausbc/
└── src/main/java/com/jiangdg/ausbc/
    ├── MultiCameraClient.kt       # Multi-camera management
    ├── camera/CameraUVC.kt        # UVC camera implementation
    ├── base/CameraFragment.kt     # Base fragment for camera operations
    └── utils/                     # Utilities
```

## Key Architecture Patterns

### MVVM with Hilt DI (Standard Pattern)
- **ViewModel**: `CameraViewModel` (`@HiltViewModel`) - Injects use cases, emits `StateFlow<CameraUiState>`
- **Composable**: `CameraScreen` - Uses `hiltViewModel()` to get ViewModel, collects state via `collectAsStateWithLifecycle()`
- **Repository pattern**: Interfaces in `domain/repository/`, implementations in `data/repository/`

**Correct way to use ViewModel in Composable:**
```kotlin
@Composable
fun UsbCameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // UI reads from uiState
    // UI calls viewModel methods for actions
}
```

**Wrong (anti-pattern) - Don't do this:**
- Don't inject dependencies directly in Composable using EntryPointAccessors
- Don't put business logic in Composable (NDI init, settings save, etc.)
- Composable should only handle UI rendering and user interactions

### Dependency Injection (Hilt)
- `@HiltAndroidApp` on `MyNdiApp.kt` enables Hilt
- `AppModule` binds repository interfaces to implementations
- `@HiltViewModel` injects use cases into ViewModels
- Uses **KSP** (not kapt) for annotation processing

### CameraInfo Sealed Class
```kotlin
sealed class CameraInfo {
    data class CameraX(
        override val name: String,
        override val type: CameraType,
        val cameraSelector: CameraSelector
    ) : CameraInfo()

    data class Usb(
        val deviceId: Int,
        override val name: String,
        val vendorId: Int,
        val productId: Int
    ) : CameraInfo() { override val type: CameraType = CameraType.EXTERNAL }
}
```

### NDI Module API
```kotlin
// Global lifecycle (call once at app startup)
NDIManager.initialize()
val sender = NDIManager.createSender("My Camera")

// Use sender
sender.sendFrame(uyvyData, width, height, stride)
sender.tallyState.collect { state -> /* handle tally */ }

// Cleanup
sender.release()
NDIManager.cleanup()
```

### Data Flow

**Current USB Camera Pipeline (CameraScreen → ViewModel):**
```
UsbCameraFragment (CameraFragment base class)
    ↓ frames via IPreviewDataCallBack
CameraScreen (Composable)
    ↓ frame callback
    ↓ convert format (NV21/RGBA → UYVY)
    ↓ viewModel.sendFrame()
CameraViewModel
    ↓ ndiSender.sendFrame()
NDISender → NDI async send
```

**Settings Flow:**
```
CameraScreen (UI)
    ↓ viewModel.saveSourceName()
CameraViewModel
    ↓ saveSettingsUseCase.saveSourceName()
SettingsRepository (SettingsRepositoryImpl)
    ↓ SharedPreferences
```

### Settings Repository
- Uses SharedPreferences via `SettingsRepositoryImpl`
- `SettingsRepository` interface in domain layer
- Exposes Flow for reactive updates
- Persists: source name, frame rate

### Tally System
```
Native C++ thread (10Hz polling) → NDISender.onTallyStateChange() →
StateFlow<TallyState> → UI observes via uiState.tallyState → tally indicators update
```

## Important Constants

- **Min SDK**: 29, **Target SDK**: 36
- **Resolution**: 1920x1080 (1080p)
- **Frame rates**: 30 FPS (default), 60 FPS
- **NDI format**: UYVY (2 bytes per pixel, 4:2:2 subsampling)
- **ABI filters**: arm64-v8a, armeabi-v7a
- **Java**: 11
- **NDK**: r21 or later (for NDI module), 27.0.12077973 (for libnative)
- **CMake**: 3.22.1
- **Screen Mode**: `Constants.SCREEN_MODE` controls which screen to show (2 = UsbCameraScreen)

## NDI SDK Setup (Required)

The NDI SDK is NOT included in the repository. You must manually add it:

1. Download from https://ndi.video/download/
2. Copy `libndi.so` to:
   - `ndi/src/main/jniLibs/arm64-v8a/libndi.so`
   - `ndi/src/main/jniLibs/armeabi-v7a/libndi.so`

The native build dynamically loads the NDI library via `Processing.NDI.DynamicLoad.h`.

## USB Camera Support

The app supports USB UVC cameras via the libausbc library:

### USB Camera Detection
- `UsbCameraDataSource` detects USB cameras via `USBMonitor`
- USB cameras appear in the camera list with `CameraInfo.Usb` type
- USB device filter is defined in `app/src/main/res_filter.xml`

### USB Camera Preview
/xml/usb_device- `UsbCameraFragment` extends `CameraFragment` from libausbc
- Fragment handles camera preview automatically via base class
- Frames exposed via `setFrameCallback(IPreviewDataCallBack)`

### USB Camera Permissions
- USB host mode permission is required (`android.hardware.usb.host`)
- USB device attachment intent filter for automatic app launch
- Permission is requested via `USBMonitor.requestPermission()`

### Frame Format Conversion
- USB camera provides NV21 or RGBA format
- Must convert to UYVY for NDI streaming
- Conversion functions in `CameraScreen.kt`:
  - `convertToUyvy()` - dispatches to appropriate converter
  - `convertNv21ToUyvy()` - NV21 → UYVY
  - `convertRgbaToUyvy()` - RGBA → UYVY

## Code Patterns

### Composable State Management
- Use `hiltViewModel()` to get ViewModel
- Use `collectAsStateWithLifecycle()` to collect StateFlow
- Use `remember { mutableStateOf() }` for local UI state only
- Use `derivedStateOf { }` for computed values
- Use `LaunchedEffect` for side effects on state changes

### CameraViewModel State (CameraUiState)
```kotlin
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
```

### ViewModel Methods for Composable
- `toggleStreaming()` - Toggle NDI streaming on/off
- `saveSourceName(name: String)` - Save NDI source name
- `updateActualResolution(width, height)` - Update resolution from camera
- `sendFrame(data, width, height, stride)` - Send frame to NDI

### Performance Patterns
1. **Volatile cache**: `CameraViewModel` uses `@Volatile` for cached streaming state to avoid StateFlow read barrier in hot path (sendFrame called every frame)
2. **Buffer pooling**: `UyvyBufferPool` in `core/util/ImageFormatExtensions.kt` provides buffer pooling to reduce GC pressure from per-frame allocations
3. **Direct ByteBuffer access**: Reads directly from Image.Plane ByteBuffers to avoid intermediate array copies

### Native Lifecycle
Always call `NDIManager.cleanup()` in `Application.onTerminate()` or `Activity.onDestroy()` to properly release native resources and stop the tally polling thread. NDISender instances should be released via `release()` when no longer needed.

### Module Dependencies
- **app** depends on **ndi**, **libausbc** (via `implementation(project(":..."))`)
- **libausbc** depends on **libuvc**, **libnative**
- **ndi** has NO dependencies on **app** (can be used standalone)
