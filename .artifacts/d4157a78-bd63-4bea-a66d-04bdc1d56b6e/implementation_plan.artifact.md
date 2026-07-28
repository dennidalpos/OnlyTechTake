# Implementation Plan - Critical App Audit & Technical Debt Cleanup

Audit and cleanup of the OnlyTechTakeRepairApp to improve architecture, remove legacy debt, and ensure consistency.

## User Review Required

> [!IMPORTANT]
> This plan involves **destructive changes**:
> - Reorganizing the `data` layer (moving files).
> - Refactoring `OrderWorkflowRepository` and `OrderWorkflowCommands`.
> - Moving business logic from Data to Domain layer.
> - Removing hardcoded strings and cleaning up `Context` usage in non-UI layers.

> [!WARNING]
> Database schema is **LOCKED** at version 1. No changes will be made to entities or DAOs that affect the schema.

## Proposed Changes

### 1. Domain Layer (Architecture Fix)
- **Goal**: Ensure Domain layer depends only on itself and Core.
- **Action**:
    - [NEW] Move all use cases from `domain/workspace` to a new `domain/usecases` package.
    - [MODIFY] [CreateIntakeOrderUseCase.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/domain/workspace/CreateIntakeOrderUseCase.kt): Remove dependencies on `data.local.entities` and `data.repositories`. Use Domain Models instead.
    - [MODIFY] [GenerateOrderStatusPdfUseCase.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/domain/workspace/GenerateOrderStatusPdfUseCase.kt): Remove `UserDao` dependency. Use `UserRepository` interface. Remove hardcoded strings.
    - [NEW] `domain/usecases/ChangeOrderStatusUseCase.kt`: Extract status transition logic from `OrderWorkflowCommands`.

### 2. Data Layer (Cleanup & Decoupling)
- **Goal**: Clean up the "God Repository" and remove UI dependencies.
- **Action**:
    - [MODIFY] [CustomerRepository.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/data/repositories/CustomerRepository.kt): Remove `Context` and `capitalizeFirst` (move to Mapper or Domain).
    - [MODIFY] [OrderWorkflowRepository.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/data/repositories/OrderWorkflowRepository.kt): Rename to `DefaultOrderRepository.kt`.
    - [MODIFY] [OrderWorkflowCommands.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/data/repositories/OrderWorkflowCommands.kt): Remove validation logic and permissions checks (move to Use Cases).
    - [DELETE] Redundant internal helper classes in `data/repositories` if they can be merged or moved.
    - [NEW] `data/backup/` package: Move `BackupArchive.kt`, `BackupRepository.kt`, `BackupMetadataService.kt`, etc. here.
    - [NEW] `data/messaging/` package: Move `WhatsAppSenders.kt` here.

### 3. Core & Utils
- **Goal**: Centralize formatting and string management.
- **Action**:
    - [MODIFY] `core/utils/Formatters.kt`: Ensure all formatting logic is here and not in repositories.
    - [NEW] `core/error/DomainException.kt`: Typed exceptions for domain errors (e.g., `OrderNotFoundException`, `PermissionDeniedException`).

### 4. UI & Features
- **Goal**: Update ViewModels to use the new Use Cases.
- **Action**:
    - [MODIFY] ViewModels in `features/` to inject Use Cases instead of Repositories where appropriate.

## Verification Plan

### Automated Tests
- Run existing unit tests: `./gradlew test`
- Run instrumentation tests (Robot pattern): `./gradlew connectedAndroidTest`

### Manual Verification
- Deploy to device/emulator.
- Test "New Service Order" flow (Intake).
- Test Status transitions in "Order Detail".
- Verify PDF generation works without hardcoded strings.
- Check "Backup" and "WhatsApp" integration.
