# BUILD_VERIFICATION_CHECKLIST.md

This checklist is required for contributor handoff. Do **not** claim build readiness without recording the command receipts below.

## 1) Environment prerequisites

- OS: Linux/macOS/Windows (WSL okay).
- JDK: 17 (`java -version` should report 17.x).
- Android SDK installed with:
  - Platform `android-35`
  - Build tools compatible with AGP 8.4.0
  - Platform tools (`adb`)
- `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) points to the SDK root.
- `local.properties` exists in `uView/` with:

```properties
sdk.dir=<absolute path to Android SDK>
```

## 2) Required project files (must exist)

Run from repository root (`/workspace/uView`):

```bash
ls -l uView/gradlew \
      uView/gradlew.bat \
      uView/gradle/wrapper/gradle-wrapper.jar \
      uView/gradle/wrapper/gradle-wrapper.properties \
      uView/settings.gradle.kts \
      uView/build.gradle.kts \
      uView/app/build.gradle.kts \
      uView/gradle/libs.versions.toml
```

**Pass:** all files listed with non-zero size.  
**Fail:** any file missing.

## 3) Wrapper and Gradle coherence

From `uView/`:

```bash
bash ./gradlew --version
bash ./gradlew :app:tasks --all
```

**Pass:** Gradle runs and prints task graph without dependency resolution failures.  
**Fail:** wrapper startup failure, plugin resolution errors, or missing SDK/JDK.

## 4) Build commands (record exact output)

From `uView/`:

```bash
bash ./gradlew :app:clean
bash ./gradlew :app:assembleDebug
bash ./gradlew :app:testDebugUnitTest
```

Optional strict checks:

```bash
bash ./gradlew :app:lintDebug
```

**Pass:** all required commands return exit code 0.  
**Fail:** any non-zero exit code.

## 5) Manifest/resource integrity checks

From repository root:

```bash
ls -l uView/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml \
      uView/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml \
      uView/app/src/main/res/drawable/ic_launcher_foreground.xml \
      uView/app/src/main/res/values/colors.xml

rg -n "android:icon|android:roundIcon|MainActivity|CameraMonitorService|BootReceiver|FileProvider" \
  uView/app/src/main/AndroidManifest.xml
```

**Pass:** adaptive icon resources exist and manifest references resolve.  
**Fail:** missing resources or manifest references to absent classes/resources.

## 6) Runtime handoff verification (manual)

Install and run:

```bash
bash ./gradlew :app:installDebug
adb shell am start -n com.sentinel.app.debug/com.sentinel.app.MainActivity
```

### 6.1 Navigation sanity
- Open Dashboard → Camera List → Camera Detail → Settings.
- Confirm no crash on route transitions.

### 6.2 Camera Detail tactical action strip
- Confirm tactical strip visual density/framing remains HUD-style (no flattened Material card conversion).
- Recording control must reflect true backend capability:
  - MJPEG + active stream: recording can start/stop.
  - ExoPlayer-backed streams: unavailable state must be explicit.

### 6.3 Settings integrity
- Verify no deceptive placeholder rows are interactive.
- Rows with unwired behavior must be clearly marked unavailable/locked with honest text.
- Working toggles remain functional (dark theme, app lock, background monitoring, notifications, etc.).

### 6.4 Launcher resources
- Verify launcher icon and round icon appear correctly in launcher.
- Ensure adaptive icon is used (no regression to placeholder icon).

## 7) HTML authority compliance

Before merging any UI-affecting change, visually compare against:
- `uview_screen1.html`
- `uview_screen2.html`
- `uview_screen3.html`

**Pass:** colorway, spacing density, framing/glow language, and hierarchy remain consistent.  
**Fail:** generic Material restyle, flattened layout, or loss of tactical HUD identity.

## 8) Reporting format for contributor handoff

Include in PR/handoff notes:
1. Environment used (OS, JDK, SDK versions).
2. Exact commands run.
3. Exit codes and key output lines.
4. What passed.
5. What could not be proven locally and why.

Do **not** claim "build verified" without command receipts.
