# AGENTS.md — Repository Instructions

Repository-specific instructions for AI Agents (Android Studio + Codex).

Primary local environment: **Windows + PowerShell**.

Use repository conventions when they are clear. Keep changes small, focused, verifiable, and limited to the requested task.

---

## 1. Priorities

1. **Preserve user work**: Inspect working tree before editing.
2. **Android Studio first**: Use IDE-native tools for analysis, refactoring, and resource management. Use Codex for complex automation, broad refactors, and logical implementations.
3. **Release Readiness**: Treat the project as "fresh" (no legacy dependencies) but target a "Production/Ready-for-Release" quality. Avoid temporary hacks or non-localized UI strings.
4. **Maintainability & Future-proofing**: Adhere to Clean Architecture and SOLID principles to allow future expansion (e.g., transition from LOCAL_ONLY to Cloud/Centralized backend) without breaking changes.
5. **Verified changes**: Run Gradle builds, lint, and available tests before considering a task complete.
6. **Project Hygiene**: Keep `PROJECT_STATUS.json` updated with actionable residuals only.

Do not change unrelated files. Do not introduce dependencies, public API changes, config/deployment changes, broad refactors, migrations, or destructive operations unless the task clearly requires them.

---

## 2. Fresh project policy & Scalability

OnlyTechTake Repair App is a modern app in active development.

* **Freshness**: Do not add Room migrations (unless requested), legacy workarounds, or historical cleanup. Prefer current Jetpack Compose and Material 3 standards.
* **Architecture**: Use the existing `AppContainer` based Dependency Injection.
* **Backend Readiness**: Design Repository and Data Source layers using interfaces to allow future swapping of local Room storage with a Cloud/API backend.
* **Localization**: Move all user-visible strings to `strings.xml`. Do not use hardcoded strings in Composable files.
* **Design System**: Use `RepairAppTheme` and components in `core/design/components`. Avoid direct Material 3 component usage if a Design System wrapper exists.

---

## 3. Data & Backup Policy

* **LOCAL_ONLY by default**: Current phase is strictly offline.
* **Manual ZIP backup**: Primary recovery mechanism.
* **Android Data Preservation**: Use `android:hasFragileUserData="true"`.
* **Database Safety**: Any schema change must be documented and tested for fresh installs.
  RICHIEDE CAMBIO SCHEMA - BLOCCATA

---

## 4. Workflow

1. **Check status**: `git status --short`.
2. **IDE Inspection**: Use Android Studio's "Analyze File" and "Find Usages" before editing.
3. **Atomic Implementation**: Smallest contiguous production-quality change.
4. **Documentation Sync**: Update relevant files in `docs/` or `README.md` if behavior, setup, or structure changes.
5. **Validation**: Run `:app:assembleDebug` and relevant unit tests.
6. **Reporting**: State what changed, files affected, and remaining tasks in `PROJECT_STATUS.json`.

---

## 5. Cleanliness & Demo Pipeline

Keep the repository clean of build artifacts, temporary files, and uncommitted generated reports.

### Demo asset pipeline

Use `docs/DEMO_ASSET_WORKFLOW.md` as the source of truth for assets.
After UI or workflow changes:
1. Update instrumented scenarios in `app/src/androidTest/.../demo/`.
2. Update and run `scripts/windows/create_demo_assets.ps1`.
3. Record the change in `docs/DEMO_ASSET_WORKFLOW.md`.

---

## 6. Structure & Maintainability

* **One Responsibility**: Keep files focused. Split logic from UI (ViewModel vs Composable).
* **Naming**: Be explicit. Avoid `utils`, `helpers`, `common` unless following existing patterns.
* **Extensibility**: When adding features (e.g., WhatsApp integration, PDF export), centralize templates and logic in `core` or `domain` to ensure consistency.

---

## 7. Final response requirements

For implementation tasks, include:
1. What changed.
2. Files changed.
3. Documentation updated (if applicable).
4. Build/Test results.
5. `PROJECT_STATUS.json` update.
6. Future integration risks (Backend, Cloud, etc.).
