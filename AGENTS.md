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

# Run connected Android tests
./gradlew connectedAndroidTest
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
- Group imports alphabetically by library type
- Order: Android imports, third-party imports, project imports
- Use wildcard imports sparingly (avoid `import *`)
- Example order:
  ```kotlin
  import android.hardware.usb.UsbDevice  // Android framework
  import androidx.lifecycle.ViewModel       // AndroidX
  import kotlinx.coroutines.flow.StateFlow // Third-party
  import com.soerjo.myndicam.domain.model.CameraInfo  // Project imports
  ```

### Formatting
- Use 4-space indentation (no tabs)
- Follow Kotlin official code style (`kotlin.code.style=official`)
- Maximum line length: 120 characters
- Use trailing commas in multi-line function calls and lists
- Blank line between functions and logical sections

### Types
- Use sealed classes for modeling restricted hierarchies (e.g., `CameraInfo`, `UsbConnectionState`)
- Use data classes for models that primarily hold data
- Use enums for fixed sets of values (e.g., `FrameRate`)
- Use StateFlow for reactive state management
- Prefer immutable data classes with `copy()` for state updates

### Naming Conventions
- Classes/Interfaces: PascalCase (e.g., `CameraViewModel`, `NDISender`)
- Functions/Properties: camelCase (e.g., `detectCameras`, `uiState`)
- Constants in companion objects: UPPER_SNAKE_CASE (e.g., `DEFAULT_SOURCE_NAME`)
- Private properties with backing: prefix with underscore (e.g., `_uiState`)
- Composable functions: PascalCase (e.g., `CameraScreen`)
- Package names: lowercase with dots (e.g., `com.soerjo.myndicam.presentation.screen.camera`)

### Error Handling
- Always catch exceptions in initialization and critical paths
- Use try-catch blocks with meaningful error logging
- Return nullable types or Result types where appropriate
- Use Log.d, Log.e, Log.w for logging with TAG constant
- Example:
  ```kotlin
  try {
      val initialized = NDIManager.initialize()
      if (!initialized) {
          Log.e(TAG, "Failed to initialize NDI")
      }
  } catch (e: Exception) {
      Log.e(TAG, "Error initializing NDI", e)
  }
  ```

### Architecture Patterns
- Follow Clean Architecture: presentation/domain/data layers
- Use MVVM pattern with ViewModels and StateFlow
- Use Hilt for dependency injection
- ViewModel state: use `MutableStateFlow` backing, expose as `StateFlow`
- Repository pattern: interfaces in domain, implementations in data
- Use cases: single-responsibility classes in domain/usecase
- Hilt modules: use `@Module` and `@InstallIn` for DI configuration

### Coroutines and Async
- Use `viewModelScope.launch` for ViewModel coroutines
- Use `suspend` functions for long-running operations
- Use `Flow` for reactive data streams
- Use `collectAsStateWithLifecycle` in Composables
- Use `combine` to merge multiple flows
- Avoid blocking calls on main thread

### Compose UI Guidelines
- Use Material 3 components (`androidx.compose.material3`)
- State hoisting: lift state to parent components
- Use `remember` for local state, `rememberSaveable` for persistable state
- Use `LaunchedEffect` for side effects on composition
- Use `collectAsStateWithLifecycle` to observe StateFlow
- Pass lambdas for callbacks to maintain unidirectional data flow

### Comments and Documentation
- Use KDoc comments for all public APIs
- Include parameter and return value descriptions
- Add usage examples in class KDoc
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
      suspend operator fun invoke(): List<CameraInfo> {
          return cameraRepository.detectCameras()
      }
  }
  ```

### Dependencies Management
- Use version catalog in `gradle/libs.versions.toml`
- Define library versions in `[versions]` section
- Define library dependencies in `[libraries]` section
- Use `version.ref` to reference versions
- Example:
  ```toml
  [versions]
  hilt = "2.51.1"
  [libraries]
  hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
  ```

### Code Organization
- Package structure mirrors Clean Architecture layers:
  - `presentation/` - UI layer (Compose, ViewModels)
  - `domain/` - Business logic (models, use cases, repository interfaces)
  - `data/` - Data layer (repository implementations, data sources)
  - `core/` - Core utilities (DI modules, extension functions, constants)
- Use subdirectories for related functionality
- Place repository interfaces in domain, implementations in data

### Native Code (JNI/C++)
- Native NDI code lives in `ndi/src/main/cpp/`
- Use JNI wrapper pattern (`NDIWrapper.kt`)
- Document memory management and lifecycle
- Ensure proper cleanup in `release()` methods

### Testing Guidelines
- Use JUnit 4 for unit tests (`testImplementation(libs.junit)`)
- Use Espresso for UI tests (`androidTestImplementation(libs.androidx.espresso.core)`)
- Use Compose testing for UI tests (`androidx.compose.ui.test.junit4`)
- Mock dependencies with appropriate libraries
- Test critical paths and edge cases
- Use descriptive test names: `should_return_correct_camera_list_when_available`

### Performance Considerations
- Use `@Volatile` for thread-safe single-read scenarios (e.g., `isStreaming` flag)
- Use buffer pooling for frequent allocations (e.g., `UyvyBufferPool`)
- Avoid StateFlow reads in hot paths (use volatile cache)
- Use `ByteArray` instead of `ByteBuffer` for performance-critical paths
- Consider using `synchronized` for thread-safe operations

### Project Configuration
- Min SDK: 29 (Android 10)
- Target SDK: 36
- Compile SDK: 36
- JVM target: 11
- Kotlin: 1.9.22
- Gradle: Kotlin DSL
- Compose compiler version: 1.5.8

### Special Notes
- NDI SDK must be manually added to `ndi/src/main/jniLibs/` (see NDI_SETUP.md)
- The app supports both internal (CameraX) and USB cameras
- USB camera support requires runtime permissions handling
- Video format: UYVY (4:2:2 packed) for efficient streaming
- Default resolution: 1280x720 (720p)
- Target aspect ratio: 16:9
