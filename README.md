# MyNdiCam - Android NDI Camera Streamer

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android-blue)
![Language](https://img.shields.io/badge/language-Kotlin-orange)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-purple)
![Native](https://img.shields.io/badge/native-C%2B%2B%20JNI-yellow)
![Architecture](https://img.shields.io/badge/architecture-Clean%20Architecture-brightgreen)
![DI](https://img.shields.io/badge/DI-Hilt-orange)
![Min SDK](https://img.shields.io/badge/min%20SDK-29-green)
![Target SDK](https://img.shields.io/badge/target%20SDK-36-green)

**A professional Android NDI camera streamer built with Clean Architecture and modular design**

[Features](#features) • [Architecture](#architecture) • [Installation](#installation) • [Usage](#usage) • [Documentation](#documentation)

</div>

---

## Overview

MyNdiCam is a modern Android application that transforms your device into a professional NDI (Network Device Interface) video source. Stream live camera footage to NDI-compatible receivers like OBS Studio, vMix, Wirecast, or NDI Studio Monitor over your local network.

Built with industry-standard practices including Clean Architecture, MVVM, Hilt dependency injection, and Jetpack Compose, this project serves as both a functional NDI streaming tool and a reference implementation for modern Android development.

## Features

### Core Features
- **Live Camera Streaming** - Real-time video streaming via NDI protocol
- **Multiple Camera Support** - Front, back, and external USB cameras
- **Tally Indicators** - Visual feedback for on-air/preview status (green for live, yellow for preview)
- **Flexible Frame Rates** - 30 FPS (standard) or 60 FPS (smoother motion)
- **HD Resolution** - 1280x720 (720p) output
- **Custom Source Names** - Personalize your NDI source name

### Technical Features
- **Modular Architecture** - NDI functionality in separate reusable module
- **Clean Architecture** - Separation of concerns with presentation/domain/data layers
- **StateFlow + ViewModel** - Reactive state management with lifecycle awareness
- **Hilt DI** - Type-safe dependency injection
- **Native Optimization** - UYVY format for efficient streaming (2 bytes/pixel vs 4 for BGRA)
- **JNI Bridge** - Native NDI SDK integration via Processing.NDI v6.3
- **Material 3 Design** - Modern UI with Jetpack Compose

## Architecture

### Project Structure

```
MyNdiCam/
├── app/                           # Main application module
│   ├── presentation/             # UI layer (Compose + ViewModels)
│   │   ├── MainActivity.kt
│   │   └── screen/camera/
│   │       ├── CameraScreen.kt
│   │       ├── CameraViewModel.kt
│   │       └── components/
│   ├── domain/                    # Business logic layer
│   │   ├── model/                # Domain models
│   │   ├── repository/           # Repository interfaces
│   │   └── usecase/              # Use cases
│   ├── data/                      # Data layer
│   │   ├── repository/           # Repository implementations
│   │   └── datasource/           # Data sources
│   └── core/                      # Core utilities
│       ├── di/                   # Hilt modules
│       ├── util/                 # Extension functions
│       └── common/               # Constants
│
└── ndi/                           # NDI library module (standalone)
    ├── model/                     # NDI domain models
    │   └── TallyState.kt
    ├── internal/                  # Internal implementation
    │   └── NDIWrapper.kt         # JNI bindings
    ├── NDIManager.kt             # Public API - lifecycle
    ├── NDISender.kt              # Public API - sender
    └── cpp/                       # Native C++ code
        ├── ndi_wrapper.cpp
        ├── Include/
        └── CMakeLists.txt
```

### Technology Stack

| Layer | Technology |
|-------|------------|
| **UI** | Jetpack Compose, Material 3 |
| **Architecture** | Clean Architecture, MVVM |
| **DI** | Hilt |
| **Async** | Coroutines, StateFlow |
| **Camera** | CameraX (1.3.4) |
| **NDI** | Processing.NDI v6.3 (JNI) |
| **Native** | C++17, CMake |
| **Build** | Gradle (Kotlin DSL) |

### Data Flow

```
Camera → CameraX → YUV_420_888 → UYVY Conversion → NDI Sender → Network
                                      ↓
                                  NDI Tally (10Hz polling)
                                      ↓
                                  StateFlow → UI Updates
```

## Installation

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 36
- NDK r21 or later
- CMake 3.18.1 or later
- Android device with:
  - Android 10 (API 29) or higher
  - Camera support
  - WiFi connectivity

### NDI SDK Setup

The NDI SDK is **not included** in the repository due to licensing. You must manually add it:

1. Download NDI SDK from [ndi.video](https://ndi.video/download/)
2. Extract the downloaded file
3. Copy `libndi.so` to:
   ```
   ndi/src/main/jniLibs/arm64-v8a/libndi.so
   ndi/src/main/jniLibs/armeabi-v7a/libndi.so
   ```

### Building

```bash
# Clone the repository
git clone https://github.com/yourusername/MyNdiCam.git
cd MyNdiCam

# Open in Android Studio and sync
# Build debug APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug
```

## Usage

### Quick Start

1. **Launch the app** - Grant camera and network permissions
2. **Select camera** - Tap menu (⋮) → choose camera
3. **Start streaming** - Tap the play button
4. **Connect receiver** - Open NDI-compatible software on same network

### Receiving the Stream

#### Using NDI Studio Monitor (Free)
1. Download from [ndi.video](https://ndi.video/tools/)
2. Install and run
3. Your camera source appears automatically

#### Using OBS Studio
1. Install [OBS Studio](https://obsproject.com/)
2. Install [NDI Plugin](https://github.com/Palakis/obs-ndi)
3. Add NDI Source → Select your camera

#### Other Options
- vMix
- Wirecast
- VLC with NDI plugin
- Any NDI-compatible software

### Settings

| Setting | Options | Location |
|---------|---------|----------|
| Camera | Front/Back/External | Menu (⋮) → Current Camera |
| Frame Rate | 30/60 FPS | Menu (⋮) → Frame Rate |
| Source Name | Custom text | Menu (⋮) → Source Name |

### Tally Indicators

- **🟢 Green Blinking + Green Border** - On-air (live program)
- **🟡 Yellow Blinking** - In preview
- **No indicator** - Not connected

## Documentation

- [Architecture Guide](docs/ARCHITECTURE.md) - Detailed architecture documentation
- [NDI Module API](docs/API.md) - NDI library API reference
- [Development Guide](docs/DEVELOPMENT.md) - Contributing and development setup
- [Modularization Summary](MODULARIZATION_SUMMARY.md) - Module architecture details
- [Restructuring Summary](RESTRUCTURING_SUMMARY.md) - Clean architecture details

## Network Requirements

- Both devices on **same local network**
- **5GHz WiFi** recommended for best performance
- Some routers may block multicast traffic
- Default NDI port: 5961
- Minimum bandwidth: ~15 Mbps for 720p60

## Permissions

| Permission | Purpose |
|------------|---------|
| `CAMERA` | Video capture |
| `ACCESS_WIFI_STATE` | Check connectivity |
| `INTERNET` | NDI streaming |
| `ACCESS_NETWORK_STATE` | Monitor network |
| `CHANGE_WIFI_MULTICAST_STATE` | NDI discovery |

## Troubleshooting

### Source not appearing on receiver

**Solutions:**
- Verify same network (check IP addresses)
- Disable firewall temporarily
- Try mobile hotspot on Android device
- Check router allows multicast traffic

### Poor quality or lag

**Solutions:**
- Use 5GHz WiFi
- Reduce frame rate to 30 FPS
- Move closer to router
- Close other bandwidth-heavy apps

### App crashes on startup

**Solutions:**
- Verify NDI SDK installed correctly
- Check device ABI compatibility
- Review logcat for errors

## Contributing

We welcome contributions! Please see [DEVELOPMENT.md](docs/DEVELOPMENT.md) for guidelines.

## Development

### Project Status

- [x] Clean Architecture implementation
- [x] Modular design with NDI library
- [x] MVVM with ViewModel + StateFlow
- [x] Hilt dependency injection
- [x] Tally support
- [x] Multiple camera support
- [x] Material 3 UI

### Roadmap

- [ ] Audio streaming support
- [ ] Adjustable resolution
- [ ] NDI discovery UI
- [ ] Multiple concurrent streams
- [ ] NDI receiver functionality
- [ ] Unit tests
- [ ] Instrumentation tests

## License

This project uses the NDI SDK which is subject to NewTek's license terms. Please refer to the [NDI SDK license agreement](https://ndi.video/assets/downloads/sdk-license/NDI_SDK_license_agreement_2021-09-06.pdf) for usage restrictions.

## Credits

- **NDI** by NewTek - Network Device Interface technology
- **CameraX** by Google - Modern camera API
- **Jetpack Compose** - Modern UI toolkit
- **Hilt** - Dependency injection for Android

## References

- [NDI Documentation](https://ndi.video/tools/)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [NDI SDK Download](https://ndi.video/download/)

---

<div align="center">

**Note:** This application is provided as-is for educational and personal use. Commercial use requires proper NDI SDK licensing from NewTek.

Made with ❤️ using Android and NDI

</div>
