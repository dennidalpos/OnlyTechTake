---
name: background-workmanager
description: Reliable background task execution for OnlyTechTake using WorkManager.
---

# Background Work (WorkManager)

Use WorkManager for persistent, deferrable background tasks (e.g., database cleanup, log rotations, media processing).

## Standards
1. **Constraints**: Always define constraints (e.g., `setRequiresCharging(true)`, `setRequiredNetworkType(NetworkType.CONNECTED)`).
2. **Hilt Integration**: Use `HiltWorker` and `@AssistedInject` for dependency injection into Workers.
3. **Unique Work**: Use `enqueueUniqueWork` or `enqueueUniquePeriodicWork` to prevent duplicates.
4. **Progress**: Use `setProgress` to report intermediate status if the task is long-running.

## Implementation
- **Worker**: Implement `CoroutineWorker` for Kotlin-first execution.
- **Configuration**: Initialize WorkManager in the `Application` class via `Configuration.Provider` if Hilt is used for workers.

## References
- [Getting Started with WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started)
- [Hilt and WorkManager Integration](https://developer.android.com/training/dependency-injection/hilt-jetpack#workmanager)
