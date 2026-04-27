# Build Verification Checklist

This checklist is mandatory for contributor handoff. Do not claim build readiness
without command receipts.

## 1) Local Prerequisites

Run commands from `uView/`.

Required:

- Windows PowerShell or Command Prompt for `gradlew.bat`, or a Unix shell for `./gradlew`.
- JDK 17 or newer. Latest local receipt used Temurin JDK 21.0.10.
- Android SDK configured through one of:
  - `local.properties` with `sdk.dir=<absolute path to Android SDK>`
  - `ANDROID_HOME`
  - `ANDROID_SDK_ROOT`
- Android platform for compileSdk 35 installed.
- Network access available the first time Gradle resolves dependencies.

Local wrapper and build stack observed on 2026-04-26:

- OS: Windows 11 10.0 amd64
- JVM: Eclipse Adoptium 21.0.10+7-LTS
- Gradle wrapper: 8.6
- Android Gradle Plugin: 8.4.0
- Kotlin Gradle plugin: 1.9.22
- compileSdk / targetSdk: 35

Known warning:

- Android Gradle Plugin 8.4.0 warns that it was tested up to compileSdk 34 while this project uses compileSdk 35. Debug compile, assembly, lint, and the unit-test task pass locally despite that warning.

## 2) Required Wrapper And Package Files

Verify from `uView/`:

```powershell
Test-Path .\gradlew
Test-Path .\gradlew.bat
Test-Path .\gradle\wrapper\gradle-wrapper.properties
Test-Path .\gradle\wrapper\gradle-wrapper.jar
Test-Path .\settings.gradle.kts
Test-Path .\build.gradle.kts
Test-Path .\app\build.gradle.kts
Test-Path .\gradle\libs.versions.toml
```

Pass condition: every command returns `True` and files are non-empty.

## 3) Baseline Gradle Integrity Checks

Run from `uView/`:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:tasks --all
```

Pass condition: wrapper starts and the app task graph is listed.

## 4) Build Verification Commands

Run from `uView/`:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

If a command fails, classify it:

- Environment failure: missing SDK/JDK, unavailable emulator/device, invalid SDK path, or dependency resolution failure.
- App-code failure: Kotlin/Java compile error, manifest merge/resource failure, test failure, or lint regression.

## 5) Manifest And Resource Checks

From repository root:

```powershell
rg -n "android:icon|android:roundIcon|MainActivity|CameraMonitorService|BootReceiver|FileProvider" uView/app/src/main/AndroidManifest.xml
```

Adaptive icon resources that must remain present:

- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`

## 6) Settings Truthfulness Checks

Manual validation on device or emulator:

- No clickable no-op rows in Settings.
- Unwired settings are hidden or displayed as read-only state.
- Diagnostics is labeled as TCP reachability only.
- Privacy/About rows do not navigate to placeholder destinations.

## 7) Capability Boundary Checks

Mandatory release boundary:

- Do not claim ExoPlayer, RTSP, HLS, or generic HTTP recording support.
- Current recording support is MJPEG-backed sessions only.
- ONVIF profile setup, Alfred cloud relay ingest, and demo cameras are excluded from release setup.

See `RELEASE_CAPABILITY_SUMMARY.md` for the exact capability table.

## 8) Latest Local Verification Receipt

Date: 2026-04-26

Environment:

- OS: Windows 11 10.0 amd64
- JAVA_HOME: `C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\`
- JVM: Temurin 21.0.10+7-LTS
- Gradle wrapper: 8.6

Commands and outcomes:

```powershell
.\gradlew.bat --version
# PASS: Gradle 8.6, JVM 21.0.10, Windows 11 amd64

.\gradlew.bat :app:compileDebugKotlin
# PASS

.\gradlew.bat :app:assembleDebug
# PASS

.\gradlew.bat :app:testDebugUnitTest
# PASS: task completed with NO-SOURCE

.\gradlew.bat :app:lintDebug
# PASS

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
# PASS: debug APK assembly passed; unit-test task completed with NO-SOURCE
```

Receipt limits:

- Debug APK assembly is proven locally.
- Lint is proven locally.
- Unit test task execution is proven, but no debug unit-test sources exist.
- Signed release APK/AAB, emulator install, and physical camera smoke tests were not performed in this pass.
