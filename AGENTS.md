# AGENTS.md

This file provides guidance to agentic coding assistants working on this repository.

## Build & Test Commands

```bash
# Build commands
./gradlew assembleDebug              # Build debug APK
./gradlew assembleRelease            # Build release APK
./gradlew :app:assembleDebug         # Build specific module
./gradlew :ndi:assembleDebug
./gradlew clean                      # Clean build outputs

# Install to connected device
./gradlew installDebug

# Test commands (Junit 4 + Espresso)
./gradlew test                       # Run unit tests
./gradlew connectedAndroidTest       # Run instrumented tests
./gradlew :app:test --tests CameraViewModelTest   # Run single test class
./gradlew :app:test --tests "CameraViewModel.*"  # Run tests matching pattern

# Lint/typecheck (check project files for available commands)
./gradlew lint                       # Run Android lint
./gradlew ktlintCheck                # If ktlint is configured
./gradlew detekt                     # If detekt is configured
```

## Code Style Guidelines

### Imports
- Alphabetical order, grouped by Android/Jetpack → third-party → project
- No wildcard imports (e.g., `import android.view.*`)

### Naming Conventions
- Classes: PascalCase (`CameraViewModel`, `NDIManager`)
- Functions/variables: camelCase (`saveSourceName`, `uiState`)
- Constants: UPPER_SNAKE_CASE (`TAG`, `DEFAULT_SOURCE_NAME`)
- Private backing fields: underscore prefix (`_uiState`, `_sourceName`)
- Sealed class subclasses: PascalCase (`CameraInfo.CameraX`, `CameraInfo.Usb`)
- Use companion objects for constants and factory methods

### Types & Data Structures
- Use `sealed class` for type hierarchies with shared behavior (e.g., `CameraInfo`)
- Use `enum class` for fixed sets of values (`CameraType`, `FrameRate`)
- Use `data class` for data holders (`CameraUiState`, `CameraInfo.Usb`)
- Use `object` for singletons (`NDIManager`, `UyvyBufferPool`)
- Use `Flow`/`StateFlow` for reactive streams - never `LiveData`
- Use `@Volatile` for cached state accessed from hot paths (e.g., `isStreaming` in ViewModel)

### Architecture Patterns
- **Clean Architecture**: presentation/ → domain/ → data/ layers
- **MVVM with Hilt**: ViewModels inject use cases, Composables observe StateFlow
- **ViewModel in Composable**: Always use `hiltViewModel()`, never `EntryPointAccessors`
- **State collection**: Use `collectAsStateWithLifecycle()` for StateFlow
- **Local UI state**: Use `remember { mutableStateOf() }` for transient UI state

**Correct pattern:**
```kotlin
@Composable
fun Screen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // UI renders from uiState, calls viewModel methods
}
```

**Anti-pattern - Don't do this:**
- Don't inject dependencies directly in Composable
- Don't put business logic in Composable (NDI init, settings save, etc.)
- Don't call viewModel methods in LaunchedEffect unless it's a one-time action

### Hilt Dependency Injection
- `@HiltAndroidApp` on Application class
- `@HiltViewModel` for ViewModels with constructor injection
- `@Module @InstallIn(SingletonComponent::class)` for DI modules
- `@Binds` for interface → implementation binding
- `@Singleton` for singleton-scoped dependencies
- Use `@Inject constructor(...)` for all DI

### Error Handling
- Always wrap native calls in try-catch with `Log.e(TAG, "message", e)`
- Use `IllegalStateException` for invalid state operations
- Log messages use `TAG` constant (private field or companion object)
- Format: `Log.d(TAG, "message")` or `Log.e(TAG, "message", exception)`
- Return graceful defaults (empty list, false, null) instead of crashing

### Performance Patterns
- **Buffer pooling**: Use `UyvyBufferPool` to reduce GC pressure for frame data
- **Volatile cache**: `@Volatile` for frequently accessed state (avoid StateFlow read barrier)
- **Direct ByteBuffer access**: Read directly from Image.Plane ByteBuffers, avoid copies
- **Native lifecycle**: Always call `NDIManager.cleanup()` in `Application.onTerminate()` or `Activity.onDestroy()`

### Composable Best Practices
- Use `Modifier.fillMaxSize()` for full-screen layouts
- Use `Modifier.padding(12.dp)` or `padding(8.dp)` for spacing
- Use `MaterialTheme` colors and typography
- Remember animations with `rememberInfiniteTransition` or `animateFloatAsState`
- Use `LaunchedEffect` for side effects on state changes
- Keep Composables pure - no side effects, only UI rendering

### Kotlin Style
- Use expression bodies for single-line functions: `fun isRunning(): Boolean = isRunning`
- Use Kotlin scope functions: `let`, `apply`, `run`, `with`, `also` judiciously
- Use string templates: `"Selected: ${camera.name}"` instead of concatenation
- Use `require()` or `check()` for preconditions
- Use `?:` for elvis operator and default values
- Prefer immutable `val` over mutable `var` where possible

### Module Structure
- **app**: Main application (UI + orchestration)
- **ndi**: Standalone NDI library module (reusable, no app dependency)
- **libuvc**: Native JNI library for USB UVC (ndk-build)
- **libnative**: Native YUV utilities (CMake)
- **libausbc**: USB Camera library (Kotlin wrapper)

### Important Constants
- Min SDK: 29, Target SDK: 36
- Java: 11, Kotlin: 1.9.22
- NDI format: UYVY (2 bytes/pixel, 4:2:2 subsampling)
- Target resolution: 1920x1080 (1080p)
- Frame rates: 30 FPS (default), 60 FPS
- ABI filters: arm64-v8a, armeabi-v7a

### Native Code Notes
- NDI SDK is NOT in repo - must manually add to `ndi/src/main/jniLibs/`
- Native C++ code uses NDK r21 or later
- CMake 3.22.1 for libnative, ndk-build for libuvc
- Always release native resources: `NDISender.release()`, `NDIManager.cleanup()`

### State Management in ViewModels
- Expose immutable `StateFlow<T>` from `MutableStateFlow<T>`
- Update state using `.copy()`: `_uiState.value = _uiState.value.copy(isLoading = false)`
- Use `viewModelScope.launch { }` for coroutines
- Never expose mutable state - always return `asStateFlow()`
- Use `@Volatile` for frequently accessed boolean flags

### Native Resource Management
Critical: Always call `NDIManager.cleanup()` to stop tally polling thread.
Critical: Always call `NDISender.release()` when sender is no longer needed.
Critical: USB camera controllers must be cleaned up in `ViewModel.onCleared()`
