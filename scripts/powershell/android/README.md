# Android scripts

Run these PowerShell wrappers from the repository root. They detect the project from their own path and load the shared environment helper. Use explicit arguments and `-NonInteractive` for unattended verification.

| Script | Purpose | Main options |
|---|---|---|
| `check_env.ps1` | Inspect JDK, SDK, wrapper and adb availability; invokes the wrapper's version check when prerequisites are available | `-NonInteractive` |
| `build_installer.ps1` | Build an APK or AAB; optionally install an APK | `-Flavor Full|Demo|Showcase`, `-BuildType Debug|Release`, `-OutputType Apk|Aab`, `-Install`, `-NonInteractive` |
| `gradle.ps1` | Configure supported Android/JDK tools and forward arguments to Gradle | `-NonInteractive`, `-Stop`, Gradle arguments after `--` |
| `start_app.ps1` | Install and launch the selected package using adb | `-Flavor`, `-BuildType`, `-ApkPath`, `-NoInstall`, `-CleanInstall` |

```powershell
./scripts/powershell/android/check_env.ps1 -NonInteractive
./scripts/powershell/android/build_installer.ps1 -Flavor Demo -BuildType Debug -OutputType Apk -NonInteractive
./scripts/powershell/android/gradle.ps1 -NonInteractive -- :app:testFullDebugUnitTest
```

The configured application JVM toolchain is 21 and compile SDK is 37. The helper can detect JDK 17 or 21, but JDK 17 by itself does not meet the application's toolchain requirement. Set process `JAVA_HOME` to a verified JDK 21 if necessary. Check actual configured values in `app/build.gradle.kts`.

Builds write Gradle and module outputs. Managed verification uses an isolated runtime work area; environment variables alone do not redirect every tool output. Release builds require the owner's ignored `keystore.properties` and referenced keystore. Do not copy signing material into fixtures, logs or documentation.

Device installation and launch require a deliberately selected connected test device or emulator. `-CleanInstall` uninstalls an app and may destroy its local data. `-Clean` removes build outputs, and `-RerunTasks` repeats tasks; none is needed for ordinary documentation verification. See [project checks](../project/README.md) and the [release checklist](../../../docs/play-store/release-checklist.md).
