# Architecture Documentation

## Table of Contents

- [Overview](#overview)
- [Architecture Principles](#architecture-principles)
- [Module Structure](#module-structure)
- [Clean Architecture Layers](#clean-architecture-layers)
- [Design Patterns](#design-patterns)
- [Data Flow](#data-flow)
- [Dependency Injection](#dependency-injection)
- [State Management](#state-management)
- [Native Integration](#native-integration)
- [Testing Strategy](#testing-strategy)

## Overview

MyNdiCam follows **Clean Architecture** principles with clear separation of concerns. The project is organized into two main modules:

1. **app** - Main application with camera and UI functionality
2. **ndi** - Standalone NDI library module

This architecture ensures:
- **Testability** - Each layer can be tested independently
- **Maintainability** - Clear boundaries make code easier to understand
- **Scalability** - Easy to add features without affecting existing code
- **Reusability** - NDI module can be used in other projects

## Architecture Principles

### 1. Separation of Concerns
Each layer has specific responsibilities:
- **Presentation** - UI and user interactions
- **Domain** - Business logic and rules
- **Data** - Data sources and implementations

### 2. Dependency Rule
Dependencies point inward:
```
Presentation → Domain → Data
```
Outer layers depend on inner layers, never the opposite.

### 3. Abstraction
- Domain layer defines interfaces (repositories, use cases)
- Data layer implements these interfaces
- Presentation layer depends on abstractions, not implementations

### 4. Reusability
- NDI module has no dependencies on app module
- Can be used in other projects independently
- Clear API boundaries

## Module Structure

### Module: app

Main application module containing camera and UI functionality.

```
app/
├── presentation/                 # Presentation layer
│   ├── MainActivity.kt          # Activity entry point
│   └── screen/camera/
│       ├── CameraScreen.kt      # Main composable
│       ├── CameraViewModel.kt   # State management
│       └── components/
│           └── CameraDialogs.kt # Reusable UI components
│
├── domain/                       # Domain layer
│   ├── model/                   # Domain models
│   │   ├── CameraInfo.kt
│   │   ├── CameraType.kt
│   │   └── FrameRate.kt
│   ├── repository/              # Repository interfaces
│   │   ├── CameraRepository.kt
│   │   └── SettingsRepository.kt
│   └── usecase/                 # Use cases
│       ├── DetectCamerasUseCase.kt
│       ├── ObserveSettingsUseCase.kt
│       └── SaveSettingsUseCase.kt
│
├── data/                         # Data layer
│   ├── repository/              # Repository implementations
│   │   ├── CameraRepositoryImpl.kt
│   │   └── SettingsRepositoryImpl.kt
│   └── datasource/              # Data sources
│       └── CameraDataSource.kt
│
└── core/                         # Core utilities
    ├── di/
    │   └── AppModule.kt         # Hilt DI module
    ├── util/
    │   ├── ImageFormatExtensions.kt
    │   └── MathExtensions.kt
    └── common/
        └── Constants.kt
```

### Module: ndi

Standalone NDI library module with no dependencies on app module.

```
ndi/
├── model/                       # Domain models
│   └── TallyState.kt           # Tally state data class
│
├── internal/                    # Internal implementation
│   └── NDIWrapper.kt           # JNI bindings (private)
│
├── NDIManager.kt               # Public API - lifecycle
├── NDISender.kt                # Public API - sender
│
└── cpp/                         # Native code
    ├── ndi_wrapper.cpp         # JNI implementation
    ├── Include/                # NDI SDK headers
    └── CMakeLists.txt          # Native build config
```

## Clean Architecture Layers

### Presentation Layer

**Responsibilities:**
- Display UI to user
- Handle user interactions
- Observe and display state
- Navigate between screens

**Components:**
- **Activities** - Entry points, permission handling
- **Composables** - UI components with Jetpack Compose
- **ViewModels** - State management with StateFlow

**Example:**
```kotlin
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // UI code
}
```

### Domain Layer

**Responsibilities:**
- Define business logic
- Declare repository interfaces
- Contain use cases
- Define domain models

**Components:**
- **Models** - Pure data classes
- **Repositories (interfaces)** - Data access contracts
- **Use Cases** - Business logic operations

**Example:**
```kotlin
class DetectCamerasUseCase @Inject constructor(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): List<CameraInfo> {
        return cameraRepository.detectCameras()
    }
}
```

### Data Layer

**Responsibilities:**
- Implement repository interfaces
- Manage data sources
- Handle external APIs (CameraX, SharedPreferences)
- Provide data to domain layer

**Components:**
- **Repository Implementations** - Concrete implementations
- **Data Sources** - External data access
- **Mappers** - Convert between data models

**Example:**
```kotlin
@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val cameraDataSource: CameraDataSource
) : CameraRepository {
    override suspend fun detectCameras(): List<CameraInfo> {
        return cameraDataSource.detectCameras(cameraProvider)
    }
}
```

## Design Patterns

### MVVM (Model-View-ViewModel)

```
┌─────────────┐
│    View     │  (Composable)
│ (CameraScreen)│
└──────┬──────┘
       │ observes
       ↓
┌─────────────┐
│ ViewModel  │  (CameraViewModel)
└──────┬──────┘
       │ uses
       ↓
┌─────────────┐
│   Model     │  (Use Cases + Repositories)
└─────────────┘
```

### Repository Pattern

```
┌─────────────┐
│   Use Case  │
└──────┬──────┘
       │ depends on interface
       ↓
┌─────────────┐
│  Repository │  (Interface in domain)
│  Interface  │
└──────┬──────┘
       │ implemented by
       ↓
┌─────────────┐
│ Repository  │  (Implementation in data)
│   Impl      │
└──────┬──────┘
       │ uses
       ↓
┌─────────────┐
│ Data Source │  (CameraX, SharedPreferences)
└─────────────┘
```

### Use Case Pattern

Encapsulates business logic in reusable classes:

```kotlin
class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    fun getSourceName(): Flow<String> = settingsRepository.getSourceName()
    fun getFrameRate(): Flow<FrameRate> = settingsRepository.getFrameRate()
}
```

### Dependency Injection (Hilt)

- **@HiltAndroidApp** - Application class
- **@HiltViewModel** - ViewModels
- **@Inject** - Constructor injection
- **@Module** - Dependency bindings
- **@Singleton** - Single instances

## Data Flow

### Camera Streaming Flow

```
1. User taps "Start Streaming"
   ↓
2. CameraViewModel.toggleStreaming()
   ↓
3. StateFlow updates isStreaming = true
   ↓
4. LaunchedEffect in CameraScreen reacts
   ↓
5. CameraX ImageAnalysis starts
   ↓
6. Frames captured → YUV_420_888
   ↓
7. ImageFormatExtensions.convertYuvToUyvy()
   ↓
8. CameraViewModel.sendFrame()
   ↓
9. NDISender.sendFrame()
   ↓
10. NDIManager → Network
```

### Tally State Flow

```
1. Native C++ thread (10Hz)
   ↓
2. NDI send_get_tally()
   ↓
3. JNI callback to NDIWrapper.TallyCallback
   ↓
4. NDISender.onTallyStateChange()
   ↓
5. MutableStateFlow<TallyState>.emit()
   ↓
6. CameraViewModel observes tallyState
   ↓
7. UI State updates with new tally state
   ↓
8. CameraScreen recomposes with indicators
```

### Settings Persistence Flow

```
1. User changes setting
   ↓
2. SaveSettingsUseCase.saveFrameRate()
   ↓
3. SettingsRepository.saveFrameRate()
   ↓
4. SharedPreferences.edit().putInt()
   ↓
5. MutableStateFlow emits new value
   ↓
6. UI observes and updates
```

## Dependency Injection

### Hilt Modules

**AppModule** - App-level bindings:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindCameraRepository(
        impl: CameraRepositoryImpl
    ): CameraRepository
}
```

### Injection Points

**ViewModel Injection:**
```kotlin
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val detectCamerasUseCase: DetectCamerasUseCase,
    // ...
) : ViewModel()
```

**Repository Injection:**
```kotlin
@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val cameraDataSource: CameraDataSource
) : CameraRepository
```

## State Management

### StateFlow Pattern

```kotlin
// ViewModel
private val _uiState = MutableStateFlow(CameraUiState())
val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

// Update
_uiState.value = _uiState.value.copy(isStreaming = true)
```

### Lifecycle-Aware Collection

```kotlin
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // UI automatically unsubscribes when not visible
}
```

### UI State Data Class

```kotlin
data class CameraUiState(
    val isStreaming: Boolean = false,
    val availableCameras: List<CameraInfo> = emptyList(),
    val selectedCamera: CameraInfo? = null,
    val selectedFrameRate: FrameRate = FrameRate.FPS_30,
    val actualResolution: Size = Size(1280, 720),
    val tallyState: TallyState = TallyState(),
    val sourceName: String = Constants.DEFAULT_SOURCE_NAME,
    val isLoading: Boolean = true
)
```

## Native Integration

### JNI Bridge Architecture

```
Kotlin (NDIWrapper)
    ↓ external functions
C++ (ndi_wrapper.cpp)
    ↓ loads dynamically
NDI SDK (libndi.so)
```

### JNI Function Naming

Package-specific function names:
```cpp
// Java: com.soerjo.ndi.internal.NDIWrapper
JNIEXPORT jboolean JNICALL
Java_com_soerjo_ndi_internal_NDIWrapper_nativeInitialize(...)
```

### Data Conversion

```
Java ByteArray
    ↓
C++ jbyteArray
    ↓
GetByteArrayElements
    ↓
uint8_t*
    ↓
NDIlib_video_frame_v2_t
```

### Tally Callback Threading

```
C++ Thread (10Hz polling)
    ↓
JNI AttachCurrentThread
    ↓
CallVoidMethod (Java callback)
    ↓
NDISender.onTallyStateChange()
    ↓
StateFlow update
    ↓
UI observes change
```

## Testing Strategy

### Unit Tests

**Domain Layer:**
- Use case tests with fake repositories
- Model validation tests

**Data Layer:**
- Repository tests with fake data sources
- Extension function tests

**Presentation Layer:**
- ViewModel tests with fake use cases

### Integration Tests

**CameraX Integration:**
- Camera detection tests
- Frame processing tests

**NDI Integration:**
- Sender lifecycle tests
- Tally state tests

### Instrumentation Tests

**UI Tests:**
- Composable UI tests
- Navigation tests
- User flow tests

## Performance Considerations

### Image Processing

- **UYVY Format** - 2 bytes/pixel vs 4 for BGRA
- **Async API** - `send_send_video_async_v2()` for non-blocking sends
- **Frame Queue** - Limited depth (2) to prevent memory buildup

### Memory Management

- **Frame Recycling** - ImageProxy closed after processing
- **Native Cleanup** - Proper JNI resource cleanup
- **Lifecycle Awareness** - Streaming stops when not visible

### Network Optimization

- **Direct Streaming** - No transcoding overhead
- **Native Format** - UYVY matches NDI expectations
- **Efficient Encoding** - Hardware-accelerated where available

## Security Considerations

### Permissions

- Minimal required permissions
- Runtime permission requests
- Graceful handling of denials

### Network

- Local network only
- No internet communication
- No data collection

### Native Code

- Validates all JNI inputs
- Proper memory management
- No buffer overflows

## Future Architecture Improvements

1. **Multi-Module Structure**
   - Separate feature modules
   - Core module for shared code

2. **Testing Infrastructure**
   - Fake implementations for all dependencies
   - Test utilities

3. **Error Handling**
   - Result type for operations
   - User-friendly error messages

4. **Offline Support**
   - Cache camera configuration
   - Offline mode indicators

5. **Analytics**
   - Crash reporting
   - Usage analytics (opt-in)
