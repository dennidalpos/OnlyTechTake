# Audit Critico e Piano di Modernizzazione (OnlyTechTake Repair App)

L'audit ha rilevato una struttura solida basata su **Clean Architecture** e **Jetpack Compose M3**. Tuttavia, con la crescita del progetto, alcuni componenti "Gate" e "Dashboard" sono diventati eccessivamente complessi, mescolando logica di navigazione, gestione dello stato e UI.

Il piano prevede una rifattorizzazione profonda ("distruttiva" dove necessario) per eliminare il debito tecnico e migliorare la manutenibilità.

## User Review Required

> [!IMPORTANT]
> **Introduzione di Hilt**: Propongo di passare dalla Dependency Injection manuale (Nested Containers) a **Hilt**. Questo permetterà di ripulire drasticamente i file `NavHost` e i `ViewModelProvider.Factory`.
> Se preferisci mantenere la DI manuale per coerenza con il resto del progetto "legacy", posso procedere comunque con una scomposizione manuale più pulita.

> [!WARNING]
> **Refactoring AuthGate**: Questa modifica cambierà radicalmente la struttura di `AuthGateScreen.kt`. La logica di inizializzazione sarà separata dagli stati UI.

## Open Questions
- Confermi l'introduzione di **Hilt**? È lo standard moderno e risolverebbe la verbosità attuale del `DashboardNavHost`.
- Vuoi che mantenga la compatibilità con i dati esistenti durante il refactoring del database (se necessario)? Anche se hai autorizzato modifiche distruttive, è meglio chiarire se il reset del DB è accettabile in questa fase di audit.

## Proposed Changes

### 1. [Componente] Dependency Injection & Core
Semplificazione della gestione delle dipendenze per ridurre il boilerplate.

#### [MODIFY] [libs.versions.toml](file:///D:/GITHUB/OnlyTechTake/gradle/libs.versions.toml)
- Aggiunta di Hilt e dei relativi plugin.

#### [MODIFY] [AppContainer.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/core/di/AppContainer.kt)
- Preparazione per la transizione a Hilt o scomposizione dei factory.

---

### 2. [Componente] Auth Feature (B12)
Scomposizione di `AuthGate` in componenti atomici.

#### [MODIFY] [AuthGateScreen.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/auth/AuthGateScreen.kt)
- Estrazione di `LoginContent`, `SetupContent`, `WizardContent` e `RecoveryContent` in file separati.
- Pulizia della logica di "Auto-Seed" per lo Showcase.

#### [MODIFY] [AuthGateViewModel.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/auth/AuthGateViewModel.kt)
- Scomposizione della logica in Use Cases (es. `LoginUseCase`, `RestoreBackupUseCase`) per alleggerire il ViewModel.

---

### 3. [Componente] Dashboard & Navigation
Miglioramento dell'esperienza di navigazione e scomposizione della UI.

#### [MODIFY] [DashboardScreen.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/DashboardScreen.kt)
- Estrazione della gestione della `NavigationBar` e `NavigationRail`.
- Spostamento della logica di "Unsaved Changes" in un Navigation Interceptor o componente dedicato.

#### [MODIFY] [DashboardNavHost.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/DashboardNavHost.kt)
- Rimozione dei Factory manuali (se Hilt approvato).
- Ottimizzazione delle rotte type-safe.

---

### 4. [Componente] Clean Code & Legacy
Rimozione di boilerplate e logiche ridondanti.

#### [MODIFY] [RepairAppDatabase.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/data/local/db/RepairAppDatabase.kt)
- Ottimizzazione dell'inizializzazione SQLCipher.

#### [DELETE] Eventuali file o funzioni orfane rilevate durante l'audit.

## Verification Plan

### Automated Tests
- Esecuzione dei test esistenti per garantire regressione nulla.
- `./gradlew check`
- Verifica del pattern **Robot** nei test UI.

### Manual Verification
- Verifica dei flussi di First Setup e Login.
- Test del backup/restore (critico).
- Verifica del comportamento Responsive (NavRail vs NavigationBar).
