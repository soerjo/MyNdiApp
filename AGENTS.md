# AGENTS.md

This file contains guidelines and commands for agentic coding assistants working on this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run unit tests for a specific class
./gradlew test --tests "com.soerjo.myndicam.presentation.screen.camera.CameraViewModelTest"

# Run instrumented tests
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

## Code Style Guidelines

### Formatting & Types
- 4-space indentation, `kotlin.code.style=official`, max 120 chars
- Group imports: Android → AndroidX → third-party → project
- Sealed classes for hierarchies (`CameraInfo`, `UsbConnectionState`)
- Data classes for models, enums for fixed sets (`FrameRate`)
- StateFlow for reactive state, immutable data classes with `copy()`

### Naming Conventions
- Classes/Interfaces: PascalCase (`CameraViewModel`, `NDISender`)
- Functions/Properties: camelCase (`detectCameras`, `uiState`)
- Constants: UPPER_SNAKE_CASE (`DEFAULT_SOURCE_NAME`)
- Private backing properties: `_uiState`
- Packages: lowercase with dots (`com.soerjo.myndicam.presentation.screen.camera`)

### Error Handling
- Catch exceptions in initialization and critical paths with meaningful logging
- Return nullable or Result types, use Log.d/Log.e/Log.w with TAG

### Architecture
- Clean Architecture: presentation/domain/data layers
- MVVM with ViewModels and StateFlow, Hilt for DI
- ViewModel state: MutableStateFlow backing, expose as StateFlow
- Repository interfaces in domain, implementations in data
- Use cases in domain/usecase with single responsibility

### Coroutines & Compose
- Use `viewModelScope.launch`, `suspend` for long-running operations, `Flow` for streams
- Use `collectAsStateWithLifecycle` in Composables
- Material 3, state hoisting, `remember`/`rememberSaveable`, `LaunchedEffect` for side effects

### Dependencies
- Use version catalog in `gradle/libs.versions.toml`
- Define versions in `[versions]`, libraries in `[libraries]`, reference with `version.ref`

## Code Organization

- `presentation/` - UI (Compose, ViewModels)
- `domain/` - Business logic (models, use cases, repository interfaces)
- `data/` - Data layer (repository implementations, data sources)
- `core/` - Utilities (DI, extensions, constants, managers)
- Managers in `core/manager/` for complex logic (NdiManager, UsbCameraManager)

## Multi-Module Architecture

**app** - Main module with Clean Architecture, depends on ndi and libausbc
**ndi** - NDI v6.3 JNI wrapper, native C++ via CMake, reusable
**libausbc** - Android USB Camera via UVC protocol
**libuvc** - Native UVC implementation via NDK build system
**libnative** - Native utilities via CMake

## Native Code

- NDI: `ndi/src/main/cpp/`, UVC: `libuvc/src/main/jni/`, utilities: `libnative/src/main/cpp/`
- NDK: 27.0.12077973, C++ standard: C++17
- CMake for ndi/libnative, NDK build (ndk-build) for libuvc
- Document memory management, ensure proper cleanup in `release()`

## Testing

- JUnit 4 for unit tests, Espresso/Compose testing for UI
- Mock dependencies, test critical paths and edge cases
- Descriptive names: `should_return_correct_camera_list_when_available`

## Performance

- Use `@Volatile` for thread-safe single-read (e.g., `isStreaming`)
- Avoid StateFlow in hot paths (use volatile cache)
- Use `ByteArray` instead of `ByteBuffer` for performance-critical paths

## Configuration

- Min SDK: 29, Target SDK: 36, Compile SDK: 36
- JVM target: 11, Kotlin: 1.9.22, Compose compiler: 1.5.8
- ABIs: arm64-v8a, armeabi-v7a

## Special Notes

- NDI SDK manually added to `ndi/src/main/jniLibs/` (see NDI_SETUP.md)
- Supports CameraX (Front/Back) and USB cameras via UVC
- Video format: UYVY (4:2:2), 1280x720, 16:9
- Frame pipeline: YUV_420_888/NV21/MJPEG → RGBA → UYVY
- USB cameras require runtime permissions, support multiple simultaneously
- Use converters in `core/util/conversion/` (FrameConverter, Yuv420Converter, RgbaConverter, Nv21Converter)
