# Release Notes - v0.1.0 (Alpha)

**Data:** 25 Maggio 2026
**Stato:** Alpha / Internal Testing

## Novità principali
- **Workflow completo:** Implementata la gestione delle commesse dalla presa in carico alla chiusura.
- **Local-first:** Database Room locale con gestione media e firme integrata.
- **Wizard Nuova Commessa:** Processo guidato in 6 step per la creazione rapida di clienti e commesse.
- **Workspace Operativo:** Dettaglio commessa dinamico con azioni contestuali allo stato.
- **Documentazione PDF:** Generazione locale di report PDF per ogni commessa.
- **WhatsApp Manuale:** Integrazione con WhatsApp per l'invio rapido di messaggi preimpostati.
- **Backup & Restore:** Supporto per export ZIP completo di database e file multimediali.
- **Multi-utente:** Supporto per ruoli Admin e Operatore con permessi distinti.

## Miglioramenti tecnici
- Architettura Clean con Dependency Injection manuale (AppContainer).
- UI moderna in Jetpack Compose con supporto per schermi grandi (NavRail).
- Sistema di validazione robusto per transazioni database e integrità file.
- Tracciamento completo degli eventi tramite Timeline e Log di Audit.

## Note per il collaudo
- Testare il ripristino da ZIP su un'installazione pulita.
- Verificare la corretta rotazione delle foto scattate in-app.
- Controllare che i PDF generati contengano tutti gli eventi della timeline.
