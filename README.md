# OnlyTechTake Repair App

Gestionale "local-first" per laboratori di riparazione smartphone e PC.

## Overview
OnlyTechTake è un'applicazione Android progettata per professionisti tecnici. Consente la gestione completa del ciclo di vita delle riparazioni (commesse), anagrafica clienti, firma digitale e generazione report PDF, tutto mantenendo i dati esclusivamente sul dispositivo.

## Visual Showcase
### Screenshots (Pro Version)
<p align="center">
  <img src="reports/generated-assets/full/store-pro/play-store/vertical/screenshots/01-dashboard-pro.png" width="200" />
  <img src="reports/generated-assets/full/store-pro/play-store/vertical/screenshots/02-nuova-commessa-pro.png" width="200" />
  <img src="reports/generated-assets/full/store-pro/play-store/vertical/screenshots/03-foto-firma-pro.png" width="200" />
  <img src="reports/generated-assets/full/store-pro/play-store/vertical/screenshots/05-whatsapp-pdf-pro.png" width="200" />
</p>

### Video Demo
[Guarda la registrazione completa del workflow](reports/generated-assets/full/store-pro/device/raw/onlytechtake-full-store-pro.mp4)

## Project Metrics & Status
| Metrica | Valore |
|---------|--------|
| **Target SDK** | 37 (Android 15) |
| **Min SDK** | 26 (Android 8.0) |
| **Database** | Room + SQLCipher (v1) |
| **Code Style** | Spotless / ktlint |
| **Tests** | Instrumented (Passed), Unit (Passed) |
| **Local-First** | 100% Offline, No Cloud |

## Stack Tecnologico
- **Linguaggio**: Kotlin
- **UI**: Jetpack Compose con Material 3
- **Database**: Room + SQLCipher (cifratura locale)
- **Altro**: DataStore, CameraX, Navigation Compose, Coroutines.

## Policy Local-First
- **Privacy**: Nessun dato lascia il dispositivo senza azione esplicita dell'utente (backup/export).
- **Offline**: Funzionalità completa senza connessione internet.
- **Sicurezza**: Database cifrato.

## Varianti (Flavors)
- `full`: Versione Pro completa.
- `demo`: Versione Trial limitata (1 sola commessa attiva, export disabilitato).
- `showcase`: Build interna per dimostrazioni con dati pre-caricati.

## Sviluppo e Comandi
L'ambiente di sviluppo preferito è Windows 11 con PowerShell.

```powershell
# Verifica ambiente
./scripts/powershell/android/check_env.ps1

# Controllo qualità (Spotless, Lint, Tests)
./scripts/powershell/project/quality.ps1

# Build e controllo progetto
./scripts/powershell/project/check_project.ps1 -Flavor Full -SkipReleaseBuild
```

Per istruzioni operative dettagliate per agenti AI, consultare [AGENTS.md](AGENTS.md).
Per asset marketing e testi, consultare [marketing/](marketing/README.md).
