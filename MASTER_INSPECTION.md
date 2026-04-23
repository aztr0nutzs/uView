# Sentinel Home – Master Inspection Report

This report provides a high‑level inspection of the Sentinel Home project,
covering its architecture, implementation phases and recommendations. It
serves as an entry point for new contributors as well as a checklist for
reviewing the system’s integrity.

## Project Structure

The codebase is organised by responsibility rather than strict MVC:

- **`app/src/main/java/com/sentinel/app/core`** – Infrastructure services,
  dependency injection, logging, notifications, security and background
  services.
- **`app/src/main/java/com/sentinel/app/data`** – Persistence (Room
  entities/DAOs), remote stream adapters, configuration import/export,
  recording, playback and event pipelines. Subpackages group related
  functionality.
- **`app/src/main/java/com/sentinel/app/domain`** – Plain data models,
  repository and service interfaces used throughout the app.
- **`app/src/main/java/com/sentinel/app/features`** – Compose UI screens
  implementing user‑facing features such as the dashboard, camera list,
  camera detail, multi‑view grid, event feed, settings and snapshot
  gallery.
- **`app/src/main/java/com/sentinel/app/ui`** – Shared UI components,
  preview definitions and theme resources. These components define the
  look and feel of the entire application and must remain intact unless
  explicitly extended.

## Phased Development Summary

The Sentinel Home application was developed through a series of
incremental phases, each adding substantial functionality:

1. **Baseline** – Scaffolding of the Jetpack Compose UI with dashboard,
   camera list and camera detail screens. Introduced room database and
   DataStore preferences.
2. **Connectivity** – Added stream adapters for RTSP and MJPEG, along
   with playback using ExoPlayer. Implemented basic camera discovery via
   TCP probing.
3. **Motion Detection** – Integrated a frame difference algorithm to
   detect motion in live streams and write events to the database.
4. **Camera Discovery Enhancements** – Replaced linear port scanning
   with mDNS/Bonjour discovery, ONVIF WS‑Discovery and ARP table
   reading.
5. **Event Pipeline** – Added the `EventPipeline` and
   `NotificationEventRelay` to generate motion alerts and persist
   events. Implemented motion sensitivity settings.
6. **Persistent Monitoring** – Created `CameraMonitorService` to keep
   streams and motion detection alive when the app is backgrounded. Set
   up multiple notification channels and ensured wake‑lock management.
7. **Local Recording** – Added `RecordingControllerImpl` for local
   recordings, `RecordingSchedule` model scaffolding, a `StorageManager`
   to prune old recordings and a snapshot gallery screen to view saved
   images.
8. **Polish & Hardening** – Implemented real AES/GCM credential
   encryption (`CryptoManager`), added versioned database migrations,
   stubbed crash reporting (`CrashReportingTree`), integrated an app
   lock with `AppLockManager` and UI toggle, enabled configuration
   export/import via `ConfigManager` and introduced adaptive icons.

## Notable Implementations

### AES Credential Encryption

Passwords in camera connection profiles are now encrypted at rest using
AES/GCM. The encryption key is generated and stored in the Android
Keystore. Ciphertext is stored with a prepended 12‑byte IV, encoded in
Base64. This prevents casual extraction of credentials if the database
is compromised.

### Recording & Snapshots

The application can record local copies of streams and capture
snapshots. Recording support is currently **truthful and stream-type
dependent**: MJPEG-backed sessions can be written as multipart MJPEG
(`.mjpeg`) with sidecar metadata, while ExoPlayer-backed streams
(RTSP/HLS/etc.) are explicitly treated as unsupported until an encoded
sample pipeline is implemented. Snapshots are saved as JPEGs in the
external files directory and displayed in a dedicated gallery. A
`StorageManager` monitors space and prunes old files.

### Background Services

`CameraMonitorService` ensures that motion detection continues to run
even when the UI is not visible. It acquires a wake‑lock and posts a
persistent notification. When the main activity returns to the
foreground, the service is stopped to hand control back to the UI.

### Export & Import Configuration

`ConfigManager` serialises the entire list of cameras to JSON using
`kotlinx.serialization`. When exporting, the JSON file is saved to the
cache directory and can be shared via the system share sheet (enabled by
the `FileProvider` entry in the manifest). Importing clears existing
cameras and repopulates the database from the imported file.

## Recommendations & Future Work

- **App lock behavior:** Biometric/device-credential authentication is
  implemented via `BiometricPrompt`. Contributors should preserve the
  current truthful behavior (enablement failure when no credential is
  configured) and avoid reintroducing stubbed unlock paths.
- **Crash reporting:** Replace the placeholder `CrashReportingTree`
  implementation with Firebase Crashlytics or another crash
  aggregation service for real insights in production.
- **Recording schedule UI:** The `RecordingSchedule` model is scaffolded
  but not surfaced in the UI. Add schedule creation and management in
  the settings screen.
- **Automatic pruning controls:** Expose storage threshold controls for
  recordings and snapshots so users can adjust how aggressively the
  system prunes old files.
- **Security hardening:** Consider encrypting the database itself and
  implementing certificate pinning for remote streams.

## UI Preservation Amendment

The user interface is a critical part of Sentinel Home’s identity. Any
future development must adhere to the following rules:

1. **Do not alter existing colour schemes, typography or layout
   patterns.** All Compose screens under `com.sentinel.app.features` and
   shared components in `com.sentinel.app.ui` form the visual contract.
2. **Integrate new features by extension rather than modification.**
   When introducing new screens or elements, reuse existing
   `SectionCard`, `SettingsToggleRow`, `CameraTile` and related
   components wherever possible.
3. **Respect the neon HUD aesthetic.** Icons, progress bars and
   backgrounds use a specific palette (deep gray backgrounds with
   neon orange, cyan and green accents). New assets must match this
   style.
4. **Adaptive icon retention.** The adaptive launcher icon added in
   Phase 8 must remain in use. Do not reintroduce the placeholder icon.

Deviations from these guidelines should undergo a design review to
ensure a consistent user experience.

## Build Integrity Amendment

Contributor-ready archives must include the complete Android build system,
not just source files. Each inspection must verify:

1. The Gradle wrapper files exist at the Android project root:
   `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`,
   and `gradle/wrapper/gradle-wrapper.jar`.
2. `settings.gradle.kts`, root `build.gradle.kts`, `app/build.gradle.kts`,
   and `gradle/libs.versions.toml` are coherent and resolve the `:app`
   module without ad hoc reconstruction.
3. `app/src/main/AndroidManifest.xml` declares the application, launcher
   activity, foreground monitoring service, boot receiver, FileProvider,
   permissions, backup/data extraction XML, and adaptive launcher icons.
4. `BUILD_VERIFICATION_CHECKLIST.md` contains exact open, build, install,
   run, and clean verification commands.
5. No build repair may alter the authoritative HTML-derived HUD interface
   unless an explicit feature requirement authorizes UI changes.
6. Settings rows must be truthful: no future-only/no-op/deceptive
   controls may remain interactive.
7. Recording claims must be truthful per stream type. Do not claim
   end-to-end recording support for ExoPlayer-backed streams unless an
   actual encoded-sample pipeline exists and is wired.
