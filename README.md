<p align="center">
  <img src="marketing/registered-assets/brand/social/github-cover.svg" width="100%" alt="OnlyTechTake Cover">
</p>

# OnlyTechTake

<p align="center">
  <img src="marketing/registered-assets/brand/identity/onlytechtake-logo.svg" alt="OnlyTechTake Logo" width="400">
</p>

<p align="center">
  <a href="https://android.com"><img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg" alt="UI"></a>
  <img src="https://img.shields.io/badge/License-Proprietary-red.svg" alt="License">
</p>

---

## 📱 Overview

**OnlyTechTake** is a powerful local-first solution for job management, goods intake, and technical documentation. Designed for maximum privacy and speed, the app handles the entire lifecycle of service orders, from initial intake to final delivery, without ever sending sensitive data to the cloud.

The app is ideal for technical assistance centers, repair labs, and any business that needs to document the intake of customer goods and track intervention activities.

The app uses **English** as the default fallback language and includes full localization in **Italian**.

### ✨ Key Features
- 🛠️ **Job Management**: Real-time tracking of service orders (Open, Diagnosis, In Progress, Ready, Closed) with expanded operator permissions.
- 👤 **Customer CRM**: Local database optimized for quick search and intervention history.
- ✍️ **Digital Signature**: Secure signature capture for terms, privacy, and intake acceptance directly on screen.
- 📸 **Technical Documentation**: Photo and video attachments of goods status integrated into the order for complete evidence.
- 📄 **PDF Reports**: Instant generation of intake forms, status receipts, and intervention reports.
- 🔄 **Smart Templates**: WhatsApp and legal templates that auto-switch language (English/Italian) unless customized by the user.
- 🔐 **Total Privacy**: SQLCipher encrypted database and offline storage.
- 🌐 **Multi-language**: Native support for English (Default) and Italian.

---

## 🖼️ Visual Showcase

### Screenshots (Pro Version)
<p align="center">
  <img src="marketing/screenshots/play-store/pro/01-dashboard-pro.png" width="200" alt="Dashboard">
  <img src="marketing/screenshots/play-store/pro/02-nuova-commessa-pro.png" width="200" alt="New Order">
  <img src="marketing/screenshots/play-store/pro/03-foto-firma-pro.png" width="200" alt="Signature">
  <img src="marketing/screenshots/play-store/pro/05-whatsapp-pdf-pro.png" width="200" alt="PDF Export">
</p>

### 🎥 Video Demo
[▶️ Watch the Full Workflow](marketing/videos/play-store/pro-workflow.mp4)

---

## 🌐 Landing Page & Presence

The project includes a modern and responsive landing page located in `/landing`.
- **Technologies**: HTML5, CSS3 (Modern Material 3 Design), Lucide Icons.
- **Features**: Interactive gallery, Dark/Light mode toggle, mobile optimization.
- **Local URL**: Open `landing/index.html` in your browser.

---

## 📊 Project Metrics & Status

| Metric | Value |
|:---|:---|
| **Target SDK** | 37 |
| **Min SDK** | 26 (Android 8.0) |
| **Architecture** | Clean Architecture / MVI |
| **Database** | SQLCipher v4 (Encrypted) |
| **Build Flavors** | `full`, `demo`, `showcase` |
| **Localization** | 🇺🇸 English (Default), 🇮🇹 Italian |
| **Status** | 🟢 Production Ready (v1.0.0) - Baseline Audit July 2026 |

---

## 🏗️ Project Structure
```text
OnlyTechTake/
├── app/                      # Main Android module
│   ├── src/
│   │   ├── main/             # Core logic and shared resources
│   │   ├── full/             # Pro version specific features
│   │   ├── demo/             # Trial version (1 order limit)
│   │   └── showcase/         # Internal demo build with preloaded data
│   └── build.gradle.kts
├── docs/                     # Play Store guides and technical docs
├── landing/                  # Product landing page (HTML/CSS)
├── marketing/                # Approved assets, screenshots, and copy
├── scripts/powershell/       # Dev automation (CI/CD, environment)
└── PROJECT_STATUS.json       # Active backlog and project health
```

---

## 🛠️ Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Local Storage**: [Room](https://developer.android.com/training/data-storage/room) + [SQLCipher](https://www.zetetic.net/sqlcipher/) (Encrypted at rest)
- **Asynchrony**: Coroutines & Flow
- **Dependency Injection**: Manual DI (Container Pattern for zero-overhead)
- **Media**: CameraX (Photo/Video evidence)
- **Navigation**: Jetpack Navigation Compose

---

## 🛡️ Privacy & Security Model
- **Local-First**: No data leaves the device. No cloud sync, no telemetry, no analytics.
- **Encrypted Storage**: The database is encrypted using SQLCipher.
- **Scoped Storage**: All media and documents are stored in private app directories.
- **Digital Trust**: Signature capture and photo/video attachments provide documented evidence for interventions, helping to reduce disputes.

---

## 🚀 Getting Started

### Environment Requirements
- **OS**: Windows 11 (Optimized for PowerShell)
- **IDE**: Android Studio Ladybug (or newer)
- **JDK**: Java 17 (Managed via Gradle)
- **Android SDK**: API 37

### Local Setup
1. **Clone the repository**:
   ```bash
   git clone https://github.com/dennidalpos/OnlyTechTake.git
   ```
2. **Environment Check**: Run the diagnostic script to ensure your SDK/JDK are correctly configured.
   ```powershell
   ./scripts/powershell/android/check_env.ps1
   ```
3. **Build & Sync**: Open in Android Studio and wait for Gradle sync.

### Build Flavors
| Flavor | Purpose | Constraints |
|:---|:---|:---|
| `full` | **Pro Production** | Unlimited orders, full export capabilities. |
| `demo` | **Public Trial** | Limited to 1 active order, watermarked PDFs. |
| `showcase`| **Internal Demo** | Preloaded with sample data for quick presentations. |

---

## 🧪 Quality & Automation

We use a suite of PowerShell wrappers to maintain code quality:

- **Quality Suite**: Runs Spotless (formatting) and Unit Tests.
  ```powershell
  ./scripts/powershell/project/quality.ps1
  ```
- **Full Project Check**: Compiles the specified flavor and runs static analysis.
  ```powershell
  ./scripts/powershell/project/check_project.ps1 -Flavor Full -SkipReleaseBuild
  ```
- **Asset Generation**: Automatically generates marketing screenshots from UI tests.
  ```powershell
  ./scripts/powershell/assets/create_demo_assets.ps1
  ```

For AI agent operational guidelines, see [AGENTS.md](AGENTS.md).

---

<p align="center">
  OnlyTechTake © 2026 | <a href="https://github.com/dennidalpos">GitHub Repo</a>
</p>
