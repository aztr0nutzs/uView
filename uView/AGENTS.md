# Sentinel Agents Overview

This document enumerates the major *agents* and services within the Sentinel Home
application. Each agent encapsulates a distinct responsibility within the
system, whether it’s maintaining a video stream, sending notifications or
managing user credentials. Understanding these agents will help you navigate
the codebase, extend its functionality and debug issues efficiently.

## Core Agents

### Camera Monitor Service

* **Class:** `CameraMonitorService` (package `com.sentinel.app.core.service`)
* **Purpose:** Keeps camera streams alive and monitors motion while the app is in the
  background. Runs as a foreground service, acquires a wake‐lock and
  displays a low‑importance notification. When the app returns to the
  foreground, the service stops to avoid duplicating streams.
* **Key Interactions:**
  - Utilises `PlaybackManager` to maintain active streams.
  - Listens to `MotionMonitorService` (from Phase 5) to detect motion events.
  - Posts notifications via `NotificationHelper` when motion is detected.

### Notification Helper

* **Class:** `NotificationHelper` (package `com.sentinel.app.core.notifications`)
* **Purpose:** Centralises notification channel creation and notification
  construction. Provides functions to build persistent monitoring
  notifications and motion alert notifications with deep links back into
  the app.
* **Key Channels:**
  - `CHANNEL_ALERT`: High‑importance channel for motion and connection
    alerts.
  - `CHANNEL_MONITOR`: Low‑importance channel for the persistent
    background monitoring notification.

### Event Pipeline

* **Class:** `EventPipeline` (package `com.sentinel.app.data.events`)
* **Purpose:** Collects motion detection events and other camera events
  and relays them to repositories and the notification system.
* **Key Components:**
  - Subscribes to motion events from the motion detector (Phase 5).
  - Publishes events to `CameraEventRepository` for persistence.
  - Emits alerts via `NotificationEventRelay`.

### Recording Controller

* **Class:** `RecordingControllerImpl` (package
  `com.sentinel.app.data.recording`)
* **Purpose:** Handles local recording of camera streams. Starts and stops
  recordings, writes video using `MediaMuxer`/`MediaRecorder` and manages
  a `MutableStateFlow` of `ActiveRecording` sessions. Prunes old
  recordings when storage falls below a threshold.

### Snapshot Controller

* **Class:** `SnapshotControllerImpl` (package
  `com.sentinel.app.data.playback`)
* **Purpose:** Captures still frames from active camera streams and
  saves them as JPEG images. Supports both MJPEG and ExoPlayer sources.
* **Interactions:** Uses `MjpegSessionRegistry` for MJPEG frames and
  `SurfaceCapture` to capture ExoPlayer surfaces. Exposes `getSnapshots`
  to list saved snapshots for a camera.

### Config Manager

* **Class:** `ConfigManager` (package `com.sentinel.app.data.config`)
* **Purpose:** Exports all camera configurations to JSON and imports
  them from JSON. Supports sharing via Android `FileProvider` by
  writing to the app’s cache directory. Uses `kotlinx.serialization`
  for encoding and decoding.

### Preferences Data Source

* **Class:** `AppPreferencesDataSource` (package
  `com.sentinel.app.data.preferences`)
* **Purpose:** Wraps Android DataStore to expose strongly‑typed
  application preferences. Provides flows for each setting and
  functions to update them. Includes an `appLockEnabled` flag to
  control the app lock feature.

### App Lock Manager

* **Class:** `AppLockManager` (package `com.sentinel.app.core.security`)
* **Purpose:** Observes the `appLockEnabled` preference and handles
  prompting the user for authentication when the app is opened. The
  current implementation stubs out biometric/PIN authentication via
  `requireUnlock()`, which returns `true`. It exposes a `Flow<Boolean>`
  to indicate whether the lock is enabled.

### Crypto Manager

* **Class:** `CryptoManager` (package `com.sentinel.app.core.security`)
* **Purpose:** Encrypts and decrypts camera passwords. Uses AES/GCM with
  keys stored in the Android Keystore. Prepends a 12‑byte IV to the
  ciphertext and encodes the result in Base64 for storage. Decryption
  extracts the IV and performs the reverse operation.

### Crash Reporting

* **Class:** `CrashReportingTree` (package `com.sentinel.app.core.logging`)
* **Purpose:** A `Timber.Tree` that forwards warning and error logs to a
  crash reporting service. Currently a stub; in production it should be
  replaced with a proper Crashlytics implementation.

## Data Layer Agents

### Camera Repository

* **Class:** `CameraRepositoryImpl` (package
  `com.sentinel.app.data.repository`)
* **Purpose:** Provides high‑level CRUD operations for cameras. Talks to
  `CameraDao` for persistence, `StreamUrlResolver` for stream URLs and
  the various stream adapters to test connectivity.

### Camera Event Repository

* **Class:** `CameraEventRepositoryImpl` (package
  `com.sentinel.app.data.repository`)
* **Purpose:** Stores and retrieves camera events from the database via
  `CameraEventDao`. Supports marking events as read and pruning old
  events.

### Stream Adapters and Playback Services

These classes abstract the differences between RTSP, MJPEG and other stream
types. Key classes include `RtspCameraAdapterImpl`,
`MjpegStreamAdapterImpl`, `OnvifCameraAdapterImpl` and
`CameraPlaybackServiceImpl`. They work together to resolve stream URLs,
open ExoPlayer/MJPEG sessions and provide frames to the UI.

## User Interface Preservation Amendment

Sentinel Home is defined by its neon, tactical HUD‑style user interface. To
ensure continuity across future development:

* **Do not modify existing Compose screens** unless a build plan or
  feature requirement explicitly calls for it. Screens in
  `com.sentinel.app.features` and components in `com.sentinel.app.ui`
  are considered UI contracts.
* **Extend rather than replace**: when adding new UI elements, create
  new composables or build upon `SharedComponents.kt` instead of
  altering existing ones. Use the provided colour palette (`BackgroundDeep`,
  `TextPrimary`, `CyanPrimary`, `NeonOrange` etc.) and typography.
* **Maintain hierarchy and spacing**: keep header sizes, padding and
  margins consistent with existing designs.
* **Adaptive Icons**: the launcher icon has been replaced with an
  adaptive icon. Do not revert to the old placeholder.

Any future changes that could impact the visual identity of the
application must be reviewed against this amendment to avoid regressions.