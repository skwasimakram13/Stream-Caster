# Stream Caster

Stream Caster is an Android service that captures the device screen and streams it using WebRTC. This project leverages the Android `MediaProjection` API and WebRTC for real-time screen sharing.

## Features
- **Screen Capture**: Uses `MediaProjection` to capture the screen.
- **WebRTC Streaming**: Streams captured video using WebRTC.
- **Foreground Service**: Runs as a foreground service with a persistent notification.
- **STUN Support**: Uses Google's STUN server for connectivity.

## Prerequisites
- Android 8.0+ (API Level 26 and above)
- WebRTC library integrated in the project
- Permissions:
  - `FOREGROUND_SERVICE`
  - `INTERNET`
  - `SYSTEM_ALERT_WINDOW`
  - `RECORD_AUDIO`

## Setup & Installation
1. Clone the repository:
   ```sh
   git clone https://github.com/your-repo/StreamCaster.git
   cd StreamCaster
   ```
2. Open the project in Android Studio.
3. Add the required WebRTC dependency in `build.gradle`:
   ```gradle
   implementation 'org.webrtc:google-webrtc:1.0.32006'
   or
   implementation 'io.github.webrtc-sdk:android:125.6422.07'
   ```
4. Build and run the app on a real Android device.

## How to Use
1. Start the service using an intent:
   ```java
   Intent intent = new Intent(this, ScreenCaptureService.class);
   intent.putExtra("resultCode", resultCode);
   intent.putExtra("data", data);
   startService(intent);
   ```
2. The service runs in the foreground and starts screen capture.
3. WebRTC initializes the peer connection and begins streaming the screen.

## Stopping the Service
To stop screen sharing, call:
```java
stopService(new Intent(this, ScreenCaptureService.class));
```

## License
This project is licensed under the MIT License.

## Author
**Developed By** - [Sk Wasim Akram](https://github.com/skwasimakram13)
