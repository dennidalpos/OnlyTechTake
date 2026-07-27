# Implementation Plan - Project Cleanup and Dependency Update

Cleanup redundant code (technical debt), update libraries to the latest stable versions, and align project documentation.

## User Review Required

> [!IMPORTANT]
> - **Redundant Code Removal**: I will remove `isBackupReminderDue` and `exportCompleteBackup` from `HomeViewModel` and `OrdersViewModel` as they are now centralized in `DashboardViewModel` and `SettingsViewModel`.
> - **Library Updates**: I will update `spotless` to `8.9.0`. Most other libraries are already at their latest stable versions for 2026.
> - **Documentation Alignment**: `PROJECT_STATUS.json` will be updated to reflect this cleanup task.

## Proposed Changes

### [Component] Core & Cleanup

#### [MODIFY] [HomeViewModel.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/home/HomeViewModel.kt)
- Remove `isBackupReminderDue` property.
- Remove `exportCompleteBackup` function.
- Remove related `@Suppress("unused")`.

#### [MODIFY] [OrdersViewModel.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/orders/OrdersViewModel.kt)
- Remove `isBackupReminderDue` property.
- Remove `exportCompleteBackup` function.
- Remove related `@Suppress("unused")`.

#### [MODIFY] [SettingsViewModel.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/settings/SettingsViewModel.kt)
- Remove redundant `isBackupReminderDue` property (centralized in `DashboardViewModel`).

### [Component] Dependencies

#### [MODIFY] [libs.versions.toml](file:///D:/GITHUB/OnlyTechTake/gradle/libs.versions.toml)
- Update `spotless` version to `8.9.0`.

### [Component] Documentation

#### [MODIFY] [PROJECT_STATUS.json](file:///D:/GITHUB/OnlyTechTake/PROJECT_STATUS.json)
- Update `last_updated` timestamp.
- Update `status` to reflect the completed cleanup.
- Remove completed tasks from `backlog` if any.

#### [MODIFY] [AGENTS.md](file:///D:/GITHUB/OnlyTechTake/AGENTS.md)
- Ensure all technical rules and principles are up to date.

## Verification Plan

### Automated Tests
- Run `./gradlew spotlessApply` via PowerShell script to ensure code formatting.
- Run `./gradlew test` to ensure no regressions in business logic.
- Run `./gradlew assembleDebug` to verify build success.

### Manual Verification
- Verify that the app still shows the backup reminder correctly (since it now uses `DashboardViewModel`).
