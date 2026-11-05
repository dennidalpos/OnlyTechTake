# Shared PowerShell helpers

These scripts are loaded by the Android, project and asset wrappers. They are libraries rather than standalone user commands.

## `android_env.ps1`

- `Set-AndroidCliEnvironment` detects supported JDK 17/21 and Android SDK installations and configures the current process environment.
- `Get-JavaMajorVersion` reads the Java major version.
- `Get-CandidateJavaHomes` searches candidate JDK locations.

The application's configured JVM toolchain is 21. Detecting a supported helper JDK does not prove that the application toolchain, SDK platform or dependencies are available. The wrappers should report environment failures before claiming build success.

## `android_demo.ps1`

- `Invoke-DemoScreenshotTest` runs an instrumented capture scenario.
- `Invoke-DemoApkBuildAndInstall` prepares and installs a demonstration build.
- `Resolve-DemoDeviceSerial` chooses the test device/emulator.
- `Set-DeviceGlobalSettingTemporary` records device-setting changes for restoration.

The device helpers can install applications and change locale/theme. Use a selected test device with synthetic data and review restoration results. They are not part of a read-only documentation check. See [asset scripts](../assets/README.md).
