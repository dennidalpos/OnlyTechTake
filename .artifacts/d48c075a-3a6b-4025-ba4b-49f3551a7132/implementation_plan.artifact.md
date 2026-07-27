# Allineamento Varianti e Qualità Progetto

Questo piano prevede l'allineamento delle varianti `demo` e `showcase` con le ultime modifiche architetturali (Room projections, DI ActionRunner, rimozione SharedWorkspaceViewModel) e la verifica della qualità tramite build e test.

## User Review Required

> [!IMPORTANT]
> Ho rilevato che la variante `showcase` non include il watermark nei PDF generati, a differenza della variante `demo`. Proporrò di allineare questo comportamento poiché entrambe sono versioni non commerciali per scopi dimostrativi.

## Proposed Changes

### Core Logic & Flavors

#### [MODIFY] [GenerateOrderStatusPdfUseCase.kt](file:///D:/GITHUB/OnlyTechTake/app/src/main/java/com/onlytechtake/repairapp/domain/workspace/GenerateOrderStatusPdfUseCase.kt)
- Abilitare il watermark anche se `BuildConfig.IS_SHOWCASE_MODE_ENABLED` è vero.

#### [MODIFY] [DemoMode.kt](file:///D:/GITHUB/OnlyTechTake/app/src/showcase/java/com/onlytechtake/repairapp/core/demo/DemoMode.kt) e [DemoMode.kt](file:///D:/GITHUB/OnlyTechTake/app/src/demo/java/com/onlytechtake/repairapp/core/demo/DemoMode.kt)
- Verificare e allineare eventuali discrepanze nei messaggi di errore o etichette. In particolare, assicurarmi che `showcase` non esponga involontariamente limiti della `demo`.

### Data & Seeders

#### [MODIFY] [DataSeederImpl.kt](file:///D:/GITHUB/OnlyTechTake/app/src/demo/java/com/onlytechtake/repairapp/demo/DataSeederImpl.kt)
- Verifica che il seeder della versione Demo sia allineato con la rimozione del fallback distruttivo del database (già verificato: usa `withTransaction` e `upsert`).

### Testing & Verification

#### [NEW] [GenerateOrderStatusPdfUseCaseShowcaseTest.kt](file:///D:/GITHUB/OnlyTechTake/app/src/testShowcase/java/com/onlytechtake/repairapp/domain/workspace/GenerateOrderStatusPdfUseCaseShowcaseTest.kt)
- Aggiungere un test per verificare il watermark nella variante `showcase`, speculare a quello presente in `demo`.

## Verification Plan

### Automated Tests
- `gradlew app:testFullDebugUnitTest`
- `gradlew app:testDemoDebugUnitTest`
- `gradlew app:testShowcaseDebugUnitTest`
- `gradlew app:assembleDebug` (per tutte le varianti)

### Manual Verification
- Verifica dei log di build per confermare l'assenza di warning su componenti rimossi (es. `SharedWorkspaceViewModel`).
- Esecuzione di `spotlessApply` per garantire la coerenza formale tra i sorgenti delle varie flavor.
