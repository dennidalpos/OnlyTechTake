# AGENTS.md - Mobile App Development Guide (OnlyTechTake)

Operational guide for AI agents. Environment: **Windows 11 + PowerShell**.
*Guida operativa per agenti AI. Ambiente: **Windows 11 + PowerShell**.*

---

## 1. Core Principles
1. **Source of Truth**: Always consult and update `PROJECT_STATUS.json`. (*Consultare e aggiornare sempre `PROJECT_STATUS.json`.*)
2. **Local-First & Privacy**: No cloud, telemetry, or analytics. No sensitive data logging. Database encrypted with SQLCipher. (*Niente cloud o telemetria. Database cifrato SQLCipher.*)
3. **Product Positioning**: The app is for **job management and technical documentation**, not just "repairs". Use prudent formulas like "helps reduce disputes". (*L'app è per gestione commesse e documentazione tecnica. Evitare claim legali assoluti.*)
4. **Versioning**: `versionCode = 1` is the baseline. Do NOT increment without explicit order. (*`versionCode = 1` è la base. NON incrementare senza ordine.*)

---

## 2. Technical Rules
- **Environment**: Use PowerShell wrappers in `scripts\powershell\`. (*Usare i wrapper PowerShell.*)
- **Dependencies**: Use ONLY `gradle/libs.versions.toml`. No hardcoded versions. (*Usare solo `libs.versions.toml`.*)
- **UI & Strings**: All strings in `strings.xml`. English default, Italian in `values-it`. No hardcoded copy. (*Tutte le stringhe in `strings.xml`.*)
- **Database**: `DATABASE_VERSION = 1` locked. Do not modify schema or entities without explicit order. (*Schema e versione DB bloccati.*)
- **Architecture**: UI (Compose/M3) → ViewModel → Repository. Every screen MUST have Light/Dark Previews. (*Architettura pulita. Preview Obbligatorie.*)
- **UI Tests**: Use **Robot** pattern. Call `TestUtils.refreshAppContainer()` after seeding or resetting DB. (*Pattern Robot. Refresh obbligatorio dopo reset DB.*)
- **Flavors**: Maintain consistency across `full` (Pro), `demo` (Trial), and `showcase` (Internal). Do NOT promote `showcase`. (*Mantenere coerenza tra i flavor. Non promuovere `showcase`.*)

---

## 3. Operational Workflow
1. **Analysis**: Check `PROJECT_STATUS.json` and `git status`.
2. **Execution**: Atomic changes. Run `./gradlew spotlessApply` (via ps1) before closing.
3. **Verification**:
   - `scripts\powershell\project\check_project.ps1 -Flavor Full -SkipReleaseBuild`
   - `scripts\powershell\project\quality.ps1`
4. **Cleanup**: Update `PROJECT_STATUS.json` (remove DONE, update timestamp).

---

## 4. Final Report Requirements
1. Modified/deleted files.
2. Executed checks and results.
3. Updated status of `PROJECT_STATUS.json`.
4. Residual risks.
