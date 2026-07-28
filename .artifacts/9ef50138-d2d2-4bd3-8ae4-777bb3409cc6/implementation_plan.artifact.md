# Implementation Plan - App Audit & Critical Refactoring

Audit critico e refactoring dell'app per rimuovere il debito tecnico, migliorare la modularità e pulire la logica di business.

## User Review Required

> [!IMPORTANT]
> Questo piano prevede modifiche **distruttive** alla struttura delle feature `Customer` e `OrderDetail`.
> La logica di navigazione e gestione dello stato verrà centralizzata per ridurre la complessità dei Composable.

## Open Questions

Non ci sono domande aperte al momento. Procederò con il refactoring basato sulle best practice identificate.

## Proposed Changes

### [Feature Refactoring]

Il focus principale è la scomposizione dei file monolitici e la centralizzazione dello stato.

#### [MODIFY] [CustomerPanel.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/customers/CustomerPanel.kt)
Scomposizione del file in:
- `CustomerListContent.kt`: Gestione della lista e ricerca.
- `CustomerFormContent.kt`: Gestione del form di inserimento/editing.
- `CustomerDetailContent.kt`: Visualizzazione del dettaglio e ordini associati.

#### [MODIFY] [OrderDetailWorkspace.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/detail/OrderDetailWorkspace.kt)
Scomposizione in componenti atomici:
- `OrderActionCard.kt`: Estrazione della logica delle azioni di stato.
- `OrderSections.kt`: Separazione di Note, Foto, Firme e Timeline.
- `OrderDetailDialogs.kt`: Spostamento dei dialoghi in un file dedicato.

#### [NEW] [CustomerViewState.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/customers/CustomerViewState.kt)
Introduzione di un oggetto di stato unificato per la feature Customer.

#### [NEW] [OrderDetailViewState.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/detail/OrderDetailViewState.kt)
Introduzione di un oggetto di stato unificato per la feature OrderDetail.

---

### [Navigation & Dashboard Refactoring]

#### [MODIFY] [DashboardScreen.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/DashboardScreen.kt)
- Pulizia della logica di navigazione.
- Spostamento della gestione del "dirty state" nel `DashboardViewModel`.

#### [MODIFY] [DashboardNavHost.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/DashboardNavHost.kt)
- Semplificazione dei parametri passati ai panel.

---

### [Core & Logic Cleanup]

#### [MODIFY] [AppLocaleManager.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/core/utils/AppLocaleManager.kt)
- Miglioramento della gestione del locale per evitare dipendenze da intent extras fragili.

#### [DELETE] [Legacy/Unused Files]
- Identificazione e rimozione di eventuali DAO o helper non più utilizzati dopo il refactoring.

## Verification Plan

### Automated Tests
- Esecuzione dei test strumentali esistenti: `./gradlew connectedFullDebugAndroidTest`
- Verifica della qualità del codice: `scripts\powershell\project\quality.ps1 -NonInteractive`

### Manual Verification
- Verifica del corretto funzionamento dei form (Customer, Order).
- Test della navigazione adattiva (Wide vs Narrow) per assicurarsi che il refactoring non abbia rotto il layout.
- Verifica della gestione del "dirty state" e degli avvisi di salvataggio.
