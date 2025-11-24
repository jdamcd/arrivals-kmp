# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Arrivals is a Kotlin Multiplatform project for live transit times with 3 targets:
- **macOS**: Native SwiftUI status bar app (in `macOS/Arrivals.xcodeproj`)
- **Desktop**: Compose Multiplatform window for Linux/macOS/Windows (in `desktop/`)
- **CLI**: Command-line interface (in `cli/`)

All targets share common business logic in the `shared/` module.

Supported data sources:
- TfL API for London transit
- MTA GTFS feeds for NYC Subway
- Custom GTFS feeds for other transit systems

## Build & Development Commands

### Prerequisites

A TfL API key is required in `shared/secret.properties`:
```
tfl_app_key=YOURKEY
```

### Run Targets

**macOS app**: Open `macOS/Arrivals.xcodeproj` in Xcode and press Run

**Desktop app**:
```bash
./gradlew :desktop:run
./gradlew :desktop:run --args="-pi 1280 400"  # Fullscreen with dimensions
./gradlew :desktop:createDistributable        # Create native distribution
```

**CLI**:
```bash
./gradlew :cli:run --args="--help"
./gradlew :cli:run --args="tfl --station 910GSHRDHST --platform 'Platform 2'"
```

### Testing & Quality

```bash
./gradlew allTests          # Run all tests (JVM + macOS targets)
./gradlew jvmTest          # Run JVM tests only
./gradlew spotlessCheck    # Check Kotlin formatting
./gradlew spotlessApply    # Apply Kotlin formatting
./gradlew check           # Run all checks
./check                   # Convenience script: swiftformat + gradlew clean spotlessApply assemble allTests
```

Swift code formatting:
```bash
swiftformat .
```

## Architecture

### Module Structure

```
shared/          - Kotlin Multiplatform shared logic
  src/commonMain  - Shared code (Ktor, serialization, coroutines)
  src/jvmMain     - JVM-specific implementations (desktop & CLI)
  src/macosMain   - macOS-specific implementations (Swift interop)
  src/commonTest  - Shared tests
  src/jvmTest     - JVM-specific tests
cli/            - JVM CLI application (Clikt)
desktop/        - Compose Multiplatform desktop UI
macOS/          - SwiftUI macOS app (consumes ArrivalsLib framework)
```

### Shared Module (Kotlin Multiplatform)

The `shared/` module produces:
- JVM artifact for `cli` and `desktop` targets
- Native framework `ArrivalsLib` for macOS (arm64 + x64)

Key dependencies:
- Ktor (HTTP client, different engines per platform)
- Kotlinx Serialization (JSON)
- Wire (Protocol Buffers for GTFS-RT)
- Koin (dependency injection)

### Dependency Injection

The shared module uses Koin for DI. The main module is defined in `shared/src/commonMain/kotlin/com/jdamcd/arrivals/Arrivals.kt`:

- `initKoin()` - Initialize Koin (called from platform code)
- `MacDI` - Helper class for Swift to access dependencies
- `ArrivalsSwitcher` - Routes to TfL or GTFS based on `Settings.mode`

Core interfaces:
- `Arrivals` - Main entry point for fetching arrival data
- `TflSearch` - Search TfL stops
- `GtfsSearch` - Search GTFS stops

### Data Sources

**TfL**: `shared/src/commonMain/kotlin/com/jdamcd/arrivals/tfl/`
- `TflApi.kt` - HTTP client for TfL API
- `TflArrivals.kt` - Business logic for TfL arrivals + stop search

**GTFS**: `shared/src/commonMain/kotlin/com/jdamcd/arrivals/gtfs/`
- `GtfsApi.kt` - HTTP client for GTFS feeds
- `GtfsArrivals.kt` - Business logic for GTFS-RT arrivals
- `GtfsStops.kt` - GTFS schedule parsing for stop information
- `MtaSearch.kt` - MTA-specific stop search
- Protocol Buffer schemas in `shared/src/commonMain/proto/`

### Settings

`Settings` is an expect/actual class:
- Expect declaration: `shared/src/commonMain/kotlin/com/jdamcd/arrivals/Settings.kt`
- JVM actual: `shared/src/jvmMain/kotlin/com/jdamcd/arrivals/Settings.kt`
- macOS actual: `shared/src/macosMain/kotlin/com/jdamcd/arrivals/Settings.kt`

The macOS app uses SwiftUI for settings UI. The desktop app uses YAML configuration (`.arrivals.yml` in user home directory).

### Platform-Specific Code

**macOS** (`macOS/Arrivals/`):
- `ArrivalsApp.swift` - App entry point, menu bar setup
- `ArrivalsViewModel.swift` - SwiftUI ViewModel wrapping Kotlin code
- `ArrivalsView.swift` - Main SwiftUI view
- `Settings/` - Settings UI and UserDefaults persistence

**Desktop** (`desktop/src/main/kotlin/`):
- Compose UI with Material 3
- YAML config parser (SnakeYAML)
- Fullscreen mode for kiosk displays

**CLI** (`cli/src/main/kotlin/`):
- Clikt library for command-line parsing
- Subcommands for TfL and GTFS

### Secret Management

The TfL API key is managed via BuildKonfig:
- Key stored in `shared/secret.properties` (gitignored)
- Generated as `BuildConfig.TFL_APP_KEY` constant
- Config in `shared/build.gradle.kts` lines 60-76

## Key Implementation Details

### Protocol Buffers (Wire)

GTFS-RT uses Protocol Buffers. Wire plugin configuration in `shared/build.gradle.kts`:
- Proto files: `shared/src/commonMain/proto/`
- Generated code is in build directory
- Wire plugin generates Kotlin code for all platforms

### HTTP Client Platform Specifics

Ktor uses different engines:
- JVM: `ktor-client-jvm` (Apache HttpClient)
- macOS: `ktor-client-darwin` (NSURLSession)

Configured in `shared/src/commonMain/kotlin/com/jdamcd/arrivals/Arrivals.kt` with 10s timeout.

### Testing

- Common tests: Kotest assertions
- JVM tests: MockK for mocking, coroutines-test
- Test resources: `shared/src/jvmTest/resources/`

## Code Formatting

- Kotlin: Spotless with ktlint (version defined in `libs.versions.toml`)
- Swift: swiftformat
- Both enforced in CI (`.github/workflows/push.yml`)
