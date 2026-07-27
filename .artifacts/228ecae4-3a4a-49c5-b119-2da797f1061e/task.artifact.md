# Task List - Refactoring OnlyTechTake

- `[x]` **Data Layer & Room Optimization**
    - `[x]` Aggiungere `OrderDetailProjection` in `DaoProjections.kt`
    - `[x]` Aggiornare `RepairOrderDao.kt` con `@Transaction`
    - `[x]` Rifattorizzare `OrderWorkflowQueries.kt` per usare la proiezione
    - `[x]` Rimuovere `fallbackToDestructiveMigration` in `RepairAppDatabase.kt`
- `[x]` **ViewModel & Architecture Cleanup**
    - `[x]` Eliminare `SharedWorkspaceViewModel.kt`
    - `[x]` Aggiornare `DashboardScreen.kt` e `DashboardNavHost.kt` per rimuovere `SharedWorkspaceViewModel`
    - `[x]` Rifattorizzare `AuthenticatedActionRunner` per supporto DI o semplificazione
- `[x]` **Order Detail Refactoring**
    - `[x]` Pulizia `OrderDetailViewModel.kt` (rimozione ridondanze)
    - `[x]` Aggiornare `OrderDetailWorkspace.kt` e altri componenti UI
- `[ ]` **Verifica e Test**
    - `[ ]` Esecuzione unit test
    - `[ ]` Verifica manuale flussi principali
