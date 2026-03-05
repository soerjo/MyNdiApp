# MyFirstApp - NDI Camera Streamer

An Android application that streams live camera video over NDI (Network Device Interface) to any NDI-compatible receiver on the same network.

![Platform](https://img.shields.io/badge/platform-Android-blue)
![Language](https://img.shields.io/badge/language-Kotlin-orange)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-purple)
![Native](https://img.shields.io/badge/native-C%2B%2B%20JNI-yellow)
![Min SDK](https://img.shields.io/badge/min%20SDK-29-green)
![Target SDK](https://img.shields.io/badge/target%20SDK-36-green)

## Overview

This application turns your Android device into an NDI video source, allowing you to stream high-quality camera footage to computers running NDI-compatible software such as OBS Studio, vMix, Wirecast, VLC, or NDI Studio Monitor. It uses native NDI libraries through JNI bindings for optimal performance.

## Features

- **Live Camera Streaming** - Stream real-time video from your Android device's camera
- **Multiple Camera Support** - Switch between front, back, and external cameras
- **Frame Rate Selection** - Choose between 30 FPS (standard quality) and 60 FPS (smoother motion)
- **Customizable NDI Source Name** - Set a custom name for your NDI source
- **Modern UI** - Built with Jetpack Compose for a smooth, responsive experience
- **720p HD Output** - Streams at 1280x720 resolution in 16:9 aspect ratio
- **Landscape Orientation** - Optimized for landscape viewing
- **Settings Persistence** - Saves your preferences (source name, frame rate) between sessions

## Architecture

### Technology Stack

| Component | Technology |
|-----------|------------|
| **UI Framework** | Jetpack Compose with Material 3 |
| **Camera** | CameraX (camera-core, camera-camera2, camera-lifecycle, camera-view) |
| **Native Bridge** | JNI (C++) |
| **NDI Library** | Processing.NDI v6.3 (dynamic loading) |
| **Build System** | Gradle with Kotlin DSL |
| **Minimum SDK** | Android 10 (API 29) |
| **Target SDK** | Android 14 (API 36) |
| **Supported ABIs** | arm64-v8a, armeabi-v7a |

### Project Structure

```
app/
├── src/main/
│   ├── java/com/soerjo/myfirstapp/
│   │   ├── MainActivity.kt              # Main activity with camera & NDI orchestration
│   │   ├── NDIWrapper.kt                # Kotlin wrapper for NDI native functions
│   │   └── ui/theme/                    # Compose theme files
│   ├── cpp/
│   │   ├── Include/                     # NDI SDK headers
│   │   │   └── Processing.NDI.*.h      # NDI library include files
│   │   ├── CMakeLists.txt               # Native build configuration
│   │   └── ndi_wrapper.cpp              # JNI implementation for NDI
│   ├── jniLibs/
│   │   ├── arm64-v8a/libndi.so         # NDI native library (64-bit)
│   │   └── armeabi-v7a/libndi.so       # NDI native library (32-bit)
│   └── res/                             # Android resources
├── build.gradle.kts                     # App-level Gradle config
└── proguard-rules.pro                   # ProGuard configuration
```

### Key Components

#### MainActivity.kt
- Main entry point for the application
- Handles runtime permission requests (Camera, WiFi, Network)
- Manages camera lifecycle using CameraX
- Contains the `NDISender` class for frame transmission
- Implements Jetpack Compose UI with camera preview
- Handles user preferences via SharedPreferences

#### NDIWrapper.kt
- Kotlin singleton object providing JNI bindings
- Loads native `ndi_wrapper` library
- Provides safe API with stub mode fallback
- Methods: `initialize()`, `createSender()`, `sendFrame()`, `destroySender()`, `cleanup()`

#### ndi_wrapper.cpp
- Native C++ implementation using JNI
- Dynamically loads NDI library using `Processing.NDI.DynamicLoad.h`
- Implements all native methods declared in NDIWrapper
- Converts Java byte arrays to NDI video frames (BGRA format)
- Uses async API for better performance

## Permissions

The app requires the following permissions:

| Permission | Purpose |
|------------|---------|
| `CAMERA` | Access device camera for video capture |
| `ACCESS_WIFI_STATE` | Check WiFi connectivity status |
| `INTERNET` | Required for NDI network streaming |
| `ACCESS_NETWORK_STATE` | Monitor network connectivity |
| `CHANGE_WIFI_MULTICAST_STATE` | Enable NDI discovery on local network |

## Installation

### Prerequisites

1. Android Studio Hedgehog (2023.1.1) or later
2. Android SDK 36
3. NDK r21 or later (included with Android Studio)
4. CMake 3.18.1 or later

### Building from Source

1. Clone the repository:
```bash
git clone https://github.com/yourusername/MyFirstApp.git
cd MyFirstApp
```

2. Open the project in Android Studio

3. Sync Gradle files:
```
File > Sync Project with Gradle Files
```

4. Connect an Android device or start an emulator

5. Build and run:
```
Run > Run 'app'
```

## Usage

### Basic Usage

1. **Grant Permissions** - Allow camera and network permissions when prompted
2. **Select Camera** - Tap the menu button (⋮) to choose between front/back/external cameras
3. **Start Streaming** - Tap the play button to begin NDI streaming
4. **Stop Streaming** - Tap the stop button to end the stream

### Receiving NDI Stream

To receive the NDI stream on your computer:

#### Option 1: NDI Studio Monitor (Free)
1. Download from: https://ndi.video/tools/
2. Install and run NDI Studio Monitor
3. Your Android camera source should appear in the source list
4. Both devices must be on the same network

#### Option 2: OBS Studio with NDI Plugin
1. Install OBS Studio
2. Install the NDI plugin for OBS
3. Add NDI Source and select your Android camera

#### Option 3: Other NDI-Compatible Software
- VLC with NDI plugin
- vMix
- Wirecast
- Any software supporting NDI sources

### Settings

#### Change Frame Rate
1. Tap the menu button (⋮)
2. Select "Frame Rate"
3. Choose between 30 FPS (standard) or 60 FPS (smoother)

#### Change NDI Source Name
1. Tap the menu button (⋮)
2. Select "Source Name"
3. Enter your desired source name
4. Tap "Save" to apply

#### Select Camera
1. Tap the menu button (⋮)
2. The current camera is shown at the top
3. Tap to select a different camera (Front/Back/External)

## Network Requirements

- Both Android device and receiver must be on the **same local network**
- For best performance, use **5GHz WiFi** or wired Ethernet
- Some networks may block multicast/broadcast traffic
- Firewall may need to allow NDI traffic (default port: 5961)

## Troubleshooting

### NDI source not appearing on receiver

**Possible causes:**
- Devices are on different networks
- Firewall blocking NDI traffic
- Multicast disabled on router

**Solutions:**
- Ensure both devices are on the same WiFi network
- Disable firewall temporarily to test
- Try using a mobile hotspot on the Android device

### App crashes on startup

**Possible causes:**
- Missing NDI native libraries
- Incompatible device architecture

**Solutions:**
- Ensure `jniLibs` folder contains libndi.so for your device's ABI
- Check logcat for specific error messages

### Poor video quality or lag

**Possible causes:**
- Weak WiFi signal
- Network congestion
- High frame rate on slower device

**Solutions:**
- Move closer to WiFi router
- Switch to 5GHz WiFi
- Reduce frame rate to 30 FPS
- Close other apps using bandwidth

### Camera preview shows but streaming doesn't work

**Possible causes:**
- NDI library not loaded correctly
- Stub mode active (JNI methods not implemented)

**Solutions:**
- Check logcat for "stub mode" warnings
- Verify native library compilation succeeded
- Ensure NDI initialization completed successfully

## Configuration

### Build Variants

```gradle
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

### Native Build Configuration

```cmake
cmake_minimum_required(VERSION 3.18.1)
project("ndi_wrapper")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/Include)
```

## Development

### Adding Features

The project is structured to allow easy extension:

- **New frame rates**: Add to `FrameRate` enum in MainActivity.kt
- **Additional resolutions**: Modify `targetResolution` in CameraScreen composable
- **Custom video formats**: Update `yuvToBgra()` conversion function

### Code Style

The project follows Kotlin and Android coding conventions:
- Kotlin code style: https://kotlinlang.org/docs/coding-conventions.html
- Android code style: https://developer.android.com/kotlin/style-guide

## License

This project uses the NDI SDK which is subject to NewTek's license terms. Please refer to the NDI SDK license agreement for usage restrictions.

## Credits

- **NDI by NewTek** - Network Device Interface technology
- **CameraX by Google** - Modern camera API for Android
- **Jetpack Compose** - Modern UI toolkit for Android

## References

- [NDI Documentation](https://ndi.video/tools/)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [NDI SDK Download](https://ndi.video/download/)

---

**Note:** This application is provided as-is for educational and personal use. The NDI SDK requires a separate license agreement with NewTek for commercial use.
