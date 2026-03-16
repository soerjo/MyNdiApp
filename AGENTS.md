# AGENTS.md

This file contains guidelines and commands for agentic coding assistants working on this repository.

## Build Commands

### Basic Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Install debug APK to connected device
./gradlew installDebug
```

### Testing Commands
```bash
# Run all unit tests
./gradlew test

# Run unit tests for a specific module
./gradlew :app:test

# Run unit tests for a specific class
./gradlew test --tests "com.soerjo.myndicam.presentation.screen.camera.CameraViewModelTest"

# Run connected Android instrumented tests
./gradlew connectedAndroidTest

# Run instrumented tests for a specific class
./gradlew connectedAndroidTest --tests "com.soerjo.myndicam.presentation.screen.camera.CameraScreenTest"
```

### Code Quality Commands
```bash
# Run lint checks
./gradlew lint

# Run lint for specific module
./gradlew :app:lint

# Generate lint report
./gradlew lintDebug
```

## Code Style Guidelines

### Imports
- Group imports alphabetically: Android → AndroidX → third-party → project
- Use wildcard imports sparingly (avoid `import *`)
- Example:
  ```kotlin
  import android.hardware.usb.UsbDevice
  import androidx.lifecycle.ViewModel
  import kotlinx.coroutines.flow.StateFlow
  import com.soerjo.myndicam.domain.model.CameraInfo
  ```

### Formatting
- 4-space indentation (no tabs)
- Follow `kotlin.code.style=official`
- Max line length: 120 characters
- Use trailing commas in multi-line function calls and lists
- Blank line between functions and logical sections

### Types
- Sealed classes for restricted hierarchies (e.g., `CameraInfo`, `UsbConnectionState`)
- Data classes for models that primarily hold data
- Enums for fixed sets (e.g., `FrameRate`)
- StateFlow for reactive state management
- Prefer immutable data classes with `copy()` for state updates

### Naming Conventions
- Classes/Interfaces: PascalCase (`CameraViewModel`, `NDISender`)
- Functions/Properties: camelCase (`detectCameras`, `uiState`)
- Constants: UPPER_SNAKE_CASE (`DEFAULT_SOURCE_NAME`)
- Private properties with backing: `_uiState`
- Composable functions: PascalCase (`CameraScreen`)
- Packages: lowercase with dots (`com.soerjo.myndicam.presentation.screen.camera`)

### Error Handling
- Always catch exceptions in initialization and critical paths
- Use try-catch with meaningful error logging
- Return nullable or Result types where appropriate
- Use Log.d/Log.e/Log.w with TAG constant
- Example:
  ```kotlin
  try {
      val initialized = NDIManager.initialize()
      if (!initialized) Log.e(TAG, "Failed to initialize NDI")
  } catch (e: Exception) {
      Log.e(TAG, "Error initializing NDI", e)
  }
  ```

### Architecture Patterns
- Clean Architecture: presentation/domain/data layers
- MVVM with ViewModels and StateFlow
- Hilt for DI
- ViewModel state: `MutableStateFlow` backing, expose as `StateFlow`
- Repository pattern: interfaces in domain, implementations in data
- Use cases: single-responsibility classes in domain/usecase
- Hilt modules: `@Module` and `@InstallIn` for DI configuration

### Coroutines and Async
- Use `viewModelScope.launch` for ViewModel coroutines
- Use `suspend` for long-running operations
- Use `Flow` for reactive streams
- Use `collectAsStateWithLifecycle` in Composables
- Use `combine` to merge multiple flows
- Avoid blocking calls on main thread

### Compose UI Guidelines
- Use Material 3 (`androidx.compose.material3`)
- State hoisting: lift state to parent components
- Use `remember` for local state, `rememberSaveable` for persistable
- Use `LaunchedEffect` for side effects on composition
- Pass lambdas for callbacks to maintain unidirectional data flow

### Comments and Documentation
- Use KDoc for all public APIs with parameter/return descriptions
- Private functions don't require KDoc
- Add inline comments for complex logic
- Example:
  ```kotlin
  /**
   * Use case for detecting available cameras
   */
  class DetectCamerasUseCase @Inject constructor(
      private val cameraRepository: CameraRepository
  ) {
      suspend operator fun invoke(): List<CameraInfo> = cameraRepository.detectCameras()
  }
  ```

### Dependencies Management
- Use version catalog in `gradle/libs.versions.toml`
- Define versions in `[versions]`, libraries in `[libraries]`
- Use `version.ref` to reference versions
- Example:
  ```toml
  [versions]
  hilt = "2.51.1"
  [libraries]
  hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
  ```

### Code Organization
- Package structure mirrors Clean Architecture layers
  - `presentation/` - UI layer (Compose, ViewModels)
  - `domain/` - Business logic (models, use cases, repository interfaces)
  - `data/` - Data layer (repository implementations, data sources)
  - `core/` - Utilities (DI, extensions, constants, managers)
- Repository interfaces in domain, implementations in data
- Managers in `core/manager/` for complex business logic (NdiManager, UsbCameraManager)

### Multi-Module Architecture
The project uses a modular design with the following modules:

**app** - Main application module
- Implements Clean Architecture (presentation/domain/data/core)
- Depends on ndi and libausbc modules
- Contains UI, business logic, and data implementations

**ndi** - NDI library module (standalone)
- JNI wrapper for Processing.NDI v6.3 SDK
- Public API: NDIManager (singleton), NDISender (frame transmission)
- Native C++ code built with CMake
- Independent of app module, can be reused in other projects

**libausbc** - Android USB Camera library
- Provides USB camera support via UVC protocol
- Depends on libuvc and libnative modules
- Handles USB device detection, connection, and frame capture
- Third-party library (jiangdg/ausbc)

**libuvc** - UVC native library
- Native implementation of UVC (USB Video Class) protocol
- JNI bindings for USB camera communication
- Built with NDK build system (ndk-build)
- Contains rapidjson for configuration parsing

**libnative** - Native utilities
- Common native utilities and helper functions
- Built with CMake
- Shared by libuvc and other native components

### Native Code (JNI/C++)
- Native NDI code: `ndi/src/main/cpp/`
- Native UVC code: `libuvc/src/main/jni/`
- Native utilities: `libnative/src/main/cpp/`
- Use JNI wrapper pattern (e.g., `NDIWrapper.kt`)
- Document memory management and lifecycle
- Ensure proper cleanup in `release()` methods
- For ndi module: Use CMake build system
- For libuvc module: Use NDK build system (ndk-build)
- NDK version: 27.0.12077973
- C++ standard: C++17

### Testing Guidelines
- JUnit 4 for unit tests
- Espresso for UI tests
- Compose testing for UI tests
- Mock dependencies appropriately
- Test critical paths and edge cases
- Descriptive test names: `should_return_correct_camera_list_when_available`

### Performance Considerations
- Use `@Volatile` for thread-safe single-read scenarios (e.g., `isStreaming`)
- Avoid StateFlow reads in hot paths (use volatile cache)
- Use `ByteArray` instead of `ByteBuffer` for performance-critical paths

### Project Configuration
- Min SDK: 29, Target SDK: 36, Compile SDK: 36
- JVM target: 11, Kotlin: 1.9.22
- Compose compiler: 1.5.8
- ABIs: arm64-v8a, armeabi-v7a

### Special Notes
- NDI SDK must be manually added to `ndi/src/main/jniLibs/` (see NDI_SETUP.md)
- Supports internal (CameraX) and USB cameras
- Video format: UYVY (4:2:2 packed), 1280x720 (720p), 16:9 aspect ratio
- USB camera support requires runtime permissions handling
- USB cameras use UVC (USB Video Class) protocol via libausbc and libuvc
- Frame conversion pipeline: YUV_420_888/NV21/MJPEG → RGBA → UYVY for NDI

### USB Camera Architecture
- USB camera detection via `UsbCameraDataSource`
- Camera management via `UsbCameraManager` in core/manager
- USB camera controller in `data/camera/usb/UsbCameraController`
- Native UVC communication through libausbc and libuvc modules
- Runtime USB permission handling required
- Supports multiple USB cameras simultaneously

### Frame Conversion
- Use converter utilities in `core/util/conversion/`
- Available converters: FrameConverter, Yuv420Converter, RgbaConverter, Nv21Converter
- Handle different input formats from various camera sources
- Convert to UYVY format for NDI transmission
- Support cropping and aspect ratio adjustments
- Use extensions in `core/util/` for image format operations

### Camera Types
- CameraX: Front, Back (internal cameras via CameraX API)
- USB: External UVC cameras (webcams, capture devices)
- Both types implement sealed class `CameraInfo` in domain/model
- Selectable from camera menu in UI
- SettingsRepository persists camera selection
