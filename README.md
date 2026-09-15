# VN Planner Mobile v0.1

Native Java Android client for VN Planner Desktop v0.1. The app is an offline, touch-friendly Phase 1 implementation that uses the same project schema as the desktop application.

## Phase 1

Current implementation:

- Touch-friendly, scrollable editor for all planner sections
- New Project
- Save and Save As through Android Storage Access Framework
- Open `.vnproj` through Android file pickers/file managers
- Import JSON
- Export JSON
- Offline operation with no account or server
- File-manager handoff for supported `.vnproj` and JSON content URIs
- Project data remains available after the app closes when saved through the Android document provider

Known Phase 2 work:

- Dynamic add/edit/remove UI for Characters
- Dynamic add/edit/remove UI for Chapters
- Project list, project rename, and project delete workflows
- Safe autosave and recovery files
- Choices & Endings mind-map editor
- Share action and additional tablet polish

## Shared format

A `.vnproj` file is a ZIP archive containing one UTF-8 entry:

```text
project.json
```

The JSON property names match the desktop Java model, including `whatChanges`, `characters`, `chapters`, and nested `choices`. Android uses the platform `org.json` API, so no JSON dependency is required. The visible mobile Story section omits the legacy `What Changes` field, while the model still preserves it when reading and writing projects.

## Build

Install the Android command-line SDK, accept the required SDK licenses, and create `local.properties` with the local SDK path. For example:

```properties
sdk.dir=C:\\Android\\Sdk
```

From this directory, run with the Gradle installation or generated wrapper:

```powershell
gradle clean assembleDebug
```

If the wrapper has not been generated yet:

```powershell
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
```

Install the generated APK with Android Studio or:

```powershell
adb install app/build/outputs/apk/debug/app-debug.apk
```

The project targets Android API 35, uses Java 11 source compatibility, and supports Android API 23 or newer. A connected device or emulator is required for `adb install`.
