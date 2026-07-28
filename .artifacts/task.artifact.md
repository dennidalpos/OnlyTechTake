# Task Checklist - Align Variants and Fix Errors

- [x] **Phase 1: Variant Alignment**
    - [x] Align `DemoMode.kt` API in `full` flavor
    - [x] Align `DemoMode.kt` API in `showcase` flavor
    - [x] Verify `DemoMode.kt` in `demo` flavor

- [x] **Phase 2: Fix & Relocate Unit Tests**
    - [x] `AdminUserRepositoryTest.kt`: Fix imports and move to `data.auth`
    - [x] `BackupRepositoryTest.kt`: Fix `BackupArchive` and move to `data.backup`
    - [x] `CatalogRepositoryTest.kt`: Fix renames and move to `domain.repositories` (Wait, I kept it in `data.repositories` but fixed the code)
    - [x] `CustomerRepositoryTest.kt`: Fix renames and move to `domain.repositories` (Kept in `data.repositories` as it tests impl)
    - [x] `TemplateRepositoryTest.kt`: Fix renames and move to `domain.repositories` (Kept in `data.repositories`)
    - [x] `WhatsAppSendersTest.kt`: Fix renames and move to `data.messaging`
    - [x] `CreateIntakeOrderUseCaseTest.kt`: Update repository reference
    - [x] `OrderDetailViewModelTest.kt`: Fix constructor parameters
    - [x] `IntakeStepperTest.kt`: Fix `StagedOrderMedia` imports
    - [x] `WorkspaceUiHelpersTest.kt`: Fix extension function receiver

- [x] **Phase 3: Legacy Cleanup**
    - [x] `AuthNavHost.kt`: Replace hardcoded Italian strings with `R.string` references

- [x] **Phase 4: Verification & Finalization**
    - [x] Run `:app:assembleFullDebug`
    - [x] Run `:app:assembleDemoDebug`
    - [x] Run `:app:assembleShowcaseDebug`
    - [x] Run all Unit Tests for all flavors
    - [x] Update `PROJECT_STATUS.json`
