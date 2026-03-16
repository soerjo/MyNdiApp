# NDI Setup Instructions

This app is ready for NDI streaming, but requires the NDI SDK for Android to be added manually.

## Step 1: Download NDI SDK

1. Visit: https://ndi.video/download/
2. Create a free NewTek account (required)
3. Download the NDI SDK for Android (it will be a .zip file)
4. Extract the downloaded zip file

## Step 2: Add NDI SDK to Project

1. Find the `libndi.so` native library file in the extracted NDI SDK
2. Create the `ndi/src/main/jniLibs/` directories if they don't exist
3. Copy `libndi.so` to both architectures:
   ```
   ndi/src/main/jniLibs/arm64-v8a/libndi.so
   ndi/src/main/jniLibs/armeabi-v7a/libndi.so
   ```

**Note:** The NDI module uses a JNI wrapper and is already set up. You only need to add the native library files.

## NDI Module Architecture

The NDI functionality is implemented in a standalone `ndi` module with the following structure:

### Kotlin Layer
- **NDIManager.kt** - Singleton for managing NDI library lifecycle
- **NDISender.kt** - Handles frame transmission and tally polling
- **NDIWrapper.kt** - JNI bindings to native C++ code
- **TallyState.kt** - Sealed class for tally states (OnAir, Preview, None)

### Native Layer
- **ndi_wrapper.cpp** - C++ wrapper integrating Processing.NDI v6.3 SDK
- **CMakeLists.txt** - CMake build configuration
- **Include/** - NDI SDK headers

### Build System
- Uses CMake 3.22.1 for native code compilation
- JNI pattern for Kotlin-C++ interop
- Supports both arm64-v8a and armeabi-v7a architectures

## Usage in App

The app uses the NDI module through the following components:

### Core Layer
- **NdiManager** (`app/core/manager/NdiManager.kt`) - Manages NDI lifecycle and streaming state
- Wraps the NDI library module for the application layer

### Presentation Layer
- **CameraViewModel** - Handles NDI streaming control
- **CameraScreen** - UI for starting/stopping streaming and displaying tally

## Step 3: Build and Run

1. Sync Gradle files (File > Sync Project with Gradle Files)
2. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install to connected device:
   ```bash
   ./gradlew installDebug
   ```
4. Grant camera and WiFi permissions when prompted
5. Select camera from menu (⋮)
6. Configure NDI settings (source name, frame rate)
7. Tap "Start Streaming" to begin NDI transmission

## Receiving NDI Stream on Laptop

To receive the NDI stream on your laptop:

### Option 1: NDI Studio Monitor (Free)
1. Download from: https://ndi.video/tools/
2. Install and run NDI Studio Monitor
3. Your Android camera should appear in the source list automatically
4. Both devices must be on the same network

### Option 2: OBS Studio with NDI Plugin
1. Install OBS Studio from https://obsproject.com/
2. Install the NDI plugin for OBS
3. Add NDI Source and select your Android camera

### Option 3: NDI Tools (Various)
- VLC with NDI plugin
- vMix
- Wirecast
- Any NDI-compatible software

## Network Requirements

- Both Android device and laptop must be on the same local network
- For best performance, use 5GHz WiFi or wired Ethernet
- Some networks may block multicast/broadcast traffic
- Firewall may need to allow NDI traffic (default port is 5961)
- Minimum bandwidth: ~15 Mbps for 720p60

## Troubleshooting

### NDI not appearing
- Make sure both devices are on the same network
- Check firewall settings on your laptop
- Try restarting the NDI sender (Stop/Start button)
- Verify `libndi.so` is correctly placed in both ABI directories

### App crashes on startup
- Make sure NDI SDK native library is in `ndi/src/main/jniLibs/`
- Check device ABI compatibility (arm64-v8a or armeabi-v7a)
- Review logcat for errors
- Verify Gradle sync was successful

### Build errors
- Ensure NDK 27.0.12077973 is installed
- Check CMake version (3.22.1 or higher)
- Verify native library file permissions
- Clean and rebuild: `./gradlew clean && ./gradlew assembleDebug`

### Poor video quality
- Check your WiFi signal strength
- Try using 5GHz WiFi instead of 2.4GHz
- Reduce frame rate to 30 FPS
- Close other apps using bandwidth
- Move closer to router

## NDI Features Implemented

- **Video Streaming** - Real-time UYVY format video transmission
- **Tally Support** - 10Hz polling for on-air/preview status
- **Flexible Configuration** - Custom source names, frame rates
- **Lifecycle Management** - Proper initialization and cleanup
- **StateFlow Integration** - Reactive state updates for UI

## Notes

- The NDI SDK is not available in public Maven repositories and must be added manually
- This code uses a wrapper class `NDISender` to encapsulate NDI functionality
- The app will work without NDI SDK (it just won't stream), so you can test camera functionality first
- NDI SDK version: Processing.NDI v6.3
- Native code compiled with C++17 standard

## Additional Resources

- [NDI Documentation](https://ndi.video/tools/)
- [NDI SDK Download](https://ndi.video/download/)
- [NDI SDK License](https://ndi.video/assets/downloads/sdk-license/NDI_SDK_license_agreement_2021-09-06.pdf)
