# AGENTS.md - Mobile App Development Guide (OnlyTechTake)

Guida operativa per agenti AI. Ambiente: **Windows 11 + PowerShell**.

---

## 1. Governance & Security
1. **User First**: La richiesta dell'utente ha priorità assoluta.
2. **Source of Truth**: `PROJECT_STATUS.json` contiene il backlog attivo. Consultarlo e aggiornarlo.
3. **Local-First**: Nessun cloud, nessuna telemetria, nessun WhatsApp automatico. I dati restano sul dispositivo.
4. **Security**: Database cifrato con SQLCipher. Non loggare dati sensibili.
5. **Privacy**: Tutte le stringhe in `strings.xml`. Evitare hardcoded UI copy.

---

## 2. Environment & Tools
- **Shell**: Usare PowerShell e i wrapper in `scripts\powershell\`.
- **Gradle**: Usare `scripts\powershell\android\gradle.ps1` per JDK/SDK.
- **Dependencies**: Usare esclusivamente `gradle/libs.versions.toml`. No versioni hardcoded.
- **Code Style**: Usare Spotless: `./gradlew spotlessApply` (tramite ps1) prima di chiudere il task.

---

## 3. Architecture & Patterns
- **Stack**: Kotlin, Compose, Material 3, Room + SQLCipher, DataStore, CameraX, Navigation Compose.
- **Layers**: UI (Compose) -> ViewModel (StateHolder) -> UseCase (opzionale) -> Repository -> Data Source.
- **Naming**: `*Screen.kt` (UI), `*ViewModel.kt` (Logica), `*Repository.kt` (Dati).
- **Previews**: Ogni Screen/Componente principale DEVE avere `@Preview` per Light e Dark mode.
- **UI Tests**: Usare il pattern **Robot**. I robot comuni sono definiti in `app/src/androidTest/.../demo/Robots.kt` e `BaseAssetRobot.kt`. Non duplicare logica di interazione UI.
- **Storage**: Directory private (`filesDir`). Import/Export tramite Storage Access Framework (SAF).

---

## 4. Platform & Build Rules
- **Flavors**:
  - `full` (Pro): Prodotto completo.
  - `demo` (Trial): 1 commessa, no export, watermark PDF.
  - `showcase`: Build interna con dati pre-caricati.
- **SDK**: Target 37, Min 26.
- **Database**: `DATABASE_VERSION = 1` bloccata. Non modificare schema o entità senza ordine esplicito.
- **Permissions**: Solo `CAMERA` (per documentazione riparazioni). `android:required="false"`.

---

## 5. Marketing & Assets
- **Location**: Tutti gli asset approvati e i testi store sono in `marketing/`.
- **Store Listing**: Testi Play-compliant (no claim iperbolici) in `marketing/copy/`.
- **Screenshots**: Usare `scripts\powershell\assets\create_demo_assets.ps1` per generare screenshot reali.

---

## 6. Workflow Operativo
1. **Analisi**: Controllare `PROJECT_STATUS.json` e `git status`.
2. **Esecuzione**: Modifiche atomiche. Non creare nuove guide lunghe.
3. **Verifica**:
   - `scripts\powershell\android\check_env.ps1`
   - `scripts\powershell\project\check_project.ps1 -Flavor Full -SkipReleaseBuild`
   - `scripts\powershell\project\quality.ps1` (Spotless + Lint)
4. **Cleanup**: Aggiornare `PROJECT_STATUS.json` e cancellare task completati.

---

## 7. Report Finale
Per ogni task riportare:
1. File modificati/eliminati/spostati.
2. Check eseguiti e risultati.
3. Stato aggiornato di `PROJECT_STATUS.json`.
4. Eventuali rischi residui.
