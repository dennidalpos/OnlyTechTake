# OnlyTechTake Repair App

<p align="center">
  <img src="marketing/registered-assets/brand/social/github-cover.svg" alt="OnlyTechTake Logo" width="400">
</p>

<p align="center">
  <a href="https://android.com"><img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg" alt="UI"></a>
  <img src="https://img.shields.io/badge/License-Proprietary-red.svg" alt="License">
</p>

---

## 📱 Overview

**OnlyTechTake** è la soluzione definitiva "local-first" per la gestione di laboratori di riparazione smartphone e PC. Progettata per la massima privacy e velocità, l'app gestisce l'intero ciclo di vita delle commesse senza mai inviare dati sensibili al cloud.

### ✨ Key Features
- 🛠️ **Gestione Commesse**: Monitoraggio in tempo reale (Aperta, Diagnosi, In Lavorazione, Pronta, Chiusa).
- 👤 **Anagrafica Clienti**: Database locale ottimizzato per ricerca rapida e storico riparazioni.
- ✍️ **Firma Digitale**: Acquisizione legale della firma per accettazione e privacy direttamente su schermo.
- 📸 **Documentazione Media**: Allegati fotografici dello stato del dispositivo integrati nella commessa.
- 📄 **Report PDF**: Generazione istantanea di schede di accettazione e ricevute.
- 🔐 **Privacy Totale**: Database cifrato con SQLCipher e archiviazione offline.

---

## 🖼️ Visual Showcase

### Screenshots (Pro Version)
> [!NOTE]
> Gli screenshot ufficiali devono essere spostati da `asset/` a `marketing/screenshots/` per essere visibili su GitHub.

<p align="center">
  <img src="marketing/screenshots/play-store/pro/01-dashboard-pro.png" width="200" alt="Dashboard">
  <img src="marketing/screenshots/play-store/pro/02-nuova-commessa-pro.png" width="200" alt="New Order">
  <img src="marketing/screenshots/play-store/pro/03-foto-firma-pro.png" width="200" alt="Signature">
  <img src="marketing/screenshots/play-store/pro/05-whatsapp-pdf-pro.png" width="200" alt="PDF Export">
</p>

### 🎥 Video Demo
[▶️ Guarda il Workflow Completo](marketing/videos/play-store/pro-workflow.mp4)

---

## 🌐 Landing Page & Presence

Il progetto include una landing page moderna e reattiva situata in `/landing`.
- **Tecnologie**: HTML5, CSS3 (Modern Material 3 Design), Lucide Icons.
- **Features**: Galleria interattiva, toggle Dark/Light mode, ottimizzazione per dispositivi mobili.
- **URL Locale**: Aprire `landing/index.html` nel browser.

---

## 📊 Project Metrics & Status

| Metrica | Valore |
|:---|:---|
| **Target SDK** | 37 (Android 15) |
| **Min SDK** | 26 (Android 8.0) |
| **Architecture** | Clean Architecture / MVI |
| **Database** | SQLCipher v4 (Encrypted) |
| **Build Flavors** | `full`, `demo`, `showcase` |
| **Status** | 🟢 Production Ready |

---

## 🛠️ Tech Stack
- **Linguaggio**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Local Storage**: [Room](https://developer.android.com/training/data-storage/room) + [SQLCipher](https://www.zetetic.net/sqlcipher/)
- **Asincronia**: Coroutines & Flow
- **Dependency Injection**: Manual DI (Container Pattern)
- **Media**: CameraX & ImagePicker

---

## 🚀 Sviluppo Locale

L'ambiente è ottimizzato per **Windows 11 + PowerShell**.

```powershell
# 1. Verifica l'ambiente di sviluppo
./scripts/powershell/android/check_env.ps1

# 2. Esegui i controlli di qualità (Lint + Tests)
./scripts/powershell/project/quality.ps1

# 3. Compila la versione Pro
./scripts/powershell/project/check_project.ps1 -Flavor Full -SkipReleaseBuild
```

Per le linee guida operative degli agenti AI, consulta [AGENTS.md](AGENTS.md).

---

<p align="center">
  OnlyTechTake © 2026 | <a href="https://github.com/dennidalpos">GitHub Repo</a>
</p>
