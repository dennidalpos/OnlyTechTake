# Implementation Plan - Fix Warnings in Dashboard and Auth Components

This plan addresses several lint warnings and adherence to project rules (previews) in recently modified files, specifically `OrderFilterDialog.kt`, `DashboardScreen.kt`, and `AuthGateScreen.kt`.

## User Review Required

> [!NOTE]
> All changes are minor lint fixes and additions of previews to comply with `AGENTS.md` guidelines. No functional logic is changed.

## Proposed Changes

### Dashboard Component

#### [MODIFY] [OrderFilterDialog.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/orders/OrderFilterDialog.kt)
- Remove unused import `androidx.compose.material3.*`.
- Add trailing comma to `Modifier.heightIn`.
- [NEW] Add Light and Dark Previews using `RepairAppTheme`.

#### [MODIFY] [DashboardScreen.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/DashboardScreen.kt)
- Name boolean parameter `value = false` in `mutableStateOf`.
- Move lambda outside parentheses for `onItemClick` in `DashboardBottomBar` and `DashboardNavRail`.
- Add trailing commas.

### Auth Component

#### [MODIFY] [AuthGateScreen.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/auth/AuthGateScreen.kt)
- Simplify boolean expression if applicable (based on `analyze_file` feedback).

## Verification Plan

### Automated Tests
- Run `analyze_file` on all modified files to ensure 0 warnings/errors.
- Run `gradle_build app:assembleDebug` to ensure no regression.

### Manual Verification
- Visual inspection of the new Previews in the IDE.
