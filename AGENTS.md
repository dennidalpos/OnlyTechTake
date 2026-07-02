# AGENTS.md - Mobile App Development Guide (OnlyTechTake)

Operational guide for AI agents. Environment: **Windows 11 + PowerShell**.
*Guida operativa per agenti AI. Ambiente: **Windows 11 + PowerShell**.*

---

## 1. Core Principles
1. **Source of Truth**: Always consult and update `PROJECT_STATUS.json`. (*Consultare e aggiornare sempre `PROJECT_STATUS.json`.*)
2. **Local-First & Privacy**: No cloud, telemetry, or analytics. No sensitive data logging. Database encrypted with SQLCipher. (*Niente cloud o telemetria. Database cifrato SQLCipher.*)
3. **Product Positioning**: The app is for **job management and technical documentation**, not just "repairs". Use prudent formulas like "helps reduce disputes". (*L'app è per gestione commesse e documentazione tecnica. Evitare claim legali assoluti.*)
4. **Versioning & SDK**: `versionCode = 1` is the baseline. `targetSdk` is locked to **37**. Do NOT increment without explicit order. (*`versionCode = 1` è la base. `targetSdk = 37`. NON incrementare senza ordine.*)

---

## 2. Technical Rules
- **Environment**: Use PowerShell wrappers in `scripts\powershell\`. (*Usare i wrapper PowerShell.*)
- **Dependencies**: Use ONLY `gradle/libs.versions.toml`. No hardcoded versions. (*Usare solo `libs.versions.toml`.*)
- **UI & Strings**: All strings in `strings.xml`. English default, Italian in `values-it`. No hardcoded copy. Ensure cross-flavor consistency (Main, Demo, Showcase). (*Tutte le stringhe in `strings.xml`. Coerenza tra i flavor.*)
- **Database**: `DATABASE_VERSION = 1` locked. Do not modify schema or entities without explicit order. (*Schema e versione DB bloccati.*)
- **Architecture**: UI (Compose/M3) → ViewModel → Repository. Every screen MUST have Light/Dark Previews. (*Architettura pulita. Preview Obbligatorie.*)
- **UI Tests**: Use **Robot** pattern. Call `TestUtils.refreshAppContainer()` after seeding or resetting DB. (*Pattern Robot. Refresh obbligatorio dopo reset DB.*)
- **Media Assets**: Use `scripts\powershell\assets\` for marketing materials. Seed in English (`-Locale en`) for public assets. (*Usare script in `scripts\powershell\assets\` per materiale marketing.*)
- **Flavors**: Maintain consistency across `full` (Pro), `demo` (Trial), and `showcase` (Internal). Do NOT promote `showcase`. (*Mantenere coerenza tra i flavor. Non promuovere `showcase`.*)

---

## 3. Operational Workflow
1. **Analysis**: Check `PROJECT_STATUS.json` and `git status`.
2. **Execution**: Atomic changes. Run `./gradlew spotlessApply` (via ps1) before closing.
3. **Verification**:
   - `scripts\powershell\project\check_project.ps1 -Flavor Full -SkipReleaseBuild -NonInteractive`
   - `scripts\powershell\project\quality.ps1 -Apply -NonInteractive`
4. **Asset Refresh (if needed)**:
   - `scripts\powershell\assets\regenerate_public.ps1`
5. **Cleanup**: Update `PROJECT_STATUS.json` (remove DONE, update timestamp).

---

## 4. Media Asset Workflow
- `generate_static_brands.ps1`: (ImageMagick) SVG -> PNG/ICO/Favicon. Generates brand identity assets for landing page and social media from `marketing/registered-assets/brand/identity/onlytechtake-logo.svg` and `landing/assets/brand/onlytechtake-mark.svg`.
- `create_demo_assets.ps1`: (Instrumented Tests) Generates UI screenshots using JSON profiles in `demo/assets/profiles`. Supports `-Dark`, `-Locale`, and `-UseConnectedDevice`. Applies automatic transformations (**vertical/Story**, **square**, **landscape**) via GDI+ (`android_demo.ps1`). Assets are distributed to `marketing/screenshots/` and `landing/assets/screenshots/`.
- `record_demo_device.ps1`: (FFmpeg) Captures screen videos by executing instrumental tests. Supports `-WhatsAppOptimized` (720p, CRF 28) and `-AutomaticTrim`. Distributed to `marketing/videos/` and `landing/assets/videos/`.
- `regenerate_public.ps1`: Master script for full asset refresh. Handles **Mixed Management**: for video-enabled profiles (e.g., `operator-intake`), it first records the video (running the test), then runs the screenshot creator with `-SkipTest` to extract frames from the same device state. It performs a full cleanup of `asset/`, `marketing/screenshots/`, and `landing/assets/screenshots/` before generation.
- **Asset Locale**: Public assets MUST be generated in English (`-Locale en`). The scripts handle device locale configuration via `adb`.

---

## 5. Final Report & Status Requirements
1. **Modified/deleted files.**
2. **Executed checks and results.**
3. **Update `PROJECT_STATUS.json`**:
   - Remove completed tasks from `backlog`.
   - **DO NOT** keep history in `completed_recently` (empty the list to avoid bloat).
   - Focus on high-priority residuals and current plan.
4. **Residual risks.**
