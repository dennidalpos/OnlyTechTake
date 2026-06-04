# 01 - Guida funzionale

## Obiettivo

Realizzare un'app Android locale per gestire oggetti portati in negozio da clienti privati o aziende per interventi, riparazioni, diagnosi o lavorazioni.

L'app deve essere semplice, usabile al banco e offline.

## Principio principale

Non legare la commessa a un settore specifico.

La commessa deve avere pochi campi base, categoria obbligatoria, media, firma, note e timeline.

Esempi:

- negozio telefonia: IMEI, marca, modello, accessori;
- sartoria: capo, taglia, difetto, misure;
- laboratorio PC: seriale, sistema operativo, alimentatore consegnato;
- assistenza elettrodomestici: marca, modello, codice prodotto;
- officina generica: categoria, descrizione, allegati.

## Ruoli

### Admin

Può:

- creare operatori;
- disattivare operatori;
- vedere tutto;
- modificare tutto;
- eliminare commesse;
- modificare testi privacy;
- modificare testi WhatsApp;
- modificare categorie, template e impostazioni;
- esportare database e configurazione;
- scegliere modalità WhatsApp.

### Operatore

Può:

- creare clienti;
- creare commesse;
- vedere tutte le commesse;
- gestire le proprie commesse;
- spostare una propria commessa ad altro operatore;
- consultare commesse di altri;
- registrare feedback cliente;
- inviare messaggi WhatsApp manuali;
- aggiungere foto/documenti/note alle proprie commesse.

Non può:

- eliminare commesse;
- eliminare clienti;
- creare operatori;
- modificare testi privacy;
- modificare template WhatsApp;
- modificare configurazione generale.

## Primo avvio

Flusso:

```text
Apertura app
↓
Nessun admin presente
↓
Schermata configurazione admin
↓
Nome admin
↓
Username admin
↓
Password admin
↓
Conferma password
↓
Creazione admin
↓
Dashboard
```

Dopo il primo avvio, l'app mostra sempre il login.

## Login

Campi:

- username;
- password.

Funzioni:

- login admin;
- login operatore;
- blocco automatico dopo inattività;
- logout manuale.

## Dashboard

La dashboard deve essere pratica.

Sezioni:

```text
Nuova commessa
Cerca cliente / azienda
Le mie commesse
Tutte le commesse
In attesa cliente
Preventivi da gestire
Pronte al ritiro
Archivio
Impostazioni
```

Per operatori, `Impostazioni` mostra solo preferenze minime.
Per admin, `Impostazioni` mostra categorie, template, WhatsApp, backup e accesso a utenti/reset dati.

## Clienti

Il cliente può essere:

```text
PRIVATO
AZIENDA
```

### Ricerca cliente

La ricerca deve cercare in:

- nome;
- cognome;
- cellulare;
- email;
- ragione sociale;
- P.IVA;
- codice fiscale;
- numero commessa.

### Cliente privato

Campi consigliati:

- nome;
- cognome;
- cellulare;
- email;
- codice fiscale opzionale;
- indirizzo opzionale;
- note.

### Cliente azienda

Campi consigliati:

- ragione sociale;
- P.IVA;
- codice fiscale opzionale;
- referente;
- cellulare;
- email;
- indirizzo;
- note.

## Nuova commessa

La presa in carico deve essere un unico passaggio operativo.

Il modulo contiene:

- dati cliente, scegliendo un cliente esistente oppure inserendone uno nuovo;
- sintesi obbligatoria del problema;
- dettagli problema opzionali;
- almeno una foto o allegato;
- privacy, condizioni e firma cliente;
- pulsante unico `Crea commessa in carico`.

Alla conferma l'app crea la commessa in stato `PRESA_IN_CARICO`, la assegna
all'operatore corrente e apre direttamente il pannello operativo della commessa.
Se firma o media falliscono dopo la creazione dell'ordine, l'app apre comunque
la commessa creata e segnala all'operatore quali elementi completare dal
dettaglio.

Gli allegati importabili sono limitati ai formati operativi comuni: immagini
JPEG/PNG/WebP, PDF, TXT, Word DOC/DOCX ed Excel XLS/XLSX. Ogni allegato puo'
pesare al massimo 25 MiB; formati sconosciuti o eseguibili non sono accettati.

## Campi commessa

La commessa deve avere solo questi campi base obbligatori:

- numero commessa;
- cliente;
- titolo;
- categoria;
- stato;
- operatore assegnato;
- creato da;
- data creazione;
- descrizione libera.

I dati specifici della lavorazione si registrano in descrizione, note interne, foto,
allegati, firme, timeline e messaggi. I campi personalizzati configurabili sono stati
rimossi per mantenere il flusso di presa in carico più prevedibile.

## Stati commessa

Stati base:

```text
PRESA_IN_CARICO
IN_DIAGNOSI
PREVENTIVO_INVIATO
IN_ATTESA_CLIENTE
PREVENTIVO_ACCETTATO
PREVENTIVO_RIFIUTATO
IN_LAVORAZIONE
IN_ATTESA_RICAMBIO
COMPLETATO
NON_COMPLETABILE
PRONTO_AL_RITIRO
CONSEGNATO
CHIUSO
ANNULLATO
```

Il nome visualizzato può essere modificabile dall'admin, ma il codice interno resta stabile.

Il workspace operativo riprende sempre dallo stato corrente della commessa.
L'operatore non deve avanzare in una procedura a passi separati: apre una
commessa, vede lo stato, aggiunge note/foto/firma se necessario e usa le azioni
rapide disponibili per quello stato.

Mappa operativa ispirata a ITIL:

- `PRESA_IN_CARICO`: avvio diagnosi o annullamento;
- `IN_DIAGNOSI`: preventivo, lavorazione diretta o non completabile;
- `PREVENTIVO_INVIATO` / `IN_ATTESA_CLIENTE`: risposta cliente;
- `PREVENTIVO_ACCETTATO`: lavorazione o attesa ricambio;
- `IN_LAVORAZIONE` / `IN_ATTESA_RICAMBIO`: avanzamento o completamento;
- `COMPLETATO`: pronto al ritiro o chiusura;
- `PRONTO_AL_RITIRO`: consegna o chiusura;
- stati finali: nessuna azione operativa.

## Timeline

Ogni azione importante crea un evento timeline.

Eventi:

- commessa creata;
- cliente creato;
- foto aggiunta;
- firma acquisita;
- stato cambiato;
- preventivo inserito;
- WhatsApp preparato;
- WhatsApp confermato come inviato;
- feedback cliente registrato;
- commessa riassegnata;
- nota aggiunta;
- commessa chiusa;
- commessa eliminata da admin.

## WhatsApp

Modalità disponibili:

```text
MANUALE
API_FUTURA
```

Default:

```text
MANUALE
```

### Modalità manuale

L'app genera il testo e apre WhatsApp.
L'operatore invia manualmente.
L'app chiede conferma: "Messaggio inviato?"
Se l'operatore conferma, viene salvato un evento timeline.

### Modalità API

Non supportata in questo rilascio.

Per abilitarla servono:

- backend;
- WhatsApp Business Platform;
- template approvati;
- webhook;
- gestione risposte automatiche.

## Feedback cliente

Ogni messaggio può richiedere un feedback.

Tipi feedback:

```text
LETTO
ACCETTA
RIFIUTA
CHIEDE_INFO
RICHIAMARE
ALTRO
```

Con WhatsApp manuale, il feedback viene registrato dall'operatore.
La registrazione automatica del feedback richiede una modalità API con backend.

## Chiusura commessa

Prima della chiusura:

- stato finale;
- eventuale firma di ritiro;
- note finali;
- eventuale importo finale;
- messaggio finale al cliente.

Solo admin può eliminare.
La cancellazione deve essere logica, non fisica:

```text
deletedAt
deletedByUserId
deleteReason
```
