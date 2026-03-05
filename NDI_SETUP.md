# NDI Setup Instructions

This app is ready for NDI streaming, but requires the NDI SDK for Android to be added manually.

## Step 1: Download NDI SDK

1. Visit: https://ndi.video/download/
2. Create a free NewTek account (required)
3. Download the NDI SDK for Android (it will be a .zip file)
4. Extract the downloaded zip file

## Step 2: Add NDI SDK to Project

1. Find the `.aar` file in the extracted NDI SDK (usually in `libs/` folder)
2. Create `app/libs` folder in your project if it doesn't exist
3. Copy the NDI `.aar` file to `app/libs/ndi-sdk-android.aar`

## Step 3: Uncomment NDI Code

Once the NDI SDK is added, uncomment the following in `MainActivity.kt`:

1. **Line 33-34** - Uncomment NDI imports:
   ```kotlin
   import com.newtek.ndi.*
   ```

2. **Line 56-62** - Uncomment NDI initialization in `NDISender` class

3. **Line 76-82** - Uncomment NDI frame sending in `sendFrame()` method

4. **Line 93-96** - Uncomment NDI cleanup in `release()` method

## Step 4: Build and Run

1. Sync Gradle files (File > Sync Project with Gradle Files)
2. Build and run the app on your Android device
3. Grant camera and WiFi permissions when prompted
4. Click "Start NDI" to begin streaming

## Receiving NDI Stream on Laptop

To receive the NDI stream on your laptop:

### Option 1: NDI Studio Monitor (Free)
1. Download from: https://ndi.video/tools/
2. Install and run NDI Studio Monitor
3. Your Android camera should appear in the source list
4. Both devices must be on the same network

### Option 2: OBS Studio with NDI Plugin
1. Install OBS Studio
2. Install the NDI plugin for OBS
3. Add NDI Source and select "Android Camera"

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

## Troubleshooting

**NDI not appearing:**
- Make sure both devices are on the same network
- Check firewall settings on your laptop
- Try restarting the NDI sender (Stop/Start button)

**App crashes:**
- Make sure NDI SDK .aar file is in `app/libs` folder
- Check that all NDI code is uncommented
- Verify Gradle sync was successful

**Poor video quality:**
- Check your WiFi signal strength
- Try using 5GHz WiFi instead of 2.4GHz
- Close other apps using bandwidth

## Notes

- The NDI SDK is not available in public Maven repositories and must be added manually
- This code uses a wrapper class `NDISender` to encapsulate NDI functionality
- The app will work without NDI SDK (it just won't stream), so you can test camera functionality first
