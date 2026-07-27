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
- **Landing Page**: Google Play Store links in `landing/index.html` are currently **dormant** (commented out). Use direct APK links for Demo and `mailto:` for Pro. Do NOT reactivate without explicit order. (*Link Play Store dormienti. Usare APK diretto e mail.*)
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
- `generate_static_brands.ps1`: (ImageMagick) SVG -> PNG/ICO/Favicon. Brand identity assets from `marketing/registered-assets/brand/identity/` and `landing/assets/brand/`.
- `create_demo_assets.ps1`: (Instrumented Tests) UI screenshots from JSON profiles in `demo/assets/profiles`. Supports `-Dark`, `-Locale`, and `-UseConnectedDevice`. Generates **vertical**, **square**, **landscape**, and **linkedin** (captioned) variants via GDI+ (`android_demo.ps1`). Assets go to `marketing/screenshots/` and `landing/assets/screenshots/`.
- `record_demo_device.ps1`: (FFmpeg) Screen recording of instrumental tests. Supports `-WhatsAppOptimized` (720p) and `-AutomaticTrim`. Distributed to `marketing/videos/` and `landing/assets/videos/`.
- `regenerate_public.ps1`: Master script for full asset refresh. Performs **Dynamic Discovery** of all profiles in `demo/assets/profiles`. Handles **Mixed Management**: for video-enabled profiles, it runs `record_demo_device.ps1`, then `create_demo_assets.ps1` with `-SkipTest`. Manages priority order to satisfy dependencies (e.g., social posts after campaign). Performs full cleanup and optimized APK installation.
- **Asset Locale**: Public assets MUST be generated in English (`-Locale en`). The scripts configure device locale via `adb` and pass `locale` extra to instrumentation.
- **Seed Data**: Use `DemoDataSeeder.kt` for consistent datasets. English strings are in `app/src/androidTest/res/values/strings.xml` (`TestR`). Includes realistic templates for all stages (Intake, Diagnosis, Result, Quote, Pickup, PDF).

---

## 6. Skill Management
- **Load Skills**: Before starting a task, read relevant files in the `skills/` directory using `read_file`. These contain project-specific constraints and official standards.
- **Update Skills**: If a task involves updating a core library or architecture pattern, update the corresponding skill file in `skills/`.
- **Portability**: The `skills/` directory must be committed to the repository to ensure consistent agent behavior across different environments.

---

## 7. Final Report & Status Requirements
1. **Modified/deleted files.**
2. **Executed checks and results.**
3. **Update `PROJECT_STATUS.json`**:
   - Remove completed tasks from `backlog`.
   - **DO NOT** keep history in `completed_recently` (empty the list).
   - Update `last_updated` timestamp.
4. **Residual risks.**
