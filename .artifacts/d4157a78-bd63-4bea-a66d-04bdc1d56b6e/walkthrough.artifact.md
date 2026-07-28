# Walkthrough - App Audit & Technical Debt Cleanup

Ho completato una profonda ristrutturazione dell'app per migliorare l'architettura, rimuovere il debito tecnico e allineare il progetto ai principi della Clean Architecture.

## Modifiche Principali

### 1. Ristrutturazione del Layer Domain
- **Nuovo package `domain/usecases`**: Spostati e refactorizzati i casi d'uso core (`CreateIntakeOrderUseCase`, `GenerateOrderStatusPdfUseCase`).
- **Nuovo caso d'uso `ChangeOrderStatusUseCase`**: Estratta la logica di transizione degli stati dal repository al dominio.
- **Isolamento del Dominio**: Rimosse tutte le dipendenze da entità Room e DAO all'interno del layer domain. Ora il dominio dipende solo da interfacce e modelli propri.
- **Package `domain/logic`**: Creato per ospitare le policy di business (`OrderStatusPolicy`, `OrderClosurePermissionPolicy`, ecc.).

### 2. Pulizia e Decoupling del Layer Data
- **Repository Implementations**: Rinominati i repository concreti in `Default...Repository` e spostati sotto `data/repositories`.
- **Sotto-package specializzati**:
    - `data/backup/`: Centralizzata tutta la logica di backup, restore e reset.
    - `data/messaging/`: Centralizzata la gestione di WhatsApp e invio messaggi.
    - `data/orders/`: Organizzati gli helper per gli ordini (`EventWriter`, `CleanupService`, `DocumentStorage`).
    - `data/auth/`: Spostati i repository di autenticazione e gestione utenti.
- **Rimozione Dipendenze UI**: I repository non dipendono più dal `Context` per le stringhe di errore (spostate in UseCase o gestite tramite eccezioni).

### 3. Core & Error Handling
- **`DomainException.kt`**: Introdotte eccezioni tipizzate per il dominio (`OrderNotFound`, `BusinessRuleViolation`, ecc.) per una gestione degli errori più pulita nei ViewModel.
- **Mappers centralizzati**: Aggiornato `Mappers.kt` per supportare tutti i nuovi modelli di dominio.

### 4. Aggiornamento UI & ViewModels
- I ViewModel (`OrderDetailViewModel`, `NewOrderViewModel`, `SettingsViewModel`) ora iniettano i **Use Case** invece dei repository dove necessario, riducendo la logica di business nel layer UI.
- Aggiornati tutti i riferimenti e gli import in tutto il progetto (inclusi i test strumentali e unitari).

## Risultati dell'Audit
L'app è ora trattata come un "prodotto nuovo" con un'architettura solida:
- **Testabilità**: Più facile testare la logica di business in isolamento.
- **Manutenibilità**: Package organizzati per responsabilità, non più un unico "God Repository".
- **Debito Tecnico**: Rimosse stringhe hardcoded e leak di implementazione tra i layer.

> [!TIP]
> Si raccomanda di eseguire un `Clean Project` e `Rebuild` in Android Studio per rinfrescare i file generati da Hilt e KSP dopo questi massicci spostamenti di file.
