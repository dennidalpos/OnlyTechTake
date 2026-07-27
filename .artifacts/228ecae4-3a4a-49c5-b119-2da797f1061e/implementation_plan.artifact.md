# Audit Critico e Refactoring dell'App OnlyTechTake

Questo piano mira a semplificare l'architettura dell'app, rimuovere il debito tecnico e ottimizzare le pipeline di dati, trattando l'app come un "nuovo progetto" dove la pulizia e la manutenibilità sono prioritarie.

## User Review Required

> [!IMPORTANT]
> **Modifiche Distruttive**: Rimuoverò `SharedWorkspaceViewModel` e modificherò la gestione del database Room (rimozione del fallback distruttivo). Questo semplifica il codice ma richiede una verifica attenta dei flussi UI.
>
> **Refactoring Action Runner**: L'attuale `AuthenticatedActionRunner` viene istanziato manualmente in ogni ViewModel. Proporrò una soluzione centralizzata per ridurre il boilerplate.

## Proposed Changes

### 1. Data Layer & Room Optimization
Ottimizzazione del recupero dei dettagli dell'ordine per evitare combinazioni manuali di Flow e casting fragili.

#### [MODIFY] [DaoProjections.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/data/local/dao/DaoProjections.kt)
- Aggiunta di `OrderDetailProjection` con `@Embedded` e `@Relation`.

#### [MODIFY] [RepairOrderDao.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/data/local/dao/RepairOrderDao.kt)
- Aggiunta di un metodo `@Transaction` per recuperare `OrderDetailProjection`.

#### [MODIFY] [OrderWorkflowQueries.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/data/repositories/OrderWorkflowQueries.kt)
- Semplificazione di `observeOrderDetail` per usare la nuova proiezione Room.

#### [MODIFY] [RepairAppDatabase.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/data/local/db/RepairAppDatabase.kt)
- Rimozione di `.fallbackToDestructiveMigration()` per allinearsi agli standard di produzione.

---

### 2. ViewModel & Architecture Cleanup
Semplificazione della gestione dello stato condiviso e riduzione del boilerplate nelle azioni asincrone.

#### [DELETE] [SharedWorkspaceViewModel.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/workspace/SharedWorkspaceViewModel.kt)
- Rimozione del ViewModel proxy ridondante. Lo stato di `WorkspaceManager` sarà consumato direttamente o tramite ViewModel specifici.

#### [MODIFY] [DashboardScreen.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/DashboardScreen.kt) e dipendenti
- Aggiornamento per iniettare `WorkspaceManager` o utilizzare ViewModel specifici al posto di `SharedWorkspaceViewModel`.

#### [MODIFY] [AuthenticatedActionRunner.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/workspace/AuthenticatedActionRunner.kt)
- Trasformazione in un componente più integrato o fornitura tramite DI per evitare istanziazioni manuali nei ViewModel.

---

### 3. Pipeline di Lavoro (Order Detail)
Refactoring di `OrderDetailViewModel` per renderlo più snello e focalizzato.

#### [MODIFY] [OrderDetailViewModel.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/detail/OrderDetailViewModel.kt)
- Riduzione della logica di delega a `WorkspaceManager`.
- Integrazione della nuova gestione `ActionRunner`.
- Pulizia dei metodi inutilizzati o ridondanti.

## Verification Plan

### Automated Tests
- Esecuzione dei test esistenti: `./gradlew testFullDebugUnitTest`.
- Verifica che i test di `OrderDetailViewModel` e `OrderWorkflowQueries` passino con la nuova logica.

### Manual Verification
- Verifica del caricamento dei dettagli dell'ordine nel Dashboard.
- Verifica che le azioni (cambio stato, aggiunta note) funzionino correttamente.
- Verifica che i messaggi di successo/errore (gestiti da `WorkspaceManager`) appaiano correttamente.
