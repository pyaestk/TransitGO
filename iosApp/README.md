# TransitGO iOS

This is the SwiftUI host for the shared Kotlin/Compose Multiplatform application on the `compose-multiplatform` branch.

The [main README](../README.md) contains the complete setup guide for a fresh clone, including JDK/Android SDK requirements, Android Studio plugin compatibility, both iOS run options, simulator location setup, signing, and troubleshooting.

## Quick run

After completing the main README's setup, run this **from the repository root**:

```bash
open iosApp/TransitGO.xcodeproj
```

Select the **TransitGO** scheme, choose an installed **iPhone simulator**, and press **⌘R**. Xcode automatically builds the `Shared` framework and copies the shared resources.

The project configures ARM64 for iPhone hardware and Apple Silicon simulators. Debug builds use only the active architecture. Intel simulator builds are not configured.

## Nearby stations

In Simulator, choose **Features → Location → Custom Location** and enter latitude `13.7456`, longitude `100.5347`. In TransitGO, open the From/To picker, leave search empty and the line filter on **All**, and allow location access with **Precise Location** enabled.

## Development status

- Configured iOS deployment target: `15.0`.
- Locally checked build environment: Apple Silicon, Xcode `26.6`, iOS Simulator `26.5`.
- Shared screens: Planner, station picker, map, station details, Trips, and Settings.
- Physical-device behavior and App Store release preparation remain to be checked.
