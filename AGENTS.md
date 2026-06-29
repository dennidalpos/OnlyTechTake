# AGENTS.md - Mobile App Development Guide (OnlyTechTake)

Operational guide for AI agents. Environment: **Windows 11 + PowerShell**.
*Guida operativa per agenti AI. Ambiente: **Windows 11 + PowerShell**.*

---

## 1. Governance & Security
1. **User First**: The user's request has absolute priority. (*La richiesta dell'utente ha priorità assoluta.*)
2. **Source of Truth**: `PROJECT_STATUS.json` contains the active backlog. Consult and update it. (*`PROJECT_STATUS.json` contiene il backlog attivo. Consultarlo e aggiornarlo.*)
3. **Local-First**: No cloud, no telemetry, no automatic WhatsApp. Data stays on the device. (*Nessun cloud, nessuna telemetria, nessun WhatsApp automatico. I dati restano sul dispositivo.*)
4. **Security**: Database encrypted with SQLCipher. Do not log sensitive data. (*Database cifrato con SQLCipher. Non loggare dati sensibili.*)
5. **Privacy**: All strings in `strings.xml`. Avoid hardcoded UI copy. English is the default language (`values/strings.xml`), with Italian in `values-it/`. (*Tutte le stringhe in `strings.xml`. Evitare hardcoded UI copy. L'inglese è la lingua predefinita, con l'italiano in `values-it/`.*)

---

## 2. Environment & Tools
- **Shell**: Use PowerShell and the wrappers in `scripts\powershell\`. (*Usare PowerShell e i wrapper in `scripts\powershell\`.*)
- **Gradle**: Use `scripts\powershell\android\gradle.ps1` for JDK/SDK. (*Usare `scripts\powershell\android\gradle.ps1` per JDK/SDK.*)
- **Dependencies**: Exclusively use `gradle/libs.versions.toml`. No hardcoded versions. (*Usare esclusivamente `gradle/libs.versions.toml`. No versioni hardcoded.*)
- **Code Style**: Use Spotless: `./gradlew spotlessApply` (via ps1) before closing the task. (*Usare Spotless: `./gradlew spotlessApply` (tramite ps1) prima di chiudere il task.*)

---

## 3. Architecture & Patterns
- **Stack**: Kotlin, Compose, Material 3, AppCompat, Material Components (XML bridge), Room + SQLCipher, DataStore, CameraX, Navigation Compose.
- **Layers**: UI (Compose) → ViewModel (StateHolder) → UseCase (optional) → Repository → Data Source.
- **Naming**: `*Screen.kt` (UI), `*ViewModel.kt` (Logic), `*Repository.kt` (Data).
- **Previews**: Every main Screen/Component MUST have `@Preview` for Light and Dark mode. (*Ogni Screen/Componente principale DEVE avere `@Preview` per Light e Dark mode.*)
- **UI Tests**: Use the **Robot** pattern. Common robots are defined in `app/src/androidTest/.../demo/Robots.kt` and `BaseAssetRobot.kt`. Do not duplicate UI interaction logic. (*Usare il pattern **Robot**. I robot comuni sono definiti in `app/src/androidTest/.../demo/Robots.kt` e `BaseAssetRobot.kt`. Non duplicare logica di interazione UI.*)
- **Storage**: Private directories (`filesDir`). Import/Export via Storage Access Framework (SAF). (*Directory private (`filesDir`). Import/Export tramite Storage Access Framework (SAF).*)

---

## 4. Platform & Build Rules
- **Flavors**:
  - `full` (Pro): Complete product for end users. (*Prodotto completo per utenti finali.*)
  - `demo` (Trial): 1 order, no export, PDF watermark. Limited public version. (*1 commessa, no export, watermark PDF. Versione pubblica limitata.*)
  - `showcase`: Internal build for exclusive use of the owner/developer with preloaded data for quick demos. **DO NOT publish or mention on the landing page.** (*Build interna ad uso esclusivo del proprietario/sviluppatore con dati pre-caricati per demo rapide.*)
  - **Variant Alignment**: Every change to core logic or resources (Theme, DI, Repositories) MUST be applied to all flavors. Although development may start on `showcase`, always verify that `full` and `demo` are consistent. (*Ogni modifica alla logica core o alle risorse DEVE essere applicata a tutti i flavor. Anche se lo sviluppo può iniziare su `showcase`, verificare sempre che `full` e `demo` siano coerenti.*)
- **SDK**: Target 37, Min 26.
- **Database**: `DATABASE_VERSION = 1` locked. Do not modify schema or entities without explicit order. (*Non modificare schema o entità senza ordine esplicito.*)
- **Permissions**: `CAMERA` and `RECORD_AUDIO` (for technical repair documentation). `android:required="false"`. (*Solo `CAMERA` e `RECORD_AUDIO` per documentazione tecnica. `android:required="false"`.*)

---

## 5. Marketing & Assets
- **Location**: All approved assets and store texts are in `marketing/`. (*Tutti gli asset approvati e i testi store sono in `marketing/`.*)
- **Store Listing**: Play-compliant texts (no hyperbolic claims) in `marketing/copy/`. (*Testi Play-compliant in `marketing/copy/`.*)
- **Screenshots**: Use `scripts\powershell\assets\create_demo_assets.ps1` to generate real screenshots. (*Usare `scripts\powershell\assets\create_demo_assets.ps1` per generare screenshot reali.*)

---

## 6. Operational Workflow
1. **Analysis**: Check `PROJECT_STATUS.json` and `git status`. (*Controllare `PROJECT_STATUS.json` e `git status`.*)
2. **Execution**: Atomic changes. Do not create long new guides. (*Modifiche atomiche. Non creare nuove guide lunghe.*)
3. **Verification**:
   - `scripts\powershell\android\check_env.ps1`
   - `scripts\powershell\project\check_project.ps1 -Flavor Full -SkipReleaseBuild`
   - `scripts\powershell\project\quality.ps1` (Spotless + Lint)
4. **Cleanup**: Update `PROJECT_STATUS.json` and delete completed tasks. (*Aggiornare `PROJECT_STATUS.json` e cancellare task completati.*)

---

## 7. Final Report
For each task, report:
1. Modified/deleted/moved files. (*File modificati/eliminati/spostati.*)
2. Executed checks and results. (*Check eseguiti e risultati.*)
3. Updated status of `PROJECT_STATUS.json`. (*Stato aggiornato di `PROJECT_STATUS.json`.*)
4. Any residual risks. (*Eventuali rischi residui.*)
