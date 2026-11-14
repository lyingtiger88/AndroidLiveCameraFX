# AndroidLiveCameraFX

A native Android camera-processing prototype targeting Android 13 devices.

## Prototype 0.1.0
- Front-camera preview using CameraX
- ImageAnalysis frame pipeline
- Basic incoming frame-rate counter
- Runtime camera permission handling

This first build is a camera pipeline test only. It does **not** perform face replacement, create a system-wide virtual camera, or inject video into Google Meet / WhatsApp.

## Build
Open the project in Android Studio (JDK 17, Android SDK 35), or run:

```bash
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Device target
Initial test device: Xiaomi Redmi Note 11S, Android 13.
