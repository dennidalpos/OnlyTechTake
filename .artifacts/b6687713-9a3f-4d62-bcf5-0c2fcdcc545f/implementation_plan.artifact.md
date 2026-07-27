# Implementation Plan - Align All Variants, Build & Test

Align all flavor variants (Full, Demo, Showcase) with the latest architectural changes (OrderWorkflowRepository refactoring), synchronize versioning, and perform a full verification cycle (build, quality, test).

## User Review Required

> [!IMPORTANT]
> - The `versionName` in `app/build.gradle.kts` will be updated to `1.4.0` to match `PROJECT_STATUS.json` (Source of Truth).
> - `versionCode` remains `1` as per `AGENTS.md`.

## Proposed Changes

### Configuration Alignment

#### [MODIFY] [app/build.gradle.kts](file:///D:/GITHUB/OnlyTechTake/app/build.gradle.kts)
- Update `versionName` to `1.4.0`.

#### [MODIFY] [PROJECT_STATUS.json](file:///D:/GITHUB/OnlyTechTake/PROJECT_STATUS.json)
- Update `last_updated` timestamp.
- Clear `completed_recently` after execution.

### Verification Cycle (Execution)

1. **Spotless**: Apply formatting.
2. **Build**:
   - `assembleFullDebug`
   - `assembleDemoDebug`
   - `assembleShowcaseDebug`
3. **Quality**:
   - Run `lint` or quality scripts mentioned in `AGENTS.md`.
4. **Tests**:
   - `testFullDebugUnitTest`
   - `testDemoDebugUnitTest`
   - `testShowcaseDebugUnitTest`
   - `connectedFullDebugAndroidTest` (if a device is available/necessary, otherwise skip or run local unit tests for all variants).

## Verification Plan

### Automated Tests
- Run Gradle tasks for each variant:
  - `./gradlew spotlessApply`
  - `./gradlew assembleFullDebug assembleDemoDebug assembleShowcaseDebug`
  - `./gradlew testFullDebugUnitTest testDemoDebugUnitTest testShowcaseDebugUnitTest`
- Run quality script:
  - `powershell -ExecutionPolicy Bypass -File scripts\powershell\project\quality.ps1 -Apply -NonInteractive`

### Manual Verification
- Check `PROJECT_STATUS.json` for consistency.
