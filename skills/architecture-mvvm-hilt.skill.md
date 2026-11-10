---
name: architecture-mvvm-hilt
description: Architecture standards for OnlyTechTake using MVVM, Clean Architecture, and Hilt.
---

# Architecture (MVVM + Hilt)

OnlyTechTake follows a layered architecture to ensure testability and maintainability.

## Layers
1. **UI (Compose)**: Declarative UI components.
2. **ViewModel**: Manages UI state and business logic. Uses `SavedStateHandle` if necessary.
3. **Repository**: Single source of truth for data. Orchestrates Room, DataStore, and external APIs.
4. **Domain (Optional)**: Use Cases for complex business logic.

## Dependency Injection (Hilt)
- **Entry Points**: Use `@HiltAndroidApp` in the Application class and `@AndroidEntryPoint` in Activities/Fragments.
- **ViewModels**: Use `@HiltViewModel` and `@Inject constructor`.
- **Scopes**: Use `@Singleton` for repositories and database instances.

## References
- [Android Architecture Recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Dependency Injection with Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
