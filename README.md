<p align="center"><img src="marketing/registered-assets/brand/social/github-cover.svg" width="100%" alt="OnlyTechTake"></p>

# OnlyTechTake

Android application for repair shops and technical assistance teams.

## Purpose

Manage service orders and goods intake, attach photos and videos, capture on-screen signatures and produce PDF documents. Data is stored locally and encrypted with SQLCipher; sharing and exports happen only when the user starts them. Full, Demo and Showcase are separate build flavors; the interface is in English and Italian.

## Requirements

- Windows with PowerShell and Android Studio for the supplied automation.
- JDK 21 for the configured toolchain (finding JDK 17 alone is not enough), Android SDK platform 37 and Build Tools. The app supports Android 8.0 (API 26) and newer.
- Release signing needs the owner's local, ignored signing configuration.

## First setup

```powershell
git clone https://github.com/dennidalpos/OnlyTechTake.git
Set-Location OnlyTechTake
./scripts/powershell/android/check_env.ps1 -NonInteractive
./scripts/powershell/project/check_project.ps1 -Flavor Full -SkipReleaseBuild -NonInteractive
```

The first command checks the JDK/SDK configuration; the second runs lint, compilation, unit tests and a debug APK build. Then open the project in Android Studio and pick the `full`, `demo` or `showcase` variant.

## Usage

The first launch creates a local administrator. Configure the shop, operators, templates and backup destination in the app, then register customers and orders. Use synthetic data for demos and tests, never real customer information. A captured signature is an image of the trace, not a qualified digital signature. Backup ZIP passwords protect database entries only, and restore is not guaranteed atomic: read [Backup flow](docs/BACKUP_FLOW.md).

## Build and test

```powershell
./scripts/powershell/project/quality.ps1 -Flavor Full -NonInteractive   # Spotless and Lint
./scripts/powershell/android/build_installer.ps1 -Flavor Demo -BuildType Debug -OutputType Apk -NonInteractive
```

Do not run cleanup or asset-regeneration scripts as part of ordinary verification. GitHub Actions are disabled: checks run locally. See the [release checklist](docs/play-store/release-checklist.md).

## Documentation and security

[Documentation index](docs/README.md) · [Contributing](CONTRIBUTING.md) · [Security policy](SECURITY.md) · [License](LICENSE) (proprietary).
