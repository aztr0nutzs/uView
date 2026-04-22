



Phase 4 — Real Camera Discovery

TCP port probing works now but is slow and blind. Replace it with proper discovery.
What to build:

mDNS/Bonjour scanner using android.net.nsd.NsdManager — finds cameras advertising _rtsp._tcp or _http._tcp
ONVIF WS-Discovery — UDP multicast to 239.255.255.250:3702, parse SOAP XML responses, then call GetStreamUri on each device

ARP table reader — faster than linear TCP scan, reads /proc/net/arp to get live LAN hosts, then probe only those
Subnet auto-detection from WifiManager.connectionInfo.ipAddress

Phase 5 — Motion Detection & Events Pipeline

Right now events are only manually seeded. This makes them real.

What to build:

MotionDetectorImpl.kt — frame diff algorithm on the decoded video bitmap stream; compare consecutive frames pixel-by-pixel against a sensitivity threshold


Plug into the ExoPlayer frame callback or use ImageAnalysis from CameraX if capturing from a local CameraEventRepository

Write detected events into CameraEventRepository automatically

Connect RecordingController to start a clip on motion trigger


Phase 6 — Persistent Background Monitoring

The app currently only monitors while it's open. This phase makes it a real home security tool.

What to build:
CameraMonitorService.kt — foreground service (manifest stub already exists, just commented out), keeps streams alive when app is backgrounded

Notification channel setup — IMPORTANCE_HIGH channel for motion alerts, IMPORTANCE_LOW for the persistent monitoring notification
NotificationHelper.kt — builds motion alert notifications with camera name, timestamp, and deep-link back to CameraDetailScreen

Wake lock management so the service survives Doze mode

Phase 7 — Local Recording

What to build:

RecordingControllerImpl.kt — uses MediaMuxer + video track from the ExoPlayer decode pipeline, or MediaRecorder pointed at the stream URL


Recording schedule model + UI (the placeholder rows in Settings are already there)


Storage manager — monitors available space, prunes oldest recordings when below threshold


Snapshot gallery screen — grid ofsaved SnapshotResult images per camera


Phase 8 — Polish & Production Hardening

These don't add features but make the app trustworthy.


Credential encryption — replace the passwordEncrypted stub with real EncryptedSharedPreferences or Android Keystore AES

Room migrations — replace fallbackToDestructiveMigration() with real versioned migrations before any production data matters

Error reporting — plant a Crashlytics Timber.Tree in SentinelApplication

App lock — wire the biometric/PIN placeholder in Settings using BiometricPrompt

Config export/import — serialize camera list to JSON via kotlinx.serialization, share via Android FileProvider

Adaptive icon — replace placeholder launcher icon with a real one