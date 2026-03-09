# NDI Module API Documentation

## Overview

The NDI module (`:ndi`) is a standalone Android library that provides NDI (Network Device Interface) streaming functionality. It can be used independently in any Android application.

**Package:** `com.soerjo.ndi`

## Table of Contents

- [Installation](#installation)
- [Public API](#public-api)
- [Domain Models](#domain-models)
- [Usage Examples](#usage-examples)
- [Error Handling](#error-handling)
- [Thread Safety](#thread-safety)
- [Performance](#performance)

## Installation

### Gradle Dependency

```gradle
dependencies {
    implementation(project(":ndi"))
}
```

### Maven (Future Publishing)

```gradle
dependencies {
    implementation("com.soerjo:ndi:1.0.0")
}
```

## Public API

### NDIManager

Singleton object managing global NDI library lifecycle.

#### Methods

##### `initialize(): Boolean`

Initialize the NDI library. Must be called once before any NDI operations.

**Returns:** `true` if successful, `false` otherwise

**Throws:** None (errors are logged)

**Usage:**
```kotlin
val success = NDIManager.initialize()
if (!success) {
    // Handle initialization failure
}
```

**Thread Safety:** Thread-safe (idempotent - multiple calls are safe)

##### `createSender(sourceName: String): NDISender`

Create a new NDI sender instance.

**Parameters:**
- `sourceName` - The name that will be visible to NDI receivers

**Returns:** A new `NDISender` instance

**Throws:**
- `IllegalStateException` - if NDI is not initialized

**Usage:**
```kotlin
try {
    val sender = NDIManager.createSender("My Camera")
    // Use sender
} catch (e: IllegalStateException) {
    // NDI not initialized
}
```

##### `cleanup()`

Cleanup NDI resources. Should be called once at application shutdown.

**Throws:** None (errors are logged)

**Usage:**
```kotlin
override fun onTerminate() {
    super.onTerminate()
    NDIManager.cleanup()
}
```

**Thread Safety:** Thread-safe (idempotent)

##### `isInitialized(): Boolean`

Check if NDI is currently initialized.

**Returns:** `true` if initialized, `false` otherwise

---

### NDISender

Represents a single NDI sender that streams video to the network.

#### Lifecycle

```
Created → Running → Released
```

#### Methods

##### `sendFrame(data: ByteArray, width: Int, height: Int, stride: Int): Boolean`

Send a video frame via NDI.

**Parameters:**
- `data` - Frame data in UYVY format (2 bytes per pixel)
- `width` - Frame width in pixels
- `height` - Frame height in pixels
- `stride` - Frame stride in bytes (typically width × 2 for UYVY)

**Returns:** `true` if sent successfully, `false` otherwise

**Throws:** None (errors are logged)

**Usage:**
```kotlin
val uyvyData = convertToUyvy(yuvFrame)
val success = sender.sendFrame(
    data = uyvyData,
    width = 1280,
    height = 720,
    stride = 2560  // 1280 × 2
)
```

**Thread Safety:** Thread-safe (can be called from any thread)

**Performance:** Non-blocking (uses async API)

##### `isRunning(): Boolean`

Check if the sender is currently running.

**Returns:** `true` if running, `false` after `release()` is called

##### `getSourceName(): String`

Get the source name for this sender.

**Returns:** The source name provided at creation

##### `release()`

Release all NDI resources for this sender.

**Throws:** None (errors are logged)

**Usage:**
```kotlin
sender.release()
```

**Thread Safety:** Thread-safe (idempotent)

**Important:** Must be called when done with the sender to properly clean up native resources and stop the tally polling thread.

#### Properties

##### `tallyState: StateFlow<TallyState>`

Flow of tally state updates. Emits new values when the tally state changes.

**Type:** `StateFlow<TallyState>` (always has a value)

**Usage:**
```kotlin
// Collect in coroutine
sender.tallyState.collect { tallyState ->
    when {
        tallyState.isOnProgram -> showLiveIndicator()
        tallyState.isOnPreview -> showPreviewIndicator()
        else -> hideIndicator()
    }
}

// Or collect as state in Compose
val tallyState by sender.tallyState.collectAsState()
```

**Update Frequency:** Up to 10Hz (polled by native thread)

**Thread Safety:** Safe to collect from multiple coroutines

---

## Domain Models

### TallyState

Represents the current NDI tally state.

#### Properties

```kotlin
data class TallyState(
    val isOnPreview: Boolean = false,
    val isOnProgram: Boolean = false
) {
    val isTallyOn: Boolean  // true when on preview OR program
}
```

| Property | Type | Description |
|----------|------|-------------|
| `isOnPreview` | `Boolean` | true when in preview (e.g., OBS preview window) |
| `isOnProgram` | `Boolean` | true when live/on-air (e.g., OBS program output) |
| `isTallyOn` | `Boolean` | true when either preview or program is active |

#### Usage Examples

```kotlin
// Check if on-air
if (tallyState.isOnProgram) {
    // Show live indicator
}

// Check if any tally is active
if (tallyState.isTallyOn) {
    // Show some indicator
}

// Match specific states
when {
    tallyState.isOnProgram -> showGreenIndicator()
    tallyState.isOnPreview -> showYellowIndicator()
    else -> hideIndicator()
}
```

---

## Usage Examples

### Complete Lifecycle Example

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize NDI once at app startup
        NDIManager.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()

        // Cleanup NDI once at app shutdown
        NDIManager.cleanup()
    }
}
```

### Basic Streaming

```kotlin
class CameraStreamManager {
    private var sender: NDISender? = null

    fun startStreaming() {
        // Create sender
        sender = NDIManager.createSender("My Camera")

        // Observe tally state
        lifecycleScope.launch {
            sender?.tallyState?.collect { tallyState ->
                updateTallyUI(tallyState)
            }
        }

        // Send frames
        cameraFrameFlow.collect { frame ->
            val uyvy = convertToUyvy(frame)
            sender?.sendFrame(
                data = uyvy,
                width = frame.width,
                height = frame.height,
                stride = frame.width * 2
            )
        }
    }

    fun stopStreaming() {
        // Release sender
        sender?.release()
        sender = null
    }
}
```

### With Jetpack Compose

```kotlin
@Composable
fun StreamingScreen() {
    var sender by remember { mutableStateOf<NDISender?>(null) }
    val tallyState by sender?.tallyState?.collectAsState()
        ?: remember { mutableStateOf(TallyState()) }

    // Start streaming
    LaunchedEffect(Unit) {
        sender = NDIManager.createSender("Compose Camera")
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            sender?.release()
        }
    }

    // UI based on tally state
    Box {
        if (tallyState.isOnProgram) {
            LiveIndicator()
        }
    }
}
```

### Error Handling

```kotlin
fun startSafely(): Result<NDISender> {
    return try {
        if (!NDIManager.isInitialized()) {
            val initialized = NDIManager.initialize()
            if (!initialized) {
                return Result.failure(NDIException("Initialization failed"))
            }
        }

        val sender = NDIManager.createSender("Camera")
        Result.success(sender)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## Error Handling

### Initialization Failures

```kotlin
val initialized = NDIManager.initialize()
if (!initialized) {
    // Check logcat for specific error
    // Common causes:
    // - Missing libndi.so
    // - Incompatible ABI
    // - NDI SDK version mismatch
}
```

### Sender Creation Failures

```kotlin
try {
    val sender = NDIManager.createSender("Camera")
} catch (e: IllegalStateException) {
    // NDI not initialized
}
```

### Send Frame Failures

```kotlin
val success = sender.sendFrame(data, width, height, stride)
if (!success) {
    // Check logcat for specific error
    // Common causes:
    // - Sender released
    // - Invalid frame data
    // - Network issues
}
```

---

## Thread Safety

### Thread-Safe Operations

All public API methods are thread-safe:
- `NDIManager.initialize()` - Can be called from any thread
- `NDIManager.createSender()` - Can be called from any thread
- `NDIManager.cleanup()` - Can be called from any thread
- `NDISender.sendFrame()` - Can be called from any thread
- `NDISender.release()` - Can be called from any thread

### StateFlow Collection

`tallyState` is safe to collect from multiple coroutines:
```kotlin
// Multiple collectors are safe
scope1.launch { sender.tallyState.collect { /* ... */ } }
scope2.launch { sender.tallyState.collect { /* ... */ } }
```

### Native Threading

The native implementation uses a dedicated thread for tally polling:
- Polls at 10Hz
- Automatically attaches/detaches from JVM
- Thread-safe callback invocation

---

## Performance

### Send Frame Performance

- **Non-blocking:** Uses `send_send_video_async_v2()`
- **Zero-copy:** Data is passed directly to native code
- **No allocation:** ByteArray is reused by caller

### Tally Polling

- **Frequency:** 10Hz (100ms intervals)
- **Overhead:** Minimal (< 1ms per poll)
- **Thread:** Dedicated native thread

### Memory Usage

Approximate memory per sender:
- Native NDI instance: ~2MB
- JNI callback overhead: ~100KB
- Tally state object: Negligible

### Recommendations

1. **Reuse Senders** - Don't create/destroy frequently
2. **Limit Resolution** - 720p recommended for mobile
3. **Optimize Frame Rate** - 30 FPS for most use cases
4. **Monitor Tally** - Only collect tallyState when needed

---

## Advanced Usage

### Multiple Senders

```kotlin
// Create multiple senders
val sender1 = NDIManager.createSender("Camera 1")
val sender2 = NDIManager.createSender("Camera 2")

// Send frames independently
sender1.sendFrame(data1, w, h, stride)
sender2.sendFrame(data2, w, h, stride)

// Release separately
sender1.release()
sender2.release()
```

### Custom Frame Formats

While the module expects UYVY format, you can convert other formats:

```kotlin
// RGB to UYVY conversion
fun rgbToUyvy(rgb: ByteArray): ByteArray {
    // Your conversion logic
}

// Then send
val uyvy = rgbToUyvy(rgbFrame)
sender.sendFrame(uyvy, width, height, width * 2)
```

### Integration with Coroutines

```kotlin
class StreamingViewModel : ViewModel() {
    private val sender = NDIManager.createSender("VM Camera")

    fun startStreaming(frameFlow: Flow<Frame>) {
        viewModelScope.launch {
            frameFlow.collect { frame ->
                val uyvy = convertToUyvy(frame)
                sender.sendFrame(
                    uyvy,
                    frame.width,
                    frame.height,
                    frame.width * 2
                )
            }
        }
    }

    override fun onCleared() {
        sender.release()
        super.onCleared()
    }
}
```

---

## FAQ

### Q: Can I create multiple senders?
A: Yes, call `createSender()` multiple times.

### Q: Do I need to call `cleanup()`?
A: Only once at app shutdown, but recommended for proper cleanup.

### Q: What happens if I don't call `release()`?
A: Native resources leak. Always release when done.

### Q: Is the module compatible with NDI 5?
A: No, currently uses NDI v6.3 SDK.

### Q: Can I use this in a library project?
A: Yes, the module has no dependencies on the app module.

### Q: What's the video format requirement?
A: UYVY (2 bytes per pixel, 4:2:2 subsampling).

---

## Version History

See [CHANGELOG.md](../CHANGELOG.md) for version history.

---

## Support

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/yourusername/MyNdiCam).
