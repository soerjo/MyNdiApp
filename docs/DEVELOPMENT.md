# Development Guide

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Building](#building)
- [Testing](#testing)
- [Code Style](#code-style)
- [Git Workflow](#git-workflow)
- [Debugging](#debugging)
- [Performance Profiling](#performance-profiling)
- [Contributing](#contributing)

## Getting Started

### Prerequisites

**Required:**
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or later
- Android SDK 36
- NDK r21 or later
- CMake 3.18.1 or later
- Git

**Recommended:**
- A physical Android device (better camera performance than emulator)
- 5GHz WiFi network
- NDI Studio Monitor (for testing)

### Clone and Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/MyNdiCam.git
cd MyNdiCam

# Open in Android Studio
# .gitignore and gradle properties are pre-configured

# Setup NDI SDK (required - not in repo)
# Download from https://ndi.video/download/
# Copy libndi.so to ndi/src/main/jniLibs/<abi>/
```

## Development Setup

### IDE Configuration

**Android Studio Settings:**

1. **Code Style:**
   - Settings → Editor → Code Style → Kotlin
   - Import scheme from `codestyles/AndroidStyle.xml`

2. **Build Variants:**
   - Build Select "debug" variant for development

3. **Run Configuration:**
   - Edit "app" run configuration
   - Launch: "Default Activity"
   - Deploy: "Default"

### Gradle Properties

Check `gradle.properties` for recommended settings:

```properties
# Enable Gradle caching
org.gradle.caching=true

# Enable configuration cache
org.gradle.configuration-cache=true

# Enable parallel builds
org.gradle.parallel=true

# Increase heap size
org.gradle.jvmargs=-Xmx4096m
```

### NDI SDK Setup

The NDI SDK is **not included** due to licensing:

```bash
# After downloading NDI SDK
cd <ndi-sdk-extract>/lib/x86_64-linux-gnu/

# Copy to project (adjust paths as needed)
cp libndi.so.6 ../../../MyNdiCam/ndi/src/main/jniLibs/arm64-v8a/libndi.so
```

**Required files:**
```
ndi/src/main/jniLibs/
├── arm64-v8a/
│   └── libndi.so
└── armeabi-v7a/
    └── libndi.so
```

## Building

### Command Line

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install to device
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Android Studio

**Build > Make Project** or **Run > Run 'app'**

### Build Variants

| Variant | Minify | Signing | Use |
|---------|--------|---------|-----|
| debug | No | Debug | Development |
| release | No | Debug | Testing (no ProGuard) |

## Testing

### Unit Tests

```kotlin
// Example: ViewModel test
class CameraViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `toggleStreaming updates state`() = runTest {
        // Given
        val viewModel = CameraViewModel(...)

        // When
        viewModel.toggleStreaming()

        // Then
        assertEquals(true, viewModel.uiState.value.isStreaming)
    }
}
```

Run: `./gradlew test`

### Instrumentation Tests

```kotlin
// Example: UI test
@RunWith(AndroidJUnit4::class)
class CameraScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `startStreaming button shows when initialized`() {
        composeTestRule.setContent {
            CameraScreen()
        }

        composeTestRule
            .onNodeWithContentDescription("Start")
            .assertIsDisplayed()
    }
}
```

Run: `./gradlew connectedAndroidTest`

### Manual Testing

**Test Checklist:**

- [ ] App launches without crashes
- [ ] Permissions requested correctly
- [ ] Camera preview shows
- [ ] Can switch between cameras
- [ ] Can start/stop streaming
- [ ] NDI source appears in NDI Studio Monitor
- [ ] Tally indicators work (green/yellow)
- [ ] Settings persist after restart
- [ ] Can change source name
- [ ] Can change frame rate

### Test NDI Streaming

1. Start NDI Studio Monitor on PC
2. Ensure both devices on same network
3. Start streaming in app
4. Source should appear in NDI Studio Monitor
5. Add source to preview/program to test tally

## Code Style

### Kotlin Conventions

Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html):

**Naming:**
- Classes: `PascalCase` (e.g., `CameraViewModel`)
- Functions: `camelCase` (e.g., `toggleStreaming()`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_SOURCE_NAME`)
- Private properties: `camelCase` with underscore prefix if mutable (`_uiState`)

**Formatting:**
- 4 space indentation
- Maximum line length: 120 characters
- No trailing whitespace
- File ending: newline

### Documentation

**Public APIs:**
```kotlin
/**
 * Send a video frame via NDI
 *
 * @param data Frame data in UYVY format (2 bytes per pixel)
 * @param width Frame width in pixels
 * @param height Frame height in pixels
 * @param stride Frame stride in bytes (typically width × 2)
 * @return true if sent successfully, false otherwise
 */
fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int): Boolean
```

**Inline Comments:**
```kotlin
// Good: Explain WHY
// Use UYVY format for compatibility with NDI (2 bytes/pixel vs 4 for BGRA)

// Bad: Explain WHAT (code is self-documenting)
// Set stride to width times 2
```

### Clean Code Principles

**Functions:**
- Small, focused functions
- Maximum 3-4 parameters
- Return early for clarity

```kotlin
// Good
fun sendFrame(data: ByteArray, width: Int, height: Int, stride: Int): Boolean {
    if (!isRunning) return false
    if (data.isEmpty()) return false

    return NDIWrapper.sendFrame(data, width, height, stride)
}

// Avoid
fun sendFrameAndLogAndMaybeDoOtherThings(data: ByteArray, ...): Boolean {
    // Too many responsibilities
}
```

**Classes:**
- Single Responsibility Principle
- Prefer composition over inheritance
- Use data classes for immutable data

## Git Workflow

### Branch Strategy

```
main (protected)
  ↑
  develop
  ↑
  feature/tally-indicator
  bugfix/camera-crash
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add tally indicator support
fix: prevent crash when camera disconnects
docs: update API documentation
refactor: extract camera repository
test: add ViewModel unit tests
chore: upgrade dependencies
```

### Pull Request Process

1. Create feature branch from `develop`
2. Make changes with clear commit messages
3. Update documentation if needed
4. Ensure tests pass
5. Create PR with description:
   - What changed
   - Why it changed
   - How to test
   - Screenshots if UI changes
6. Request review
7. Address feedback
8. Merge to `develop`

### PR Template

```markdown
## Description
Brief description of changes

## Type
- [ ] Feature
- [ ] Bugfix
- [ ] Refactor
- [ ] Documentation
- [ ] Tests

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] All tests passing

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No merge conflicts
```

## Debugging

### Logcat Filtering

```bash
# Filter by package
adb logcat | grep "com.soerjo.myndicam"

# Filter by tag
adb logcat | grep "NDI_JNI"
adb logcat | grep "CameraViewModel"

# Filter multiple tags
adb logcat -s CameraViewModel CameraScreen NDISender
```

### Common Issues

**NDI not initializing:**
```
adb logcat | grep "NDI"
# Check for: "Failed to load NDI library"
# Verify libndi.so is in jniLibs
```

**Camera not binding:**
```
adb logcat | grep "CameraX"
# Check for: "Use case binding failed"
# Verify permissions granted
```

**JNI crashes:**
```
adb logcat | grep "FATAL"
# Check for: "JNI DETECTED ERROR"
# Verify native code compilation
```

### Native Debugging

```cpp
// Add logging in ndi_wrapper.cpp
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

// Use in code
LOGD("Frame sent: %dx%d", width, height);
```

### Breakpoints

**Kotlin:** Set breakpoints in Android Studio debugger

**Native C++:**
1. Run > Edit Configurations
2. Debugger: "Native"
3. Set breakpoints in ndi_wrapper.cpp
4. Debug with native symbols

## Performance Profiling

### Android Profiler

Use Android Studio Profiler to analyze:

**CPU:**
- Check for expensive operations
- Identify bottlenecks in frame processing

**Memory:**
- Monitor for memory leaks
- Check ByteArray allocations

**Network:**
- Monitor NDI bandwidth usage
- Check for dropped frames

### Systrace

```bash
# Capture trace
python systrace.py --time=10 -o trace.html sched freq idle am wm gfx view binder_driver camera

# Analyze frame drops
# Look for gaps in camera capture
```

### Custom Metrics

```kotlin
// Track frame rate
private var frameCount = 0
private var lastFpsUpdate = System.currentTimeMillis()

fun updateFps() {
    frameCount++
    val now = System.currentTimeMillis()
    if (now - lastFpsUpdate > 1000) {
        val fps = frameCount * 1000 / (now - lastFpsUpdate)
        Log.d("FPS", "Current: $fps")
        frameCount = 0
        lastFpsUpdate = now
    }
}
```

## Contributing

### First-Time Contributors

1. Read this development guide
2. Read [ARCHITECTURE.md](ARCHITECTURE.md)
3. Check existing issues
4. Create issue for your planned work
5. Request assignment
6. Follow Git workflow
7. Submit PR

### Areas for Contribution

**High Priority:**
- Unit tests for ViewModels
- Unit tests for use cases
- Instrumentation tests for UI
- Error handling improvements

**Medium Priority:**
- Additional camera resolutions
- Audio streaming support
- NDI discovery UI
- Settings migration

**Low Priority:**
- Theme customization
- Localization
- Accessibility improvements
- Performance optimizations

### Code Review Checklist

**Before Submitting PR:**
- [ ] Code compiles without warnings
- [ ] All tests pass
- [ ] New code has tests
- [ ] Documentation updated
- [ ] No merge conflicts
- [ ] Follows code style

**During Review:**
- [ ] Changes align with architecture
- [ ] No unnecessary complexity
- [ ] Error handling considered
- [ ] Performance implications understood
- [ ] Security implications reviewed

## Resources

### Documentation

- [Architecture Guide](ARCHITECTURE.md)
- [API Documentation](API.md)
- [NDI SDK Documentation](https://ndi.video/documentation/)
- [CameraX Guide](https://developer.android.com/training/camerax)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

### Tools

- [Android Studio](https://developer.android.com/studio)
- [NDI Tools](https://ndi.video/tools/)
- [ADB Shell Commands](https://developer.android.com/studio/command-line/adb)

### Libraries

- [Hilt](https://dagger.dev/hilt/)
- [CameraX](https://developer.android.com/training/camerax)
- [Compose](https://developer.android.com/jetpack/compose)
- [Coroutines](https://developer.android.com/kotlin/coroutines)

### Community

- [Android Developers](https://developer.android.com/)
- [Kotlin Slack](https://kotlinlang.slack.com/)
- [NDI Community](https://ndi.video/community/)

## Troubleshooting Development Issues

### Gradle Sync Failures

```bash
# Clean and retry
./gradlew clean --no-daemon

# Clear Gradle cache
rm -rf ~/.gradle/caches/

# Invalidate Android Studio caches
File > Invalidate Caches > Invalidate and Restart
```

### Native Build Failures

```bash
# Clean native build
./gradlew clean
rm -rf app/.cxx

# Check NDK version
ndk-build --version

# Verify CMake
cmake --version
```

### Lint Issues

```bash
# Run lint manually
./gradlew lint

# Generate report
./gradlew lintDebug
# Report: app/build/reports/lint-results.html
```

---

## Contact

For questions or issues:
- Create an issue on GitHub
- Check existing issues first
- Provide logs and device information

**Happy coding! 🚀**
