# VN Planner Mobile

Native Java Android client for VN Planner Desktop v0.1.

## Phase 1

The app currently provides:

- Touch-friendly, scrollable editor for all planner sections
- New Project
- Save and Save As through Android Storage Access Framework
- Open `.vnproj` through Android file pickers/file managers
- Import JSON
- Export JSON
- Offline operation with no account or server

Characters and chapters are represented in the shared model and are the next UI phase for dynamic editing.

## Shared format

A `.vnproj` file is a ZIP archive containing one UTF-8 entry:

```text
project.json
```

The JSON property names match the desktop Java model, including `whatChanges`, `characters`, `chapters`, and nested `choices`. Android uses the platform `org.json` API, so no JSON dependency is required.

## Build

Install Android Studio or the Android command-line SDK, then from this directory run:

```powershell
./gradlew assembleDebug
```

Install the generated APK with Android Studio or:

```powershell
adb install app/build/outputs/apk/debug/app-debug.apk
```

The project targets Android API 35 and supports Android API 23 or newer.
