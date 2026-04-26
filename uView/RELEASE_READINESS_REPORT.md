# Release Readiness Report

Date: 2026-04-26

This report does not claim the app is ship-ready. It records what is proven,
what is partial, and what remains missing after the release-trim pass.

## Proven

- Debug Kotlin compilation passes locally.
- Debug APK assembly passes locally.
- Android lint passes locally.
- Debug unit-test task runs locally and completes with `NO-SOURCE`.
- Add Camera no longer exposes ONVIF, Alfred, or Demo source tiles.
- Settings no longer exposes unnecessary release-unavailable controls for stream quality, motion sensitivity, subnet scanning, event retention, or placeholder permissions navigation.
- Diagnostics navigation is still present, but labeled as TCP reachability only.
- Recording capability boundary remains MJPEG-only and excludes ExoPlayer-backed stream recording.
- App lock dependency wiring is present and the debug build compiles with AndroidX BiometricPrompt.

## Partial

- LAN discovery remains partial by design: it can provide ARP, mDNS, ONVIF identity, and port hints, then route users to manual setup.
- Unit-test infrastructure exists, but there are no debug unit-test sources in this receipt.
- Debug build validation is strong enough for handoff, but it does not replace signed release build validation.
- Runtime behavior was not validated on an emulator or physical device in this pass.
- Real camera compatibility was not smoke-tested against RTSP, MJPEG, HLS, IP Webcam, or DroidCam hardware/apps.

## Missing Before Ship Claim

- Signed release APK or AAB build receipt.
- Release signing configuration verification.
- Emulator or physical device install and launch smoke test.
- Runtime smoke tests for at least one RTSP source and one MJPEG source.
- Recording smoke test against an active MJPEG session.
- Background monitoring smoke test with Android notification permission behavior.
- App lock smoke test on a device with biometric or device credential configured.
- Final product/privacy review for permissions, storage behavior, and Play policy requirements.
