# Store listing — Italiano

Bozze da revisionare in Play Console. Il codice configura Full e Demo come app separate; Showcase resta fuori store.

## OnlyTechTake Pro

**Nome app:** OnlyTechTake Pro
**Descrizione breve:** Gestionale locale per commesse, clienti e documentazione tecnica.

**Descrizione completa:**
OnlyTechTake gestisce il ciclo delle commesse di riparazione sul dispositivo Android: accettazione, diagnosi, preventivo, lavorazione e consegna.

- Anagrafica clienti, ricerca e storico interventi.
- Foto, video con audio e firme autografe sullo schermo.
- PDF di accettazione e riepilogo, esportazione delle commesse in XLSX.
- Template per messaggi WhatsApp aperti e inviati manualmente dall'utente.
- Account admin e operatori con permessi locali.
- Backup ZIP e ripristino locale con i limiti indicati sotto.

Il database è cifrato con SQLCipher. Media, documenti e preferenze sono conservati come file privati separati. Non sono presenti backend proprietari, sincronizzazione cloud automatica o analytics nel codice esaminato. Condivisioni ed esportazioni avvengono su richiesta dell'utente verso l'app o il provider scelto, che può anche essere un servizio cloud.

**Limiti del backup:** La password ZIP attuale protegge solo le voci database; configurazione, media e preferenze restano in chiaro. XLSX/PDF/JSON esportati singolarmente non sono cifrati dall'app. Proteggere separatamente le copie esportate. Il ripristino richiede la chiave originale dell'installazione e lo schema compatibile: non è una migrazione garantita su un nuovo telefono. La qualificazione del ripristino e le correzioni di recupero restano necessarie prima del rilascio in produzione.

**Categoria proposta:** Business / Produttività.

## OnlyTechTake Demo

**Nome app:** OnlyTechTake Demo
**Descrizione breve:** Prova il flusso locale di gestione commesse OnlyTechTake.

**Descrizione completa:**
Prova il flusso di accettazione e gestione di una commessa, con anagrafica clienti, foto/video e PDF dimostrativi.

- Massimo una commessa complessiva presente nell'app.
- Admin locale e fino a tre operatori attivi.
- Export XLSX/ZIP e ripristino riservati a Full.
- PDF con watermark Demo.

Il limite conta anche le commesse chiuse, non soltanto quelle attive. La gestione locale e le condivisioni manuali seguono il modello descritto per Pro.
