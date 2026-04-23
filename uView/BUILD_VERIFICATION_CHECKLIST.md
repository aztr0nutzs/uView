# Build Verification Checklist

Use this checklist after extracting the archive or before handing the project to another contributor.

## Open

1. Open Android Studio.
2. Select **File > Open**.
3. Choose the Android project directory: `uView`.
4. Wait for Gradle sync to complete.
5. Confirm Android Studio detects `settings.gradle.kts`, module `:app`, and the Gradle wrapper.

## Command-Line Build

From the Android project directory:

```powershell
cd uView
.\gradlew.bat --version
.\gradlew.bat tasks
.\gradlew.bat :app:assembleDebug
```

Expected result:

- Gradle downloads `gradle-8.6-bin.zip` through `gradle/wrapper/gradle-wrapper.properties`.
- The project resolves plugins from `gradle/libs.versions.toml`.
- `:app:assembleDebug` creates `app/build/outputs/apk/debug/app-debug.apk`.

## Install And Run

With an emulator or Android device connected:

```powershell
cd uView
.\gradlew.bat :app:installDebug
adb shell monkey -p com.sentinel.app.debug 1
```

Expected result:

- The debug package installs as `com.sentinel.app.debug`.
- The launcher uses the packaged adaptive icon.
- The app opens to the Sentinel Home tactical HUD interface.
- The dashboard, camera list, multi-view grid, and camera detail screens preserve the visual authority of `uview_screen1.html`, `uview_screen2.html`, and `uview_screen3.html`.

## Manifest And Packaging Checks

Before publishing or handing off:

1. Confirm `app/src/main/AndroidManifest.xml` declares `SentinelApplication`, `MainActivity`, `CameraMonitorService`, `BootReceiver`, and the non-exported `FileProvider`.
2. Confirm launcher icons exist at `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`.
3. Confirm `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`, and `file_paths.xml` are present.
4. Confirm the Gradle wrapper files are present: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, and `gradle/wrapper/gradle-wrapper.jar`.
5. Do not modify Compose UI screens while performing build-only repairs unless a feature requirement explicitly authorizes it.

## Clean Verification

Run this from the Android project directory before creating a release archive:

```powershell
.\gradlew.bat clean :app:assembleDebug
```

If this command fails, the archive is not contributor-ready.

## Latest Local Verification

Attempted on 2026-04-23:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
```

Observed result:

- `.\gradlew.bat --version` succeeded with Gradle 8.6 on JDK 21.
- `.\gradlew.bat :app:assembleDebug` did not reach Kotlin/Java compilation because this workstation has no Android SDK configured. Set `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or create `local.properties` with a valid `sdk.dir` before rerunning.
