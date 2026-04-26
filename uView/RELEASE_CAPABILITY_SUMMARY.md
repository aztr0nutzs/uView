# Release Capability Summary

This document defines what the current release surface does and does not support.
It is intentionally conservative. Do not broaden claims without matching
end-to-end implementation and verification receipts.

## Supported Source Setup

| Source | Release status | Boundary |
| --- | --- | --- |
| RTSP / RTSPS camera | Supported | Direct stream URL playback through Media3. Recording is excluded. |
| MJPEG / HTTP stream | Supported | Multipart MJPEG playback through the app MJPEG renderer. MJPEG recording is supported when a live MJPEG session is active. |
| HLS / m3u8 stream | Supported | Direct playlist playback through Media3. Recording is excluded. |
| IP Webcam Android app | Supported | Local LAN endpoint, typically `/video` on port 8080. |
| DroidCam Android app | Supported | Direct local HTTP or RTSP endpoint, typically port 4747. |
| Android custom URL | Advanced direct URL | Supported only when the phone app exposes a direct RTSP, MJPEG, HTTP, or HLS URL. |
| Generic stream URL | Advanced direct URL | Direct stream URL passthrough only. Cloud portals and app-only relay services are excluded. |

## Discovery And Diagnostics

| Area | Release status | Boundary |
| --- | --- | --- |
| LAN discovery | Partial | ARP, mDNS, ONVIF identity, and TCP port hints can suggest devices. Discovery does not complete ONVIF profile setup or prove a stream URL. |
| Discovery actions | Honest manual setup | Discovery cards may route to manual add-camera setup. They must not claim that credentials or decode readiness were validated. |
| Diagnostics screen | Supported TCP reachability | Diagnostics tests host and port reachability only. It does not validate credentials, decode streams, ONVIF profiles, or recording support. |

## Recording Paths

| Path | Release status | Boundary |
| --- | --- | --- |
| MJPEG-backed live sessions | Supported | Records active MJPEG frames to multipart Motion JPEG `.mjpeg` with a `.properties` sidecar. Files are app-managed. |
| RTSP / RTSPS recording | Excluded | Playback uses ExoPlayer/Media3, but encoded sample capture is not implemented. |
| HLS recording | Excluded | Playback uses ExoPlayer/Media3, but recording is not implemented. |
| Generic HTTP/URL recording | Excluded except MJPEG | Only MJPEG sessions can record. Other ExoPlayer-backed streams are rejected. |
| ONVIF / Alfred / Demo recording | Excluded | These are not selectable release setup sources. |

## Release-Trimmed UI Surface

Hidden or removed for release:

- ONVIF source setup tile.
- Alfred source setup tile.
- Demo source setup tile.
- Global default stream quality setting.
- Global motion sensitivity setting.
- Subnet scan setting.
- Event retention setting.
- Placeholder privacy/permissions navigation.
- Network scan row that implied more than the release diagnostics path supports.

Visible release settings:

- Dark theme.
- Data saver mode.
- Push notifications.
- Local-only mode.
- Recording save path as read-only app-managed state.
- App lock.
- Background monitoring.
- Auto reconnect.
- Diagnostics logging.
- TCP-only Diagnostics navigation.
- Local privacy statement.

## Security Boundary

The app lock surface uses AndroidX BiometricPrompt with device credential support.
There is no custom PIN system, no custom credential recovery flow, and no claimed
enterprise policy integration in this release surface.

## Unsupported Paths Explicitly Excluded

- ONVIF profile/stream setup.
- Alfred cloud relay ingest.
- Demo-camera setup for users.
- Automatic discovery-to-working-stream provisioning.
- Credential validation during discovery or diagnostics.
- Stream decode validation during diagnostics.
- ExoPlayer-backed recording for RTSP, HLS, or generic HTTP streams.
- Signed release artifact readiness without a release signing receipt.
