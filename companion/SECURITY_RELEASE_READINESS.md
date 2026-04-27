# Companion Security And Release Readiness

Date: 2026-04-26

This receipt covers the standalone Sentinel Companion Android app.

## Sensitive Data Inventory

- `DeviceProfile.username`: stored in Room as cleartext because it is needed for display and connection setup and is lower sensitivity than the secret.
- `DeviceProfile.password`: stores passwords or bearer tokens for camera streams. Stored in Room as Android Keystore encrypted ciphertext with the `ENC1:` envelope.
- `Camera.username`: legacy camera table username. Stored in Room as cleartext for the same reason as `DeviceProfile.username`.
- `Camera.password`: legacy camera table password/token. Stored in Room as Android Keystore encrypted ciphertext with the `ENC1:` envelope.
- `ConnectionPrefs.hostAddress`, `port`, `useHttps`, `autoConnect`: stored in DataStore. These identify the companion host but are not credentials.
- `AppPrefs.biometricLock`: stored in DataStore. This is a preference flag, not an authentication secret.

The app does not currently store Sentinel account credentials, OAuth refresh tokens, signing keys, or push-notification tokens.

## Inspection Findings

- `devices.password` was previously a Room `TEXT` column written through `DeviceRepository` without encryption.
- `cameras.password` was also a Room `TEXT` column available through `CameraRepository` without encryption.
- The biometric/app-lock setting existed in preferences, but the settings UI still described it as unavailable and the stream viewer activity was not gated.
- Room used `fallbackToDestructiveMigration()`, which is not acceptable for production data preservation because schema changes can wipe locally stored camera records and credentials.
- `DeviceProfile.streamUrl()` constructs an in-memory RTSP/ONVIF URI containing credentials for playback. That URI is not persisted by the new credential protection path, but callers must not log it.

## Implemented Protections

- `CredentialCipher` uses an Android Keystore AES-256-GCM key under alias `sentinel_companion_credential_v1`.
- Ciphertext format is versioned as `ENC1:<base64(iv)>:<base64(ciphertext+tag)>`.
- Repository writes encrypt non-empty plaintext passwords before Room persistence.
- Repository reads decrypt encrypted passwords for runtime use; decryption failures return an empty in-memory password so the UI can require re-entry instead of crashing.
- Startup migration scans legacy plaintext rows in both `devices` and `cameras` and rewrites them through the cipher.
- The app does not silently fall back to plaintext if encryption fails.
- `BiometricGate` uses AndroidX `BiometricPrompt` with `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`.
- Main app content and full-screen stream viewer content are not composed until the gate unlocks.
- The gate relocks after the activity stops and prompts again on resume.
- The settings toggle is availability-aware and only enables app lock when biometric or device credential authentication is available, while still allowing a previously enabled lock to be disabled.
- Room now uses an explicit `1 -> 2` migration instead of destructive migration.

## Verification Receipt

Command run from `companion/`:

```powershell
$env:ANDROID_HOME='<absolute path to Android SDK>'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Result:

- PASS: `:app:assembleDebug`
- PASS: `:app:testDebugUnitTest` with `NO-SOURCE`
- PASS: `:app:lintDebug`

Known limits:

- Signed release build was not run.
- Emulator or physical-device biometric smoke testing was not run.
- The Android Gradle Plugin 8.4.0 still warns that it was tested up to compileSdk 34 while this project uses compileSdk 35.
