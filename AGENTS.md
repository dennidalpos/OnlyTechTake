# AGENTS.md - Mobile App Development Guide (OnlyTechTake)

Istruzioni per agenti AI. Ambiente locale: **Windows 11 + PowerShell**.

---

## 1. Scope & Governance
1. La richiesta utente ha sempre la precedenza.
2. `PROJECT_STATUS.json` è la fonte di verità per il backlog attivo; consultarlo e aggiornarlo ad ogni sessione.
3. In caso di conflitto tra istruzioni, scegliere la modifica sicura meno invasiva e segnalarlo.
4. Trattare il progetto come **fresh**: evitare debito legacy, shim o workaround storici.

---

## 2. Environment & Tooling
- **Shell**: Usare PowerShell e i wrapper in `scripts\powershell\`.
- **Gradle**: Usare `scripts\powershell\android\gradle.ps1` per garantire il corretto caricamento di JDK/SDK.
- **Code Style**: Il progetto usa **Spotless**. Eseguire `./gradlew spotlessApply` (tramite wrapper ps1) prima di concludere il task.
- **Dependencies**: Usare esclusivamente `gradle/libs.versions.toml`. Non inserire versioni hardcoded nei file `build.gradle.kts`.

---

## 3. Architecture & Patterns
- **Stack**: Kotlin, Compose, Material 3, Navigation Compose, Room + SQLCipher, DataStore, CameraX.
- **Layers**: UI (Compose) -> ViewModel (StateHolder) -> UseCase (opzionale) -> Repository -> Data Source.
- **Naming**: `*Screen.kt` per UI, `*ViewModel.kt` per logica di stato, `*Repository.kt` per dati.
- **Previews**: Ogni Composable di tipo Screen o componente principale DEVE avere almeno una `@Preview` per Light e Dark mode.
- **Strings**: Spostare tutte le stringhe in `strings.xml`; evitare hardcoded UI copy.

---

## 4. Platform Rules (Android-only)
- **Flavors**: `full` (Pro), `demo` (Trial), `showcase` (Internal). Ogni modifica deve essere coerente tra i flavor.
- **UI Testing & Assets**: Usare `RepairAppTestTags` per i `testTag`. Ogni modifica UI che impatta la navigazione o i tag deve essere riflessa in `docs\operations\DEMO_ASSET_WORKFLOW.md`.
- **Storage**: Preferire `FileProvider` e storage privato (`filesDir`). Evitare permessi larghi se non necessari.

---

## 5. Data & Storage
- **Schema Lock**: `DATABASE_VERSION = 1` è bloccata. Non modificare entità Room, DAO persistenti o indici senza ordine esplicito dell'owner.
- **Security**: Database cifrato con SQLCipher. Non loggare dati sensibili, password o chiavi.
- **Integrations**: WhatsApp è gestito solo tramite handoff manuale (intent); non introdurre automazioni cloud/API.

---

## 6. Workflow Operativo
1. **Analisi**: Controllare `PROJECT_STATUS.json` e `git status`.
2. **Esecuzione**: Modifiche atomiche e focalizzate.
3. **Verifica**: 
   - Eseguire `scripts\powershell\android\check_env.ps1`.
   - Eseguire `scripts\powershell\project\check_project.ps1 -Flavor Full -SkipReleaseBuild`.
   - Se la UI cambia, verificare l'impatto su screenshot/video tramite `DEMO_ASSET_WORKFLOW.md`.
4. **Cleanup**: Eseguire Spotless e aggiornare `PROJECT_STATUS.json` (rimuovere task completati, aggiornare residui).

---

## 7. Final Reporting
Per ogni task che modifica file, riportare:
1. Cosa è cambiato e perché.
2. File modificati e documentazione aggiornata.
3. Check eseguiti (build, test, spotless) e risultati.
4. Stato aggiornato di `PROJECT_STATUS.json`.
5. Eventuali rischi o blocchi residui (es. impatto su schema Room).
