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
- Darwin API (Huxley2) for UK National Rail live departure boards
- BVG API for Berlin transit (S-Bahn, U-Bahn, Tram)

## Build & Development Commands

### Prerequisites

API keys are required in `shared/secret.properties` (gitignored):
```
tfl_key=YOURKEY
darwin_key=YOURTOKEN
org_511_key=YOURKEY
```

- TfL API key: Get from https://api.tfl.gov.uk
- Darwin access token: Register at https://raildata.org.uk and subscribe to Darwin data feeds (free tier)
- 511.org API key: Register at https://511.org/open-data (used for BART feeds)

Keys are generated as `BuildConfig` constants via BuildKonfig (configured in `shared/build.gradle.kts`).

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
```

### Testing & Quality

```bash
./gradlew allTests          # Run all tests (JVM + macOS targets)
./gradlew jvmTest          # Run JVM tests only
./gradlew spotlessCheck    # Check Kotlin formatting
./gradlew spotlessApply    # Apply Kotlin formatting
./check                   # Convenience script: swiftformat + gradlew clean spotlessApply assemble allTests
```

**Always run `./check` before committing** — it runs formatting, build, and all tests in one go. This is what CI checks, so if it passes locally, CI will pass.

Smoke test changes to the shared module via the CLI to catch runtime issues. Use search commands to find station IDs, then verify arrivals work end-to-end:
```bash
# Find station IDs
./gradlew :cli:run --args="search tfl shoreditch"
./gradlew :cli:run --args="search darwin clapham"
./gradlew :cli:run --args="search bvg alexanderplatz"
./gradlew :cli:run --args="list-stops --realtime <feed-url> --schedule <schedule-url>"

# Fetch arrivals
./gradlew :cli:run --args="tfl --station 910GSHRDHST --platform 2"
./gradlew :cli:run --args="darwin --station CLJ --platform 5"
./gradlew :cli:run --args="bvg --station 900013102 --line U8"
./gradlew :cli:run --args="gtfs --station A42N --realtime https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2Fgtfs-ace --schedule http://web.mta.info/developers/data/nyct/subway/google_transit.zip"
```

If API responses look suspicious, call the APIs directly (e.g. via `curl`) to rule out upstream issues.

### macOS UI Tests

XCUITest-based UI tests exercise the macOS app's key flows (popover display, settings configuration, live data). They hit live APIs.

```bash
# Run UI tests
xcodebuild test -project macOS/Arrivals.xcodeproj -scheme Arrivals \
  -destination 'platform=macOS' -only-testing:ArrivalsUITests

# Extract screenshots from the test results
./macOS/extract-screenshots.sh

# View screenshots to verify UI (saved as PNGs in /tmp/ui-test-screenshots/)
# Use the Read tool on any file, e.g. /tmp/ui-test-screenshots/1-popover-arrivals.png
```

After making changes to macOS UI code, run the UI tests and visually verify the screenshots to check for regressions. The tests capture screenshots at key points: popover with arrivals, settings with station selected, and popover after saving new settings.

## Architecture

### Dependency Injection

The shared module uses Koin for DI, defined in `Arrivals.kt`. Key patterns:
- `initKoin()` initializes Koin (called from platform code)
- `MacDI` is a helper class for Swift to access Koin dependencies
- `ArrivalsSwitcher` routes to the correct data source based on `Settings.mode`
- Darwin and BVG share the `StopSearch` interface with named Koin qualifiers
- `GtfsSearch` has named factories for MTA/BART (with pre-configured auth/schedule) and an unqualified factory that reads from Settings

### Settings

`Settings` is an expect/actual class. Settings fields are shared across all transit systems (only one system is active at a time). `clearStopConfig()` resets shared fields to empty defaults — called before saving new settings to prevent stale values from a previous transit system leaking through.

The macOS actual has cold start defaults (TfL Shoreditch High Street, Platform 2) for a working out-of-box experience. The JVM actual defaults to empty values since CLI/desktop require explicit configuration.

The desktop app uses YAML configuration (`.arrivals.yml` in user home directory).

### Key Implementation Details

- GTFS-RT uses Protocol Buffers via Wire (proto files in `shared/src/commonMain/proto/`, generated code in build directory)
- Ktor HTTP client uses different engines per platform: Apache on JVM, NSURLSession on macOS
- Default request timeout is 10s (60s for GTFS schedule downloads)
- Station ID formats vary by system: TfL uses NAPTAN codes (e.g. `910GSHRDHST`), Darwin uses 3-character CRS codes (e.g. `CLJ`), BVG uses numeric stop IDs (e.g. `900013102`), GTFS uses feed-specific stop IDs (e.g. `A42N` for MTA)
- BVG API filters to S-Bahn, U-Bahn, and Tram only (excludes bus/ferry/regional)
