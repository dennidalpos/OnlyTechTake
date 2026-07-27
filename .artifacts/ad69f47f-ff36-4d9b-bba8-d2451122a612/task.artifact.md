# Task List - Modernizzazione OnlyTechTake

Track dei progressi per il refactoring profondo e l'introduzione di Hilt.

- `[x]` Fase 1: Setup Hilt & Core DI
    - `[x]` Aggiornamento dipendenze in `libs.versions.toml`
    - `[x]` Configurazione Hilt nel `build.gradle.kts` (root e app)
    - `[x]` Annotazione `RepairAppApplication` e `MainActivity`
    - `[x]` Creazione dei moduli Hilt (`DatabaseModule`, `RepositoryModule`, `AppModule`, `UseCaseModule`)
- `[/]` Fase 2: Refactoring AuthGate (B12)
    - `[ ]` Scomposizione `AuthGateScreen.kt` in componenti atomici
    - `[x]` Refactoring `AuthGateViewModel` con Hilt
    - `[ ]` Estrazione logica Seeding in un componente dedicato
- `[ ]` Fase 3: Dashboard & Navigation
    - `[ ]` Pulizia `DashboardScreen.kt` (estrazione Nav components)
    - `[ ]` Refactoring `DashboardNavHost.kt` per rimuovere Factory manuali
    - `[ ]` Ottimizzazione rotte type-safe
- `[ ]` Fase 4: Finalizzazione & Cleanup
    - `[ ]` Rimozione definitiva `AppContainer` e factory manuali
    - `[ ]` Verifica integrità SQLCipher
    - `[ ]` Esecuzione controlli di qualità (`check_project.ps1`)
