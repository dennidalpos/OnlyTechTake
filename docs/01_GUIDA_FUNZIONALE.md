# 01 - Guida funzionale

## Obiettivo

OnlyTechTake Repair App gestisce commesse locali per oggetti portati in negozio da clienti privati o aziende. L'app e' pensata per uso al banco, offline, senza backend.

La commessa resta generica: i dati specifici della lavorazione non diventano colonne dedicate, ma vengono registrati in categoria, descrizione, note interne, media, firme, timeline e messaggi.

## Ruoli

### Admin

Funzioni implementate:

- creare operatori;
- attivare o disattivare utenti;
- resettare password utenti;
- vedere e gestire le commesse;
- modificare categorie ad albero con livelli numerici da 0 a N;
- modificare template privacy, condizioni e WhatsApp;
- impostare la modalita WhatsApp manuale;
- esportare configurazione, database, ZIP completo e PDF commessa;
- archiviare in batch le commesse scartate e cancellare definitivamente quelle gia' archiviate;
- eseguire reset totale dati con conferma testuale `RESET`.

### Operatore

Funzioni implementate:

- creare clienti;
- creare commesse;
- consultare le commesse;
- gestire le commesse assegnate;
- riassegnare una propria commessa;
- aggiungere note, foto, allegati, foto lavoro finito e una firma cliente alle proprie commesse;
- preparare messaggi WhatsApp manuali per le proprie commesse;
- confermare manualmente l'invio WhatsApp;
- registrare risposta cliente o chiusura tramite azioni operative nello stato commessa.

Limiti operatore:

- non accede alla sezione Admin;
- non modifica categorie, template o impostazioni generali;
- non esegue export admin;
- non esegue reset totale dati.

## Primo avvio e login

Primo avvio:

```text
Apertura app
Nessun admin presente
Setup admin
Nome, username, password, conferma password
Creazione admin
Dashboard
```

Dopo il primo avvio l'app mostra il login. La password minima e' di 8 caratteri. Dopo 5 tentativi falliti l'utente viene bloccato temporaneamente per 15 minuti. La sessione scade dopo 30 minuti di inattivita' e il logout manuale e' disponibile dalla top bar.

## Dashboard

La dashboard implementata usa tab:

```text
Home
Nuova
Clienti
Commesse / Commessa
Config
Admin
```

La Home mostra metriche locali e quattro code operative:

- Aperte: commesse `OPEN`;
- In lavorazione / diagnosi: commesse `IN_PROGRESS` e `DIAGNOSIS`;
- Risultato / riparazione: commesse `RESULT` e `REPAIRING`;
- Pronte: commesse `READY_FOR_PICKUP`.

Ogni coda apre l'elenco commesse con filtro coerente allo stato.

## Clienti

Il cliente puo' essere:

```text
PRIVATE
BUSINESS
```

Campi salvati:

- nome;
- cognome;
- ragione sociale;
- P.IVA;
- codice fiscale;
- telefono;
- email;
- indirizzo;
- note.

Per la presa in carico serve un cliente esistente oppure un nuovo cliente valido: privato con nome, cognome e telefono/email, oppure azienda con ragione sociale e telefono/email.

## Nuova commessa

La creazione commessa procede a step confermati:

- cliente esistente o nuovo cliente valido;
- problema/sintesi obbligatoria;
- categoria attiva obbligatoria, selezionata navigando l'albero fino al ramo operativo;
- almeno una foto o allegato;
- privacy, condizioni e firma cliente. I testi privacy e condizioni si aprono in dialog dalla sezione firma.

Ogni step confermato diventa un riepilogo. Il pulsante `Modifica` riapre lo step e richiede di riconfermare gli step successivi prima della creazione.
Durante lo step foto/allegati l'operatore puo' eliminare media venuti male, scattare di nuovo o aggiungere altri file.

Alla conferma, `CreateIntakeOrderUseCase` crea cliente eventuale, commessa, firma e media dentro una transazione Room. Se firma o media falliscono, la commessa non viene confermata nel database e i file gia' scritti vengono ripuliti quando possibile.

La presa in carico e' bloccata se non esiste almeno una categoria attiva.

Gli allegati importabili sono:

- immagini JPEG, PNG, WebP;
- PDF;
- TXT;
- Word DOC/DOCX;
- Excel XLS/XLSX.

Ogni allegato puo' pesare al massimo 25 MiB.

## Foto lavoro finito

Dal dettaglio commessa e' possibile aggiungere foto opzionali del lavoro finito.
Queste foto restano nello storico media della commessa, sono mostrate in una sezione separata e non sono richieste per la presa in carico.

## Campi commessa

Campi principali:

- numero commessa;
- cliente;
- titolo;
- categoria o percorso categoria;
- stato;
- priorita';
- operatore assegnato;
- creato da;
- descrizione;
- note interne;
- esito diagnosi `REPAIRABLE` o `NOT_REPAIRABLE`;
- importo preventivo, obbligatorio e positivo solo per esito riparabile, e importo finale;
- date creazione, aggiornamento e chiusura;
- campi di cancellazione logica.

Non sono implementati campi personalizzati configurabili.

## Stati commessa

Stati interni:

```text
OPEN
IN_PROGRESS
DIAGNOSIS
RESULT
REPAIRING
READY_FOR_PICKUP
CLOSED
```

Il workspace riprende dallo stato corrente e mostra separatamente stato corrente e prossima azione. La chiusura richiede sempre motivo e nota manuale.

Mappa operativa:

- `OPEN`: presa in carico o chiusura motivata;
- `IN_PROGRESS`: avvio diagnosi o chiusura motivata;
- `DIAGNOSIS`: registrazione obbligatoria di sintesi ed esito diagnosi;
- `RESULT` con `REPAIRABLE`: preventivo positivo obbligatorio, attesa accettazione o rifiuto cliente;
- accettazione preventivo: feedback `ACCEPT` e passaggio a `REPAIRING`;
- rifiuto preventivo: feedback `REJECT`, nota obbligatoria e chiusura `CUSTOMER_DECLINED`;
- `RESULT` con `NOT_REPAIRABLE`: nessun importo, avviso cliente e passaggio a `READY_FOR_PICKUP`;
- `REPAIRING`: pronta al ritiro o chiusura motivata;
- `READY_FOR_PICKUP`: chiusura motivata;
- `CLOSED`: nessuna azione operativa.

## Timeline

Ogni azione importante crea un evento timeline, tra cui:

- commessa creata;
- cliente creato;
- media aggiunto;
- firma acquisita;
- stato cambiato;
- WhatsApp preparato;
- WhatsApp confermato come inviato;
- feedback cliente registrato;
- commessa riassegnata;
- nota aggiunta;
- commessa chiusa;
- commessa eliminata logicamente;
- reset dati ed export nei log audit.

## Pulizia admin

L'admin puo':

- archiviare in batch le commesse chiuse come rifiutate, annullate o non riparabili digitando `ARCHIVIA`;
- cancellare definitivamente le commesse gia' archiviate e i relativi file digitando `CANCELLA`.

## WhatsApp e feedback

La sola modalita implementata e' `MANUAL_WHATSAPP`.

Flusso:

```text
Operatore prepara messaggio
Anteprima modificabile in app
Operatore apre WhatsApp
Operatore invia manualmente
Operatore torna in app e conferma "Inviato"
Timeline e message log vengono aggiornati
```

Non sono implementati backend, WhatsApp Business Platform, webhook o conferme automatiche.

Il feedback cliente automatico non e' implementato. La risposta viene registrata manualmente e aggiorna timeline e ultimo message log disponibile. Il ramo non riparabile usa il normale flusso pronto ritiro e chiusura motivata.

## Backup e limiti

Export implementati:

- configurazione JSON;
- database Room;
- ZIP completo con database, configurazione e file privati rilevanti;
- PDF riepilogo della singola commessa con timeline completa.

Ripristino backup:

- non supportato; lo ZIP completo e' solo export locale.

Limiti noti:

- export non cifrati dall'app;
- nessuna API WhatsApp;
- nessun backend;
- nessuna CI tracciata nel repository.
