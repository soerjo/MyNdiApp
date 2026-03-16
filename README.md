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
- **Multiple Camera Support** - Front, back, and external USB cameras (UVC devices)
- **Tally Indicators** - Visual feedback for on-air/preview status (green for live, yellow for preview)
- **Flexible Frame Rates** - 30 FPS (standard) or 60 FPS (smoother motion)
- **HD Resolution** - 1280x720 (720p) output
- **Custom Source Names** - Personalize your NDI source name
- **Settings Persistence** - Save and restore camera and NDI settings
- **Screen Modes** - Multiple display modes (Fit, Fill, Stretch)

### Technical Features
- **Modular Architecture** - NDI functionality in separate reusable module
- **Clean Architecture** - Separation of concerns with presentation/domain/data layers
- **StateFlow + ViewModel** - Reactive state management with lifecycle awareness
- **Hilt DI** - Type-safe dependency injection
- **Native Optimization** - UYVY format for efficient streaming (2 bytes/pixel vs 4 for BGRA)
- **JNI Bridge** - Native NDI SDK integration via Processing.NDI v6.3
- **USB Camera Support** - UVC (USB Video Class) camera support via native libraries
- **Multi-Format Conversion** - Convert between YUV_420_888, NV21, RGBA, and UYVY formats
- **Native Libraries** - libuvc for UVC devices, libnative for utilities
- **Material 3 Design** - Modern UI with Jetpack Compose

## Architecture

### Project Structure

```
MyNdiCam/
├── app/                           # Main application module (Clean Architecture)
│   ├── presentation/             # UI layer (Compose + ViewModels)
│   │   ├── screen/camera/
│   │   │   ├── CameraScreen.kt
│   │   │   ├── CameraViewModel.kt
│   │   │   └── components/
│   │   │       ├── camera/      # Camera preview components
│   │   │       ├── controls/    # Control buttons and menus
│   │   │       ├── dialogs/     # Settings dialogs
│   │   │       ├── overlay/     # Tally overlay
│   │   │       └── preview/     # Preview display
│   │   └── model/               # Presentation models
│   ├── domain/                   # Business logic layer
│   │   ├── model/               # Domain models
│   │   │   ├── CameraInfo.kt    # Sealed class (CameraX/USB)
│   │   │   ├── CameraType.kt    # Enum (FRONT/BACK/EXTERNAL)
│   │   │   ├── FrameRate.kt     # Enum (30/60 FPS)
│   │   │   ├── Resolution.kt    # Resolution model
│   │   │   ├── CropDimensions.kt
│   │   │   └── ScreenMode.kt
│   │   ├── repository/          # Repository interfaces
│   │   │   ├── CameraRepository.kt
│   │   │   └── SettingsRepository.kt
│   │   └── usecase/             # Use cases
│   │       ├── DetectCamerasUseCase.kt
│   │       ├── ObserveSettingsUseCase.kt
│   │       └── SaveSettingsUseCase.kt
│   ├── data/                     # Data layer
│   │   ├── camera/
│   │   │   ├── internal/       # CameraX implementation
│   │   │   │   ├── InternalCameraController.kt
│   │   │   │   └── CameraHelper.kt
│   │   │   ├── usb/            # USB camera implementation
│   │   │   │   └── UsbCameraController.kt
│   │   │   └── CameraState.kt
│   │   ├── datasource/         # Data sources
│   │   │   ├── CameraDataSource.kt
│   │   │   └── UsbCameraDataSource.kt
│   │   ├── repository/         # Repository implementations
│   │   │   ├── CameraRepositoryImpl.kt
│   │   │   └── SettingsRepositoryImpl.kt
│   │   └── model/               # Data models
│   ├── core/                     # Core utilities
│   │   ├── di/                  # Hilt modules
│   │   │   └── AppModule.kt
│   │   ├── manager/             # Business managers
│   │   │   ├── NdiManager.kt   # NDI streaming manager
│   │   │   └── UsbCameraManager.kt
│   │   ├── util/                # Utility functions
│   │   │   ├── conversion/     # Frame format converters
│   │   │   │   ├── FrameConverter.kt
│   │   │   │   ├── Yuv420Converter.kt
│   │   │   │   ├── RgbaConverter.kt
│   │   │   │   └── Nv21Converter.kt
│   │   │   ├── ImageCropExtensions.kt
│   │   │   ├── ImageFormatExtensions.kt
│   │   │   ├── MathExtensions.kt
│   │   │   └── UsbImageFormatExtensions.kt
│   │   └── common/              # Constants
│   │       └── Constants.kt
│   ├── MainActivity.kt
│   └── MyNdiApp.kt
│
├── ndi/                          # NDI library module (standalone)
│   ├── internal/                 # Internal implementation
│   │   └── NDIWrapper.kt        # JNI bindings
│   ├── model/                    # NDI domain models
│   │   └── TallyState.kt        # Sealed class (OnAir/Preview/None)
│   ├── NDIManager.kt            # Singleton NDI lifecycle manager
│   └── NDISender.kt             # NDI sender with frame sending
│
├── libausbc/                     # Android USB Camera library
│   ├── MultiCameraClient.kt     # USB camera client
│   ├── camera/                   # Camera management
│   ├── encode/                   # Video encoding
│   ├── render/                   # Surface rendering
│   ├── utils/                    # Utility classes
│   └── widget/                   # Custom widgets
│
├── libuvc/                       # UVC native library (JNI)
│   └── src/main/
│       ├── java/                 # Java/Kotlin bindings
│       ├── jni/                  # Native JNI code
│       │   └── libuvc/          # UVC implementation
│       └── cpp/                  # C++ code
│
└── libnative/                    # Native utilities library
    └── src/main/cpp/
        ├── nativelib.cpp         # Native utilities
        ├── module/               # Native modules
        ├── proxy/                # Proxy classes
        └── utils/                # Native utilities
```

### Technology Stack

| Layer | Technology |
|-------|------------|
| **UI** | Jetpack Compose, Material 3 |
| **Architecture** | Clean Architecture, MVVM |
| **DI** | Hilt |
| **Async** | Coroutines, StateFlow |
| **Camera (Internal)** | CameraX (1.3.4) |
| **Camera (USB)** | UVC via libausbc, libuvc |
| **NDI** | Processing.NDI v6.3 (JNI) |
| **Native** | C++17, CMake, NDK 27 |
| **Build** | Gradle 8.6 (Kotlin DSL) |

### Data Flow

**Internal Cameras (CameraX):**
```
Camera → CameraX → YUV_420_888 → Frame Conversion → UYVY → NDI Sender → Network
                                            ↓
                                      NDI Tally (10Hz)
                                            ↓
                                      StateFlow → UI
```

**USB Cameras (UVC):**
```
USB Camera → libausbc → UVC Frame → Frame Conversion → UYVY → NDI Sender → Network
                                            ↓
                                      NDI Tally (10Hz)
                                            ↓
                                      StateFlow → UI
```

**Frame Conversion Pipeline:**
- YUV_420_888 (CameraX) → RGBA → UYVY (NDI)
- NV21/MJPEG (USB) → RGBA → UYVY (NDI)
- Supports cropping and aspect ratio adjustment

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
3. Find `libndi.so` in the extracted SDK
4. Copy to:
   ```
   ndi/src/main/jniLibs/arm64-v8a/libndi.so
   ndi/src/main/jniLibs/armeabi-v7a/libndi.so
   ```

The NDI module uses:
- JNI wrapper (`NDIWrapper.kt`) for native bindings
- CMake build system for native code
- C++ wrapper (`ndi_wrapper.cpp`) integrating Processing.NDI v6.3

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
2. **Select camera** - Tap menu (⋮) → choose camera (Front, Back, or USB)
3. **Configure settings** - Set frame rate, source name, and screen mode
4. **Start streaming** - Tap the play button
5. **Connect receiver** - Open NDI-compatible software on same network

### USB Camera Support

The app supports UVC (USB Video Class) cameras:
- Connect USB camera to Android device
- Grant USB permission when prompted
- Select from camera menu as "EXTERNAL" camera
- Works with most webcams and USB capture devices

**Requirements:**
- Android device with USB OTG support
- USB OTG adapter/cable (if needed)
- UVC-compatible camera

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
| Screen Mode | Fit/Fill/Stretch | Menu (⋮) → Screen Mode |

### Tally Indicators

- **🟢 Green Blinking + Green Border** - On-air (live program)
- **🟡 Yellow Blinking** - In preview
- **No indicator** - Not connected

## Documentation

- [NDI Setup Guide](NDI_SETUP.md) - NDI SDK installation instructions
- [AGENTS.md](AGENTS.md) - Development guidelines for contributors
- [Architecture Guide](docs/ARCHITECTURE.md) - Detailed architecture documentation
- [NDI Module API](docs/API.md) - NDI library API reference
- [Development Guide](docs/DEVELOPMENT.md) - Contributing and development setup
- [Modularization Summary](MODULARIZATION_SUMMARY.md) - Module architecture details
- [Restructuring Summary](RESTRUCTURING_SUMMARY.md) - Clean architecture details

## Module Details

### app Module
Main application module implementing Clean Architecture:

- **presentation/**: UI layer with Composable screens and ViewModels
- **domain/**: Business logic with models, use cases, and repository interfaces
- **data/**: Data layer with repository implementations and data sources
- **core/**: Utilities, dependency injection, and managers

### ndi Module
Standalone NDI library module with JNI integration:

- JNI wrapper for Processing.NDI v6.3 SDK
- Singleton `NDIManager` for lifecycle management
- `NDISender` for frame transmission and tally polling
- CMake build system for native C++ code

### libausbc Module
Android USB Camera library for UVC devices:

- USB camera detection and management
- UVC (USB Video Class) protocol support
- Surface rendering and video encoding
- Multi-camera support

### libuvc Module
Native UVC library with JNI bindings:

- Native C++ UVC implementation
- USB device communication
- Frame capture from USB cameras
- NDK build integration

### libnative Module
Native utilities library:

- Common native utilities
- Proxy classes for native operations
- C++17 standard library support

## Network Requirements

- Both devices on **same local network**
- **5GHz WiFi** recommended for best performance
- Some routers may block multicast traffic
- Default NDI port: 5961
- Minimum bandwidth: ~15 Mbps for 720p60

## Permissions

| Permission | Purpose |
|------------|---------|
| `CAMERA` | Video capture (internal cameras) |
| `USB_HOST` | USB camera support |
| `ACCESS_WIFI_STATE` | Check connectivity |
| `INTERNET` | NDI streaming |
| `ACCESS_NETWORK_STATE` | Monitor network |
| `CHANGE_WIFI_MULTICAST_STATE` | NDI discovery |

**Note:** USB camera permission is requested at runtime when connecting a USB device.

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

- [x] Clean Architecture implementation (presentation/domain/data layers)
- [x] Modular design with NDI library and USB camera support modules
- [x] MVVM with ViewModel + StateFlow
- [x] Hilt dependency injection
- [x] NDI Tally support with 10Hz polling
- [x] Multiple camera support (CameraX internal + USB UVC external)
- [x] Material 3 UI with Jetpack Compose
- [x] Settings persistence
- [x] Multiple screen modes (Fit/Fill/Stretch)
- [x] Frame format conversion pipeline

### Roadmap

- [ ] Audio streaming support
- [ ] Adjustable resolution (1080p, 480p)
- [ ] NDI discovery UI
- [ ] Multiple concurrent streams
- [ ] NDI receiver functionality
- [ ] Unit tests
- [ ] Instrumentation tests
- [ ] Advanced camera controls (exposure, focus, white balance)
- [ ] Video codec selection (H.264, H.265)
- [ ] Network bandwidth optimization

## License

This project uses the NDI SDK which is subject to NewTek's license terms. Please refer to the [NDI SDK license agreement](https://ndi.video/assets/downloads/sdk-license/NDI_SDK_license_agreement_2021-09-06.pdf) for usage restrictions.

## Credits

- **NDI** by NewTek - Network Device Interface technology
- **CameraX** by Google - Modern camera API
- **Jetpack Compose** - Modern UI toolkit
- **Hilt** - Dependency injection for Android
- **libausbc** by jiangdg - Android USB Camera library
- **libuvc** - UVC (USB Video Class) library implementation

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
