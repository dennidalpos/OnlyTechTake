<p align="center"><img src="marketing/registered-assets/brand/social/github-cover.svg" width="100%" alt="OnlyTechTake"></p>

# OnlyTechTake

Android application for repair shops and technical assistance teams: manage service orders, record goods intake, attach photos and videos, capture signatures on screen, and create PDF documents. Data is stored locally; sharing and exports are initiated by the user.

The interface supports English and Italian. Full, Demo and Showcase are separate build flavors. The repository's configured version is 1.4.0; local build preparation does not establish production readiness or Google Play acceptance.

## Features and limits

- Customers, service orders, operators and local audit history.
- CameraX photos and videos, signatures drawn on screen, and PDF reports.
- SQLCipher database encryption. Media, preferences and exported documents have separate protection limits; see [backup and restore](docs/BACKUP_FLOW.md).
- Explicit WhatsApp, Android sharing and document-picker exports. The selected app or provider may transfer files outside the device.
- Full supports backup/restore and XLSX export. Demo allows one stored order and up to three active operators, restricts exports and restore, and watermarks PDFs. Showcase provides synthetic demonstration data.

A captured signature is an image/trace used in the workflow; this README does not establish a legally qualified digital signature. Backup ZIP passwords currently protect database entries only. Restore is not guaranteed atomic, and moving a backup to a new installation does not transfer the installation's Android Keystore key.

## Requirements

- Windows with PowerShell and Android Studio for the supplied automation.
- JDK 21 for the configured JVM toolchain. The environment helper searches supported JDK 17/21 installations; finding JDK 17 alone does not satisfy the project's toolchain requirement.
- Android SDK platform 37 and Android Build Tools. The app targets API 37 and supports Android 8.0/API 26 and newer.
- The repository's Gradle wrapper. Release signing needs the owner's local, ignored signing configuration.

## Quick start

```powershell
git clone https://github.com/dennidalpos/OnlyTechTake.git
Set-Location OnlyTechTake
./scripts/powershell/android/check_env.ps1 -NonInteractive
./scripts/powershell/project/check_project.ps1 -Flavor Full -SkipReleaseBuild -NonInteractive
```

The environment check detects JDK/SDK configuration. The project check runs lint, Kotlin compilation, unit tests and a debug APK build for the selected flavor. It skips signing with `-SkipReleaseBuild`; it does not install or exercise the app on a device unless instrumented testing is explicitly selected. See the [Android scripts](scripts/powershell/android/README.md) and [project checks](scripts/powershell/project/README.md).

To build a debug APK without installing it:

```powershell
./scripts/powershell/android/build_installer.ps1 -Flavor Demo -BuildType Debug -OutputType Apk -NonInteractive
```

The wrappers write Gradle caches and module build outputs locally. Managed agents must run verification in their isolated runtime work area and preserve its outputs. Do not use cleanup or public-asset regeneration scripts as part of ordinary verification.

## Configuration and common tasks

Open the project in Android Studio and use the `full`, `demo` or `showcase` variant. First launch creates a local administrator; do not use real customer information for demonstrations or tests. Configure the shop, operators, templates and backup destination in the app. Backup encryption does not encrypt every ZIP entry.

```powershell
./scripts/powershell/project/quality.ps1 -Flavor Full -NonInteractive
```

The quality wrapper checks Spotless formatting and Android Lint. `-Apply` changes files and requires a separate intentional formatting step. Physical-device tests, Play upload, release signing and public asset distribution are separate operations; see the [release checklist](docs/play-store/release-checklist.md).

## Documentation and repository layout

- [Documentation index](docs/README.md): operations, release preparation and store guidance.
- [Backup flow](docs/BACKUP_FLOW.md): encryption, key and rollback limitations.
- `app/src/main/`: shared Kotlin/Compose application code and resources.
- `app/src/full/`, `app/src/demo/`, `app/src/showcase/`: flavor-specific behavior.
- `marketing/`: reviewed brand assets, screenshots, videos and copy.
- `landing/`: product page source; public links and claims need review before deployment.
- [Asset tools](scripts/powershell/assets/README.md): capture from the real app using synthetic data.

## Help, security and license

Use the repository's issue forms for reproducible bugs and requests. Report vulnerabilities through [SECURITY.md](SECURITY.md). GitHub Actions workflows are present but Actions are disabled; checks run locally.

The code is proprietary; see [LICENSE](LICENSE) and [CONTRIBUTING.md](CONTRIBUTING.md). The [Italian store listing](docs/play-store/STORE_LISTING_IT.md) and [English store listing](docs/play-store/STORE_LISTING_EN.md) are maintained separately from this developer guide.
