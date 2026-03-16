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
  - `core/` - Utilities (DI, extensions, constants)
- Repository interfaces in domain, implementations in data

### Native Code (JNI/C++)
- Native NDI code: `ndi/src/main/cpp/`
- Use JNI wrapper pattern (`NDIWrapper.kt`)
- Document memory management and lifecycle
- Ensure proper cleanup in `release()` methods

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
