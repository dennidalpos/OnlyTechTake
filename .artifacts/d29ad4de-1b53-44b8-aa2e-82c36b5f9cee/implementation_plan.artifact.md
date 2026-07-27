# Piano di Perfezionamento e Preparazione al Rilascio (OnlyTechTake)

Dopo la modernizzazione dell'architettura, questo piano mira a completare le funzionalità UI avanzate e preparare l'app per la pubblicazione su Google Play, eliminando gli ultimi residui di "lavori in corso".

## User Review Required

> [!IMPORTANT]
> L'implementazione del **Split-View** cambierà il comportamento dell'app su tablet e dispositivi pieghevoli, mostrando la lista ordini e il dettaglio fianco a fianco.

> [!NOTE]
> Verrà generata una documentazione specifica per la **Data Safety** di Google Play, utile per compilare correttamente il modulo nello Store.

## Propositi di Cambiamento

---

### 1. [Component Name] UI: Split-View Orders
Completare il refactoring iniziato in `OrdersPanel.kt` per supportare schermi grandi.

#### [MODIFY] [OrdersPanel.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/orders/OrdersPanel.kt)
- Sostituire il placeholder "refactoring in progress" con l'integrazione reale di `OrderDetailWorkspace`.
- Gestire lo stato della selezione ordine per aggiornare il pannello di dettaglio senza navigazione distruttiva.
- Utilizzare una chiave unica per `hiltViewModel` nel pannello di dettaglio per supportare il cambio di `orderId`.

#### [MODIFY] [OrderDetailWorkspace.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/features/dashboard/detail/OrderDetailWorkspace.kt)
- Rendere il componente più flessibile per l'uso all'interno di un pannello (es. rimozione di Scaffold se già presente o adattamento dei margini).

---

### 2. [Component Name] Documentation: Google Play Data Safety
Creare una guida tecnica per la compilazione della Data Safety basata sulla natura "Local-first" dell'app.

#### [NEW] [DATA_SAFETY_GUIDE.md](file:///D:/GITHUB/OnlyTechTake/docs/play-store/DATA_SAFETY_GUIDE.md)
- Documentare ogni campo richiesto da Google (Location, Personal Info, Photos/Videos) spiegando che la raccolta è locale e nessuna condivisione automatica avviene con lo sviluppatore.

---

### 3. [Component Name] Clean Audit
Rimuovere ogni commento `TODO` o `refactoring in progress` rimanente.

#### [MODIFY] [MainActivity.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/MainActivity.kt)
- Verificare e pulire eventuali log o rimasugli di debug.

## Open Questions

- Nel split-view, quando un ordine viene eliminato dal pannello di dettaglio, la lista deve aggiornarsi automaticamente e la selezione deve essere resettata. *Questo dovrebbe essere gestito nativamente dai Flow dei Repository.*

## Verification Plan

### Automated Tests
- Eseguire i test unitari esistenti per assicurarsi che il cambio di logica nel ViewModel del dettaglio non rompa nulla.

### Manual Verification
- Testare lo Split-View su un emulatore Tablet o ridimensionando la finestra (se supportato).
- Verificare che la navigazione su smartphone rimanga invariata.
