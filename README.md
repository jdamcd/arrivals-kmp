# Arrivals

Arrivals is a Kotlin Multiplatform project for live transit times. It currently supports 3 targets: a macOS toolbar app, a CLI, and a desktop app (primarily intended for Raspberry Pi kiosk displays). 

![Screenshot: Arrivals app in the MacOS status bar](screenshot.png)

Supported data sources include:
- TfL API for London Underground, Overground, DLR, etc.
- Darwin API for UK National Rail
- MTA GTFS feeds for NYC Subway
- Custom GTFS feeds for other transit systems (many can be found [here](https://mobilitydatabase.org))

## Run

The macOS toolbar app can be downloaded from [releases](https://github.com/jdamcd/arrivals-kmp/releases) and moved to your Applications folder. The other targets need to be built from source with the instructions below.

## Build

### Prerequisites

1. Register to get API keys for the upstream data sources:
   - **TfL**: [Transport for London API](https://api-portal.tfl.gov.uk) app key
   - **UK National Rail**: [OpenLDBWS](https://realtime.nationalrail.co.uk/OpenLDBWSRegistration/Registration) access token
2. Create `shared/secret.properties` and add your keys:
   ```
   tfl_app_key=YOURKEY
   darwin_access_token=YOURTOKEN
   ```
3. Make sure you have a JDK configured at `$JAVA_HOME`

### Targets

| Target | Platform | Description | Quick start |
|--------|----------|-------------|------------|
| **macOS** | macOS | SwiftUI status bar app | Open `macOS/Arrivals.xcodeproj` in Xcode and click the Run button |
| **Desktop** | Linux (incl. Raspberry Pi), macOS, Windows | Compose Multiplatform window with fullscreen mode | `./gradlew :desktop:run` |
| **CLI** | JVM (all platforms) | Command-line interface | `./cli/install` |

---

### macOS toolbar app

Native status bar application for macOS, built with SwiftUI.

1. Open `macOS/Arrivals.xcodeproj` in Xcode
2. Press the Run button
3. Configure via the settings UI

---

### Desktop window

Cross-platform desktop UI, built with Compose Multiplatform. Includes a fullscreen mode for kiosk displays and configuration via a YAML file.

#### Run from Gradle

```bash
# Windowed mode
./gradlew :desktop:run

# Fullscreen with custom dimensions
./gradlew :desktop:run --args="-pi 1280 400"
```

#### Build Native Distribution

```bash
# Executable in desktop/build/compose/binaries/main/app/
./gradlew :desktop:createDistributable
```

#### Configuration via YAML

Create a `.arrivals.yml` in the user home directory to configure:

```yaml
# Mode: "tfl", "darwin", or "gtfs"
mode: tfl

# TfL fields
tfl_stop: 910GSHRDHST       # Station ID
tfl_platform: 2             # Optional platform number
tfl_direction: all          # "inbound", "outbound", or "all"

# Darwin (UK National Rail) fields
darwin_crs: PMR             # Station CRS code
darwin_platform: 2          # Optional platform number

# GTFS fields
gtfs_stop: G28S             # Station ID
gtfs_realtime: https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2Fgtfs-g
gtfs_schedule: https://transitfeeds.com/p/mta/79/latest/download
```

---

### CLI

Command-line interface (requires a JVM). Run `./cli/install` to install the `arrivals` command on macOS, or run it via Gradle with `./gradlew :cli:run`.

![Screenshot: arrivals CLI command](cli.png)

#### TfL example

```bash
arrivals tfl --station 910GSHRDHST --platform 2
```

#### Darwin (UK National Rail) example

```bash
arrivals darwin --station PMR --platform 2
```

#### GTFS example

```bash
arrivals gtfs --station G28S \
  --realtime https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2Fgtfs-g \
  --schedule https://transitfeeds.com/p/mta/79/latest/download
```

## Attribution

* Powered by [TfL Open Data](https://api.tfl.gov.uk)
  * OS data © Crown copyright and database rights 2016
  * Geomni UK Map data © and database rights 2019
* Powered by [Rail Data Marketplace](https://raildata.org.uk) via [Huxley2](https://github.com/jpsingleton/Huxley2)
* Uses this [London Underground Typeface](https://github.com/petykowski/London-Underground-Dot-Matrix-Typeface) for dot matrix text
