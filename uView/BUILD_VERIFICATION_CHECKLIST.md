# Build Verification Checklist

This checklist is mandatory for contributor handoff. Do **not** claim build readiness without command receipts.

## 1) Environment prerequisites (must be explicit)

Required before compile tasks:

- JDK 17+ (project currently runs with JDK 21 in local receipt below).
- Android SDK installed and reachable through **one** of:
  - `local.properties` with `sdk.dir=/absolute/path/to/Android/Sdk`
  - `ANDROID_HOME`
  - `ANDROID_SDK_ROOT`
- Android platform packages required by app module (compileSdk 35 stack).

If SDK is missing, mark build status as **environment-blocked**, not app-code-failed.

## 2) Required wrapper/package files

Run from `uView/`:

```bash
ls -l gradlew gradlew.bat \
      gradle/wrapper/gradle-wrapper.properties \
      gradle/wrapper/gradle-wrapper.jar \
      settings.gradle.kts build.gradle.kts \
      app/build.gradle.kts gradle/libs.versions.toml
```

Pass condition: all files present, readable, non-zero size.

## 3) Baseline Gradle integrity checks

Run from `uView/`:

```bash
bash ./gradlew --version
bash ./gradlew :app:tasks --all
```

Pass condition: wrapper starts and task graph is listed.

## 4) Build verification commands

Run from `uView/`:

```bash
bash ./gradlew :app:assembleDebug
bash ./gradlew :app:testDebugUnitTest
bash ./gradlew :app:lintDebug
```

If command fails, classify failure:

- **Environment failure**: missing SDK/JDK, unavailable emulator/device, missing `local.properties`.
- **App-code failure**: Kotlin/Java compile error, manifest merge/resource failure, test failure, lint regression.

## 5) Manifest/resource integrity checks

From repository root:

```bash
rg -n "android:icon|android:roundIcon|MainActivity|CameraMonitorService|BootReceiver|FileProvider" \
  uView/app/src/main/AndroidManifest.xml
```

Additionally confirm adaptive icon resource files exist:

- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`

## 6) Settings truthfulness checks (mandatory)

Manual validation on device/emulator:

- No clickable no-op rows in Settings.
- Any unwired settings row must be visibly unavailable/locked and non-clickable.
- Privacy & Permissions must not navigate to a placeholder destination.

## 7) Capability boundary checks (mandatory)

- Do not claim ExoPlayer/RTSP recording support unless fully implemented end-to-end.
- Current truthful boundary: MJPEG-backed recording available, ExoPlayer-backed recording unavailable.

## 8) Latest local verification receipt (2026-04-23 UTC)

Environment observed:

- OS: Linux
- JVM: 21.0.2
- Gradle wrapper: 8.6
- Android SDK: **not configured** in this container

Commands and outcomes:

```bash
bash ./gradlew --version
# PASS: Gradle 8.6, JVM 21.0.2

bash ./gradlew :app:tasks --all
# PASS: BUILD SUCCESSFUL, app tasks listed

bash ./gradlew :app:assembleDebug
# BLOCKED (environment): SDK location not found
# "Define a valid SDK location ... local.properties"
```

Conclusion:

- Wrapper + project wiring are verified.
- APK assembly was **not** proven in this environment due to missing Android SDK path.
- Do not mark this handoff as fully build-verified until `:app:assembleDebug` passes with SDK configured.
