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

The project is organized into **four active modules**:

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
│   └── screen/camera/
│       ├── CameraScreen.kt         # Main camera screen composable
│       ├── CameraViewModel.kt      # ViewModel with StateFlow state management
│       └── components/
│           └── CameraDialogs.kt    # Reusable UI components
│   └── theme/                      # Compose theme files
├── domain/                          # Business logic layer
│   ├── model/                      # Domain models (pure data classes)
│   │   ├── CameraInfo.kt           # Sealed class for CameraX and USB cameras
│   │   ├── CameraType.kt
│   │   └── FrameRate.kt
│   ├── repository/                 # Repository interfaces
│   │   ├── CameraRepository.kt
│   │   └── SettingsRepository.kt
│   └── usecase/                    # Use cases (business logic)
│       ├── DetectCamerasUseCase.kt
│       ├── ObserveSettingsUseCase.kt
│       └── SaveSettingsUseCase.kt
├── data/                            # Data layer
│   ├── repository/                 # Repository implementations
│   │   ├── CameraRepositoryImpl.kt
│   │   └── SettingsRepositoryImpl.kt
│   ├── datasource/
│   │   ├── CameraDataSource.kt     # CameraX operations
│   │   └── UsbCameraDataSource.kt  # USB camera detection via USBMonitor
│   └── camera/
│       └── UsbCameraController.kt  # USB camera lifecycle & frame capture
├── core/                            # Core utilities
│   ├── di/
│   │   └── AppModule.kt            # Hilt DI module
│   ├── util/                       # Extension functions
│   │   ├── ImageFormatExtensions.kt # CameraX format conversions
│   │   ├── UsbImageFormatExtensions.kt # USB camera format conversions
│   │   └── MathExtensions.kt
│   └── common/
│       └── Constants.kt            # App constants
└── MyNdiApp.kt                      # Application class with @HiltAndroidApp
```

### NDI Module Structure

```
ndi/src/main/
├── java/com/soerjo/ndi/
│   ├── model/
│   │   └── TallyState.kt           # NDI domain model
│   ├── internal/
│   │   └── NDIWrapper.kt           # JNI bindings (private API)
│   ├── NDIManager.kt               # Public API - lifecycle singleton
│   └── NDISender.kt                # Public API - sender instances
├── cpp/
│   ├── ndi_wrapper.cpp             # JNI implementation
│   ├── Include/                    # NDI SDK headers
│   └── CMakeLists.txt              # Native build config
└── jniLibs/
    ├── arm64-v8a/libndi.so         # NDI native library (NOT in repo - manual setup)
    └── armeabi-v7a/libndi.so       # NDI native library (NOT in repo - manual setup)
```

### USB Camera Modules Structure

```
libuvc/
├── src/main/jni/                   # Native JNI implementation (ndk-build)
│   ├── UVCCamera.cpp/h            # UVC camera implementation
│   ├── UVCPreview.cpp/h           # Preview handling
│   └── libjpeg-turbo-1.5.0/       # JPEG codec
└── jniLibs/                        # Pre-built native libraries
    ├── arm64-v8a/libUVCCamera.so
    ├── armeabi-v7a/libUVCCamera.so
    └── ...

libausbc/
└── src/main/java/com/jiangdg/ausbc/
    ├── MultiCameraClient.kt        # Multi-camera management
    ├── camera/CameraUVC.kt         # UVC camera implementation
    ├── base/CameraFragment.kt      # Base fragment for camera operations
    └── utils/                      # Utilities
```

### Key Architecture Patterns

**MVVM with Hilt DI:**
- `CameraViewModel` (`@HiltViewModel`) - Injects use cases, emits `StateFlow<CameraUiState>`
- `CameraScreen` - Collects state via `collectAsStateWithLifecycle()`
- Repository pattern - Interfaces in `domain/repository/`, implementations in `data/repository/`

**Dependency Injection (Hilt):**
- `@HiltAndroidApp` on `MyNdiApp.kt` enables Hilt
- `AppModule` binds repository interfaces to implementations
- `@HiltViewModel` injects use cases into ViewModels
- Uses **KSP** (not kapt) for annotation processing

**CameraInfo Sealed Class:**
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

**NDI Module API:**
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

**CameraX Pipeline:**
```
CameraX → YUV_420_888 → UyvyBufferPool.obtain() → convertYuvToUyvyDirect() →
NDISender.sendFrame() → NDI async send
```

**USB Camera Pipeline:**
```
USBMonitor → UsbCameraController → YUYV/NV21 → convertYuyvToUyvy() →
NDISender.sendFrame() → NDI async send
```

**Image Processing:**
- CameraX: YUV_420_888 → UYVY (NDI native format, 2 bytes/pixel)
- USB Camera: YUYV/NV21 → UYVY (NDI native format, 2 bytes/pixel)
- Buffer pooling via `UyvyBufferPool` reduces GC pressure
- Direct ByteBuffer access avoids copying intermediate arrays

**Tally System:**
```
Native C++ thread (10Hz polling) → NDISender.onTallyStateChange() →
StateFlow<TallyState> → UI observes → tally indicators update
```

### NDI Tally System

The app implements NDI tally feedback:
- Native C++ thread polls `send_get_tally()` at 10Hz
- When state changes, calls `NDISender.onTallyStateChange(isOnPreview, isOnProgram)`
- `NDISender` exposes `tallyState: StateFlow<TallyState>`
- UI observes this state for live indicators (green = live/on-program, yellow = preview)

### Important Constants

- **Min SDK**: 29, **Target SDK**: 36
- **Resolution**: 1920x1080 (1080p)
- **Frame rates**: 30 FPS (default), 60 FPS
- **NDI format**: UYVY (2 bytes per pixel, 4:2:2 subsampling)
- **ABI filters**: arm64-v8a, armeabi-v7a
- **Java**: 11
- **NDK**: r21 or later (for NDI module), 27.0.12077973 (for libnative)
- **CMake**: 3.22.1

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
- USB device filter is defined in `app/src/main/res/xml/usb_device_filter.xml`

### USB Camera Preview
- USB cameras use `UsbCameraController` for lifecycle management
- Preview frames are captured via `IPreviewDataCallBack`
- YUYV/NV21 format is converted to UYVY for NDI streaming

### USB Camera Permissions
- USB host mode permission is required (`android.hardware.usb.host`)
- USB device attachment intent filter for automatic app launch
- Permission is requested via `USBMonitor.requestPermission()`

## Code Patterns

### Composable State Management
- Use `remember { mutableStateOf() }` for local UI state
- Use `derivedStateOf { }` for computed values
- Use `LaunchedEffect` for side effects on state changes

### Camera Binding
The camera is rebound when `selectedCamera` or `isStreaming` changes via `LaunchedEffect`.
- CameraX cameras use `ProcessCameraProvider.bindToLifecycle()`
- USB cameras use `UsbCameraController.openCamera()`

### Performance Patterns
1. **Volatile cache**: `CameraViewModel` uses `@Volatile` for cached streaming state to avoid StateFlow read barrier in hot path (sendFrame called every frame)
2. **Buffer pooling**: `UyvyBufferPool` in `core/util/ImageFormatExtensions.kt` provides buffer pooling to reduce GC pressure from per-frame allocations
3. **Direct ByteBuffer access**: `convertYuvToUyvyDirect()` reads directly from Image.Plane ByteBuffers to avoid intermediate array copies

### Native Lifecycle
Always call `NDIManager.cleanup()` in `Application.onTerminate()` or `Activity.onDestroy()` to properly release native resources and stop the tally polling thread. NDISender instances should be released via `release()` when no longer needed.

### Settings Persistence
Settings use SharedPreferences with helper methods in `SettingsRepositoryImpl`.

### Module Dependencies
- **app** depends on **ndi**, **libausbc** (via `implementation(project(":..."))`)
- **libausbc** depends on **libuvc**, **libnative**
- **ndi** has NO dependencies on **app** (can be used standalone)
