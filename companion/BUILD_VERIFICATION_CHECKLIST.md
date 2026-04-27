# Companion Build Verification Checklist

This checklist is for the standalone companion Android project. Run commands from
the `companion/` directory unless stated otherwise. Do not claim build readiness
without command receipts.

## Environment Prerequisites

Required:

- JDK 17 or newer.
- Android SDK installed with the compileSdk 35 platform.
- Network access for first-time Gradle dependency resolution.
- SDK path configured through one of:
  - `local.properties` containing `sdk.dir=<absolute path to Android SDK>`
  - `ANDROID_HOME`
  - `ANDROID_SDK_ROOT`

Local receipt environment on 2026-04-26:

- OS: Windows 11 10.0 amd64
- JVM: Eclipse Adoptium 21.0.10+7-LTS
- Gradle wrapper: 8.6
- Android Gradle Plugin: 8.4.0
- Kotlin plugin: 2.0.0
- compileSdk / targetSdk: 35

Known warning:

- Android Gradle Plugin 8.4.0 warns that it was tested up to compileSdk 34 while this project uses compileSdk 35. Debug assembly, lint, and the unit-test task pass locally despite this warning.

## Required Project Files

Verify these files exist and are non-empty:

```powershell
Test-Path .\gradlew
Test-Path .\gradlew.bat
Test-Path .\gradle\wrapper\gradle-wrapper.jar
Test-Path .\gradle\wrapper\gradle-wrapper.properties
Test-Path .\settings.gradle.kts
Test-Path .\build.gradle.kts
Test-Path .\gradle.properties
Test-Path .\gradle\libs.versions.toml
Test-Path .\app\build.gradle.kts
Test-Path .\app\src\main\AndroidManifest.xml
```

Pass condition: every command returns `True`.

## Manifest And Resource Integrity

Verify manifest references resolve:

```powershell
rg -n "CompanionApplication|MainActivity|StreamViewerActivity|ForegroundStreamService|@mipmap/ic_launcher|@mipmap/ic_launcher_round|Theme.SentinelCompanion|@string/app_name" .\app\src\main
```

Required resources:

- `app/src/main/res/values/strings.xml` defines `app_name`.
- `app/src/main/res/values/themes.xml` defines `Theme.SentinelCompanion`.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` exists.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` exists.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` exists.

## Verification Commands

Baseline wrapper:

```powershell
.\gradlew.bat --version
```

Build and checks:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Optional task graph check:

```powershell
.\gradlew.bat :app:tasks --all
```

## Pass / Fail Definitions

Pass:

- Wrapper starts from `companion/`.
- Debug resources link.
- Kotlin/Java compilation passes.
- Hilt/KSP generation passes.
- Debug APK assembly passes.
- `:app:testDebugUnitTest` completes. It may report `NO-SOURCE` until tests are added.
- `:app:lintDebug` completes successfully.

Environment blocker:

- Missing or incompatible JDK.
- Missing Android SDK path.
- Missing compileSdk 35 platform.
- Dependency resolution failure caused by unavailable network or repository access.

Code or packaging blocker:

- Missing wrapper files.
- Broken settings/build/catalog wiring.
- Manifest references missing classes or resources.
- Resource linking failures.
- Kotlin/Java compilation failures.
- Hilt/KSP generation failures.
- Lint failures.

## Latest Local Verification Receipt

Date: 2026-04-26

Commands and outcomes:

```powershell
.\gradlew.bat --version
# PASS: Gradle 8.6, JVM 21.0.10, Windows 11 amd64

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# FIRST RUN BLOCKED: environment blocker, companion/local.properties missing and no ANDROID_HOME or ANDROID_SDK_ROOT set.

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# SECOND RUN FAILED: code/packaging blocker, android.useAndroidX was missing.

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# THIRD RUN FAILED: code/packaging blocker, invalid framework theme parent resource.

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# FOURTH RUN FAILED: code blocker, missing ArrowBack icon import/reference.

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# PASS: debug APK assembled, testDebugUnitTest completed with NO-SOURCE, lintDebug passed.

$env:ANDROID_HOME='<absolute path to Android SDK>'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# PASS: security hardening build receipt. Debug APK assembled, testDebugUnitTest completed with NO-SOURCE, lintDebug passed.
```

Receipt limits:

- Signed release build was not run.
- Emulator or physical-device install was not run.
- Runtime smoke testing was not performed.
