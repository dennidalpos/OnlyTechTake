# Release Notes - v0.2.0 (Release Preparation)

**Data:** 18 Giugno 2026
**Stato:** Release Candidate / Internal Testing

## Novità principali
- **Preparazione Store:** Allineamento versioni e configurazioni per il rilascio su Google Play (Pro e Demo).
- **Integrazione Design System:** Consolidamento della UI con badge di stato e navigazione "Back to Top".
- **Ottimizzazione Flavor:** Distinzione netta tra le varianti Full (Pro), Demo e Showcase.
- **Data Safety:** Revisione dei flussi di dati per conformità alle policy di Google Play (Local-only).

## Miglioramenti
- Incremento `versionCode` a 2 e `versionName` a 0.2.0.
- Aggiornamento della documentazione di release e delle checklist operative.
- Verifica del sistema di signing locale per la generazione di AAB (Android App Bundle).

## Note per il collaudo
- Verificare la corretta generazione degli AAB per entrambi i flavor (Full e Demo).
- Eseguire smoke test sulla versione release minificata.
- Controllare la coerenza dei testi nello Store Listing rispetto alle funzionalità reali.
