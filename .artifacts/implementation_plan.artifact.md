# Implementation Plan - Align Variants and Fix Errors

The goal of this task is to align all product variants (`Full`, `Demo`, `Showcase`) with the latest architectural changes, resolve the extensive unit test compilation errors resulting from recent refactorings, and perform a general cleanup of legacy code debt.

## User Review Required

> [!IMPORTANT]
> - I will be moving several unit tests to their corresponding packages (e.g., `data/repositories` -> `data/auth`, `data/backup`) to match the new project structure. This will improve maintainability but changes the file organization.
> - I will be fixing hardcoded strings in navigation files and moving them to `strings.xml`.

## Proposed Changes

### 1. Unit Test Fixes & Refactoring
Resolve all compilation errors in unit tests by updating imports, fixing class renames, and ensuring correct package visibility.

#### [MODIFY] [AdminUserRepositoryTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/data/repositories/AdminUserRepositoryTest.kt)
- Fix imports and package visibility.
- Move to `com.onlytechtake.repairapp.data.auth`.

#### [MODIFY] [BackupRepositoryTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/data/repositories/BackupRepositoryTest.kt)
- Fix imports for `BackupArchive`.
- Move to `com.onlytechtake.repairapp.data.backup`.

#### [MODIFY] [CatalogRepositoryTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/data/repositories/CatalogRepositoryTest.kt)
- Fix imports and renames.
- Move to `com.onlytechtake.repairapp.domain.repositories`.

#### [MODIFY] [CustomerRepositoryTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/data/repositories/CustomerRepositoryTest.kt)
- Fix imports and renames.
- Move to `com.onlytechtake.repairapp.domain.repositories`.

#### [MODIFY] [TemplateRepositoryTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/data/repositories/TemplateRepositoryTest.kt)
- Fix imports and renames.
- Move to `com.onlytechtake.repairapp.domain.repositories`.

#### [MODIFY] [WhatsAppSendersTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/data/repositories/WhatsAppSendersTest.kt)
- Fix imports and renames.
- Move to `com.onlytechtake.repairapp.data.messaging`.

#### [MODIFY] [CreateIntakeOrderUseCaseTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/domain/workspace/CreateIntakeOrderUseCaseTest.kt)
- Update `OrderWorkflowRepository` to `OrderRepository`.

#### [MODIFY] [OrderDetailViewModelTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/features/dashboard/detail/OrderDetailViewModelTest.kt)
- Fix missing constructor parameters in tests.

#### [MODIFY] [IntakeStepperTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/test/java/com/onlytechtake/repairapp/features/dashboard/neworder/IntakeStepperTest.kt)
- Fix imports for `StagedOrderMedia`.

### 2. Variant Alignment (`DemoMode.kt`)
Ensure `DemoMode.kt` is consistent across all three flavors (`full`, `demo`, `showcase`).

#### [MODIFY] [DemoMode.kt](file:///D:/GITHUB/OnlyTechTake/app/src/demo/java/com/onlytechtake/repairapp/core/demo/DemoMode.kt)
#### [MODIFY] [DemoMode.kt](file:///D:/GITHUB/OnlyTechTake/app/src/full/java/com/onlytechtake/repairapp/core/demo/DemoMode.kt)
#### [MODIFY] [DemoMode.kt](file:///D:/GITHUB/OnlyTechTake/app/src/showcase/java/com/onlytechtake/repairapp/core/demo/DemoMode.kt)
- Align property declarations (e.g., `isEnabled` as `var` vs `const val`) to ensure consistent API for callers in `src/main`.

### 3. Legacy Cleanup
Remove hardcoded strings and ensure modern patterns are followed.

#### [MODIFY] [AuthNavHost.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/navigation/AuthNavHost.kt)
- Replace hardcoded Italian strings with references to `strings.xml`.

#### [MODIFY] [strings.xml](file:///D:/GITHUB/OnlyTechTake/app/src/main/res/values/strings.xml)
- Add missing strings for loading and error states if needed.

## Verification Plan

### Automated Tests
- Run unit tests for all flavors:
  - `:app:testFullDebugUnitTest`
  - `:app:testDemoDebugUnitTest`
  - `:app:testShowcaseDebugUnitTest`
- Run build for all flavors:
  - `:app:assembleFullDebug`
  - `:app:assembleDemoDebug`
  - `:app:assembleShowcaseDebug`

### Manual Verification
- Check `PROJECT_STATUS.json` update and timestamp.
