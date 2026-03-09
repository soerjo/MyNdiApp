# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MyNdiCam (package: `com.soerjo.myndicam`) - Android NDI Camera Streamer that sends live camera video over NDI to receivers like OBS Studio, vMix, etc.

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
```

## Architecture

### Module Structure

The project is organized into **two modules**:

1. **`app`** - Main application (camera + UI)
2. **`ndi`** - Standalone NDI library module (reusable across projects)

### Technology Stack
- **UI**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture (presentation/domain/data layers)
- **DI**: Hilt (dagger.hilt.android)
- **Camera**: CameraX (camera-core, camera-camera2, camera-lifecycle, camera-view)
- **Native**: C++ JNI with NDI SDK (Processing.NDI v6.3 via dynamic loading)
- **Build**: Gradle with Kotlin DSL, CMake for native code

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
│   │   ├── CameraInfo.kt
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
│   └── datasource/
│       └── CameraDataSource.kt     # CameraX operations
├── core/                            # Core utilities
│   ├── di/
│   │   └── AppModule.kt            # Hilt DI module
│   ├── util/                       # Extension functions
│   │   ├── ImageFormatExtensions.kt
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

### Key Architecture Patterns

**MVVM with Hilt DI:**
- `CameraViewModel` (`@HiltViewModel`) - Injects use cases, emits `StateFlow<CameraUiState>`
- `CameraScreen` - Collects state via `collectAsStateWithLifecycle()`
- Repository pattern - Interfaces in `domain/repository/`, implementations in `data/repository/`

**Dependency Injection (Hilt):**
- `@HiltAndroidApp` on `MyNdiApp.kt` enables Hilt
- `AppModule` binds repository interfaces to implementations
- `@HiltViewModel` injects use cases into ViewModels

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

**Camera Pipeline:**
```
CameraX → YUV_420_888 → convertYuvToUyvy() (core/util/) →
NDISender.sendFrame() → NDI async send
```

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
- **Resolution**: 1280x720 (720p)
- **Frame rates**: 30 FPS (default), 60 FPS
- **NDI format**: UYVY (2 bytes per pixel, 4:2:2 subsampling)
- **ABI filters**: arm64-v8a, armeabi-v7a

## NDI SDK Setup (Required)

The NDI SDK is NOT included in the repository. You must manually add it:

1. Download from https://ndi.video/download/
2. Copy `libndi.so` to:
   - `ndi/src/main/jniLibs/arm64-v8a/libndi.so`
   - `ndi/src/main/jniLibs/armeabi-v7a/libndi.so`

The native build dynamically loads the NDI library via `Processing.NDI.DynamicLoad.h`.

## Code Patterns

### Composable State Management
- Use `remember { mutableStateOf() }` for local UI state
- Use `derivedStateOf { }` for computed values
- Use `LaunchedEffect` for side effects on state changes

### Camera Binding
The camera is rebound when `selectedCamera` or `isStreaming` changes via `LaunchedEffect`.

### Native Lifecycle
Always call `NDIManager.cleanup()` in `Application.onTerminate()` or `Activity.onDestroy()` to properly release native resources and stop the tally polling thread. NDISender instances should be released via `release()` when no longer needed.

### Settings Persistence
Settings use SharedPreferences with helper methods in `SettingsRepositoryImpl`.

### Module Dependencies
- **app** depends on **ndi** (via `implementation(project(":ndi"))`)
- **ndi** has NO dependencies on **app** (can be used standalone)
