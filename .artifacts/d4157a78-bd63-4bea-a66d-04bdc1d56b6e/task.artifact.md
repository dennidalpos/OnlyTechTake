# Task: Critical App Audit & Technical Debt Cleanup

## 1. Domain Layer Reorganization
- [x] Create `domain/usecases` package
- [x] Create `domain/repositories/UserRepository.kt` interface
- [x] Move and refactor `CreateIntakeOrderUseCase.kt` (remove data leaks)
- [x] Move and refactor `GenerateOrderStatusPdfUseCase.kt` (remove UserDao & Context leaks)
- [x] Create `ChangeOrderStatusUseCase.kt` (extract from OrderWorkflowCommands)
- [x] Move other use cases from `domain/workspace` to `domain/usecases`
- [x] Create `domain/logic` package and move policies (`OrderStatusPolicy`, etc.)
- [x] Create `domain/models/TemplateModels.kt` and `domain/models/WorkspaceModels.kt` (reorganized)

## 2. Data Layer Cleanup
- [x] Rename `OrderWorkflowRepository.kt` to `DefaultOrderRepository.kt`
- [x] Create `data/repositories/DefaultUserRepository.kt`
- [x] Reorganize `data/repositories/` into subpackages:
    - [x] `data/backup/`
    - [x] `data/messaging/`
    - [x] `data/orders/` (for helpers like `OrderNumberGenerator`, `OrderFileCleanupService`)
    - [x] `data/auth/`
- [x] Refactor `CustomerRepository.kt` to remove `Context` dependency (interface level)
- [x] Refactor `OrderWorkflowCommands.kt` to be a pure data implementation (validation/permissions moved to use cases)
- [x] Update all repository implementations to use domain interfaces and models.

## 3. Core & Utils
- [x] Move formatting logic from repositories to `core/utils/`
- [x] Create `core/error/DomainException.kt`

## 4. UI & Features
- [x] Update `DashboardViewModel.kt`
- [x] Update `OrderDetailViewModel.kt` to use new Use Cases
- [x] Update `NewOrderViewModel.kt` to use new Use Cases and domain models

## 5. Verification
- [/] Run Unit Tests
- [/] Run Instrumentation Tests
- [ ] Manual smoke test of main flows
