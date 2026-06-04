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
- modificare categorie;
- modificare template privacy, condizioni e WhatsApp;
- impostare la modalita WhatsApp manuale;
- esportare configurazione, database, ZIP completo e PDF commessa;
- eseguire reset totale dati con conferma testuale `RESET`.

### Operatore

Funzioni implementate:

- creare clienti;
- creare commesse;
- consultare le commesse;
- gestire le commesse assegnate;
- riassegnare una propria commessa;
- aggiungere note, foto, allegati e firme alle proprie commesse;
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

- Da diagnosticare;
- Attesa cliente;
- Da lavorare;
- Pronte / chiusura.

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

La creazione rapida richiede:

- cliente esistente o nuovo cliente valido;
- problema/sintesi obbligatoria;
- categoria attiva obbligatoria;
- almeno una foto o allegato;
- privacy, condizioni e firma cliente.

Alla conferma, `CreateIntakeOrderUseCase` crea cliente eventuale, commessa, firma e media dentro una transazione Room. Se firma o media falliscono, la commessa non viene confermata nel database e i file gia' scritti vengono ripuliti quando possibile.

La presa in carico e' bloccata se non esiste almeno una categoria attiva.

Gli allegati importabili sono:

- immagini JPEG, PNG, WebP;
- PDF;
- TXT;
- Word DOC/DOCX;
- Excel XLS/XLSX.

Ogni allegato puo' pesare al massimo 25 MiB.

## Campi commessa

Campi principali:

- numero commessa;
- cliente;
- titolo;
- categoria;
- stato;
- priorita';
- operatore assegnato;
- creato da;
- descrizione;
- note interne;
- importo preventivo/finale;
- date creazione, aggiornamento e chiusura;
- campi di cancellazione logica.

Non sono implementati campi personalizzati configurabili.

## Stati commessa

Stati interni:

```text
NEW
DIAGNOSIS
WAITING_CUSTOMER
WORKING
WAITING_PARTS
READY_FOR_PICKUP
CLOSED
```

Il workspace riprende dallo stato corrente e mostra solo le azioni consentite dalla policy di stato. La chiusura richiede conferma e motivo obbligatorio.

Mappa operativa:

- `NEW`: avvio diagnosi o chiusura motivata;
- `DIAGNOSIS`: attesa cliente, lavorazione, attesa ricambi o chiusura motivata;
- `WAITING_CUSTOMER`: lavorazione o chiusura motivata;
- `WORKING`: attesa cliente, attesa ricambi, pronta al ritiro o chiusura motivata;
- `WAITING_PARTS`: lavorazione, attesa cliente, pronta al ritiro o chiusura motivata;
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

Il feedback cliente automatico non e' implementato. Nel rilascio corrente l'app registra manualmente la risposta cliente: l'accettazione porta la commessa a `WORKING`, il rifiuto chiude la commessa in `CLOSED` con motivo `CUSTOMER_DECLINED`.

## Export e limiti

Export implementati:

- configurazione JSON;
- database Room;
- ZIP completo con database, configurazione e file privati rilevanti;
- PDF riepilogo della singola commessa.

Limiti noti:

- nessun ripristino/import backup;
- export non cifrati dall'app;
- nessuna API WhatsApp;
- nessun backend;
- nessuna CI tracciata nel repository.
