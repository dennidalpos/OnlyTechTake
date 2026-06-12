# 07 - Licenza Demo/Full e manuale d'uso

## Obiettivo commerciale

OnlyTechTake Repair App viene monetizzata, nella prima fase, come app Android standalone per un singolo dispositivo.

Non sono previsti, in questa fase:

- backend;
- cloud proprietario;
- sincronizzazione multi-device;
- account online;
- abbonamenti;
- API WhatsApp;
- pubblicita' in-app.

Il modello iniziale e' semplice:

```text
Versione Demo -> prova limitata
Versione Full -> licenza completa per 1 device Android
```

La vendita e' pensata per negozi di riparazione, tecnici freelance e piccoli laboratori che vogliono gestire accettazione, diagnosi, preventivo, WhatsApp manuale, PDF e storico commesse da un solo telefono o tablet Android.

---

# 1. Modello licenza

## 1.1 Versione Demo

La versione Demo serve per far provare il flusso reale dell'app senza concedere tutte le funzioni definitive.

### Funzioni abilitate in Demo

- Primo setup admin.
- Login admin e operatore.
- Creazione clienti.
- Creazione commesse.
- Foto e allegati.
- Firma cliente.
- Diagnosi.
- Preventivo.
- Gestione stati commessa.
- Timeline.
- Preparazione messaggio WhatsApp manuale.
- Conferma manuale invio WhatsApp.
- Generazione PDF riepilogo commessa con watermark Demo.
- Configurazione categorie e template base.

### Limiti consigliati in Demo

La Demo deve essere usabile, ma non deve sostituire la Full.

Limiti consigliati:

- massimo 10 commesse totali;
- massimo 2 operatori oltre all'admin;
- PDF con dicitura `VERSIONE DEMO`;
- export ZIP completo disabilitato;
- ripristino ZIP disabilitato;
- export database disabilitato;
- export XLSX disabilitato;
- nessuna rimozione del watermark;
- nessuna licenza trasferibile;
- nessun supporto commerciale prioritario.

### Messaggio UI consigliato quando una funzione e' bloccata

```text
Funzione disponibile nella versione Full.
La Demo permette di provare il flusso operativo principale, ma backup, ripristino ed export avanzati sono riservati alla licenza completa.
```

### Messaggio UI consigliato al raggiungimento limite commesse

```text
Limite Demo raggiunto.
Hai creato il numero massimo di commesse disponibili nella versione Demo.
Per continuare a usare l'app senza limiti, attiva la versione Full.
```

---

## 1.2 Versione Full

La versione Full e' la licenza commerciale completa per un solo dispositivo Android.

### Funzioni abilitate in Full

- Commesse illimitate.
- Clienti illimitati.
- Operatori gestibili dall'admin.
- Categorie multilivello.
- Template privacy, condizioni e WhatsApp modificabili.
- PDF senza watermark Demo.
- Export configurazione.
- Export database.
- Export XLSX commesse.
- Export ZIP completo.
- Ripristino ZIP completo.
- Pulizia archivio.
- Reset totale dati.
- Uso offline completo.

### Vincolo 1 device

La licenza Full e' valida per un solo dispositivo Android.

Regola commerciale:

```text
1 licenza = 1 dispositivo Android = 1 negozio/laboratorio operativo
```

Trasferimento licenza:

- consentito solo manualmente dal venditore;
- consigliato solo in caso di cambio telefono/tablet, guasto o sostituzione device;
- non deve diventare una sincronizzazione multi-device.

---

# 2. Strategia tecnica semplice per Demo e Full

Per la prima fase, la soluzione piu' pragmatica e' usare due varianti build:

```text
OnlyTechTake Demo
OnlyTechTake Full
```

## 2.1 Variante Demo

Possibili impostazioni:

```kotlin
IS_DEMO = true
MAX_ORDERS = 10
BACKUP_ENABLED = false
RESTORE_ENABLED = false
DATABASE_EXPORT_ENABLED = false
XLSX_EXPORT_ENABLED = false
PDF_WATERMARK = true
```

## 2.2 Variante Full

Possibili impostazioni:

```kotlin
IS_DEMO = false
MAX_ORDERS = null
BACKUP_ENABLED = true
RESTORE_ENABLED = true
DATABASE_EXPORT_ENABLED = true
XLSX_EXPORT_ENABLED = true
PDF_WATERMARK = false
```

## 2.3 Package name consigliati

Per evitare conflitti tra APK Demo e Full:

```text
com.onlytechtake.repairapp.demo
com.onlytechtake.repairapp
```

La Full deve mantenere il package commerciale principale.

## 2.4 Nome app consigliato

```text
OnlyTechTake Demo
OnlyTechTake
```

---

# 3. Prezzo consigliato

Per la prima vendita reale, mantenere un prezzo accessibile.

Prezzi possibili:

```text
Beta commerciale: 29 EUR
Prezzo standard iniziale: 39 EUR
Prezzo standard dopo feedback reali: 49 EUR
```

La Full deve essere presentata come licenza una tantum per 1 device, non come abbonamento.

Formula commerciale consigliata:

```text
OnlyTechTake Full - licenza permanente per 1 dispositivo Android.
Funziona offline. Nessun canone mensile. Backup e ripristino inclusi.
```

---

# 4. Manuale d'uso

## 4.1 Primo avvio

Al primo avvio l'app richiede la creazione dell'admin.

Procedura:

1. Aprire l'app.
2. Inserire nome admin.
3. Inserire username.
4. Inserire password.
5. Confermare password.
6. Toccare il pulsante di creazione admin.
7. Accedere alla dashboard.

Nota operativa:

- conservare username e password admin;
- l'admin puo' creare operatori;
- l'admin puo' resettare password operatori;
- se si perdono le credenziali admin, puo' essere necessario un reset dati.

---

## 4.2 Login

Dopo il primo avvio l'app mostra la schermata di login.

Procedura:

1. Inserire username.
2. Inserire password.
3. Toccare Accedi.

Sicurezza:

- dopo piu' tentativi errati l'utente puo' essere bloccato temporaneamente;
- la sessione scade dopo inattivita';
- il logout manuale e' disponibile dalla barra superiore.

---

## 4.3 Dashboard

La dashboard contiene le sezioni operative principali:

```text
Home
Nuova
Clienti
Commesse / Commessa
Config
Admin
```

La Home mostra le code operative:

- commesse aperte;
- commesse in lavorazione o diagnosi;
- commesse con risultato, preventivo o riparazione;
- commesse pronte al ritiro.

Uso consigliato:

- partire sempre dalla Home per vedere lo stato del laboratorio;
- usare Nuova per registrare una nuova accettazione;
- usare Commesse per cercare e continuare lavorazioni esistenti.

---

## 4.4 Creazione cliente

Il cliente puo' essere:

```text
Privato
Azienda
```

### Cliente privato

Campi consigliati:

- nome;
- cognome;
- telefono;
- email, se disponibile;
- indirizzo, se utile;
- note.

### Cliente azienda

Campi consigliati:

- ragione sociale;
- partita IVA;
- telefono;
- email;
- referente, se utile;
- indirizzo;
- note.

Regola pratica:

- per creare una commessa serve almeno un contatto valido: telefono o email.

---

## 4.5 Nuova commessa

La nuova commessa segue un flusso guidato.

Procedura:

1. Aprire la tab Nuova.
2. Selezionare un cliente esistente oppure crearne uno nuovo.
3. Inserire la descrizione del problema.
4. Selezionare la categoria.
5. Aggiungere almeno una foto o un allegato.
6. Mostrare privacy e condizioni al cliente.
7. Raccogliere la firma cliente.
8. Confermare la creazione della commessa.

Consiglio operativo:

- fotografare sempre il dispositivo o l'oggetto in ingresso;
- includere eventuali graffi, accessori consegnati, seriali o condizioni iniziali;
- scrivere una descrizione breve ma chiara del problema dichiarato dal cliente.

---

## 4.6 Allegati e foto

L'app puo' gestire foto e allegati collegati alla commessa.

Usi tipici:

- foto dispositivo in ingresso;
- foto danni;
- foto seriale;
- PDF ricevute o documenti;
- immagini del lavoro finito;
- allegati tecnici.

Regola consigliata:

```text
Ogni commessa deve avere almeno una prova visiva o documentale al momento dell'accettazione.
```

---

## 4.7 Diagnosi

Quando la commessa entra in diagnosi, l'operatore deve registrare un risultato.

Esiti disponibili:

```text
Riparabile
Non riparabile
```

### Caso riparabile

Inserire:

- sintesi diagnosi;
- importo preventivo;
- eventuali note interne;
- messaggio WhatsApp al cliente.

### Caso non riparabile

Inserire:

- sintesi diagnosi;
- motivazione;
- eventuali note;
- messaggio WhatsApp per avviso cliente;
- passaggio a pronto ritiro.

---

## 4.8 Preventivo

Se la commessa e' riparabile, il preventivo deve avere un importo positivo.

Flusso consigliato:

1. Registrare diagnosi.
2. Inserire importo preventivo.
3. Preparare messaggio WhatsApp.
4. Aprire WhatsApp.
5. Inviare manualmente al cliente.
6. Tornare in app.
7. Confermare invio.
8. Registrare risposta cliente.

Risposte cliente:

```text
Accetta
Rifiuta
Da richiamare / in attesa
```

---

## 4.9 WhatsApp manuale

OnlyTechTake non invia automaticamente messaggi WhatsApp.

Flusso:

1. L'operatore prepara il testo in app.
2. Il testo puo' essere controllato o modificato.
3. L'app apre WhatsApp tramite link/manual flow.
4. L'operatore invia il messaggio manualmente.
5. L'operatore torna in app.
6. L'operatore conferma che il messaggio e' stato inviato.

Vantaggio:

- nessun backend;
- nessuna API WhatsApp;
- massimo controllo umano;
- minori complessita' legali e tecniche.

---

## 4.10 Riparazione

Quando il cliente accetta il preventivo, la commessa passa in riparazione.

Azioni consigliate:

- aggiungere note interne sul lavoro svolto;
- aggiungere foto del lavoro finito;
- aggiornare la timeline;
- indicare quando la commessa e' pronta al ritiro.

---

## 4.11 Pronto ritiro

Quando il lavoro e' completato o quando il dispositivo non e' riparabile, la commessa puo' passare a pronto ritiro.

Procedura:

1. Aprire la commessa.
2. Verificare stato e note.
3. Preparare eventuale messaggio WhatsApp.
4. Avvisare il cliente.
5. Confermare invio manuale.
6. Attendere ritiro.

---

## 4.12 Chiusura commessa

Alla consegna o alla chiusura amministrativa, la commessa viene chiusa.

La chiusura deve includere:

- motivo;
- nota manuale;
- eventuale importo finale;
- PDF riepilogo, se richiesto.

Motivi tipici:

- riparazione completata;
- cliente ha rifiutato;
- oggetto non riparabile;
- annullata;
- altro motivo documentato.

---

## 4.13 PDF riepilogo

Il PDF riepiloga la commessa.

Contiene tipicamente:

- numero commessa;
- dati cliente;
- categoria;
- descrizione problema;
- stato;
- diagnosi;
- preventivo/importi;
- timeline;
- note essenziali.

In Demo il PDF deve contenere watermark o dicitura `VERSIONE DEMO`.

In Full il PDF viene generato senza watermark Demo.

---

## 4.14 Backup e ripristino - solo Full

Le funzioni backup sono riservate alla Full.

### Backup ZIP

Procedura:

1. Accedere come admin.
2. Aprire Config/Admin.
3. Selezionare export ZIP completo.
4. Scegliere destinazione dal selettore Android.
5. Salvare il file in una posizione sicura.

Destinazioni possibili:

- memoria dispositivo;
- Drive, se presente nel selettore documenti;
- OneDrive, se presente;
- Dropbox, se presente;
- chiavetta USB, se supportata dal device.

### Ripristino ZIP

Procedura:

1. Accedere come admin.
2. Aprire Config/Admin.
3. Selezionare ripristino ZIP.
4. Scegliere il file ZIP precedentemente esportato.
5. Confermare il ripristino.
6. Effettuare nuovamente il login.

Avvertenza:

```text
Il ripristino sostituisce i dati locali dell'app. Usare solo backup verificati.
```

---

## 4.15 Export XLSX - solo Full

L'export XLSX permette di ottenere un file tabellare delle commesse.

Uso consigliato:

- riepiloghi amministrativi;
- controllo storico;
- archiviazione esterna;
- analisi lavori eseguiti.

La Demo non deve permettere export XLSX.

---

## 4.16 Gestione utenti

L'admin puo' creare e gestire operatori.

Azioni admin:

- creare operatore;
- attivare/disattivare operatore;
- resettare password;
- controllare accessi operativi.

Regola pratica:

- ogni persona deve usare il proprio utente;
- evitare account condivisi;
- l'admin deve essere usato solo per configurazione e controllo.

---

## 4.17 Categorie

Le categorie servono a organizzare le commesse.

Esempi:

```text
Smartphone > Apple > iPhone
Smartphone > Samsung > Galaxy
Computer > Notebook
Console > PlayStation
Tablet > Android
Altro
```

Regola consigliata:

- mantenere categorie semplici;
- non creare troppi livelli all'inizio;
- usare categorie operative, non descrizioni troppo generiche.

---

## 4.18 Template

L'admin puo' modificare template di:

- privacy;
- condizioni;
- messaggi WhatsApp.

Consiglio:

- mantenere testi chiari e brevi;
- includere nome negozio;
- includere numero commessa;
- includere stato lavorazione;
- evitare promesse automatiche su tempi o garanzie se non definite.

---

# 5. Manuale rapido per il negozio

## Flusso quotidiano consigliato

```text
1. Login operatore
2. Nuova commessa
3. Cliente
4. Problema
5. Foto/allegati
6. Firma
7. Diagnosi
8. Preventivo WhatsApp
9. Accettazione/rifiuto
10. Riparazione o pronto ritiro
11. Chiusura
12. PDF
13. Backup periodico in Full
```

## Backup consigliato

Per la versione Full:

```text
Eseguire almeno 1 backup ZIP a settimana.
Conservare il file fuori dal dispositivo principale.
```

Per negozi con molte commesse:

```text
Eseguire backup ZIP ogni giorno a fine giornata.
```

---

# 6. Checklist prima della consegna commerciale

Prima di vendere una licenza Full verificare:

- build release firmata;
- installazione pulita su device Android reale;
- setup admin;
- creazione operatore;
- creazione cliente;
- nuova commessa con foto;
- firma cliente;
- diagnosi riparabile;
- preventivo;
- apertura WhatsApp;
- conferma invio;
- chiusura commessa;
- PDF finale;
- export ZIP;
- ripristino ZIP su installazione pulita;
- export XLSX;
- assenza watermark in Full;
- presenza watermark in Demo;
- blocco backup/export nella Demo;
- blocco creazione commesse oltre il limite Demo.

---

# 7. Test da implementare o verificare

## Demo

- Creare 10 commesse: consentito.
- Tentare creazione undicesima commessa: bloccata.
- Tentare export ZIP: bloccato.
- Tentare ripristino ZIP: bloccato.
- Tentare export database: bloccato.
- Tentare export XLSX: bloccato.
- Generare PDF: watermark Demo presente.

## Full

- Creare piu' di 10 commesse: consentito.
- Export ZIP: consentito.
- Ripristino ZIP: consentito.
- Export database: consentito.
- Export XLSX: consentito.
- Generare PDF: nessun watermark Demo.

---

# 8. Posizionamento marketing

Descrizione breve:

```text
Gestionale Android offline per laboratori di riparazione. Gestisci accettazione, foto, firma cliente, diagnosi, preventivo, WhatsApp manuale, PDF e backup da un solo dispositivo.
```

Descrizione commerciale:

```text
OnlyTechTake e' un'app Android standalone per negozi di riparazione smartphone, PC, console e dispositivi elettronici. Permette di registrare commesse al banco, allegare foto, raccogliere firma cliente, gestire diagnosi e preventivi, preparare messaggi WhatsApp e generare PDF riepilogativi. La versione Full include backup, ripristino ed export avanzati senza canone mensile.
```

Punti forti:

- funziona offline;
- nessun canone mensile;
- dati salvati sul dispositivo;
- flusso pensato per il banco;
- WhatsApp manuale controllato dall'operatore;
- PDF riepilogo commessa;
- backup locale nella Full;
- adatta a piccoli negozi e tecnici indipendenti.

---

# 9. Roadmap dopo la prima vendita

Non introdurre subito cloud o abbonamenti. Prima validare la Full standalone.

Roadmap consigliata:

1. Demo stabile.
2. Full 1 device.
3. Manuale PDF con screenshot.
4. Video demo brevi.
5. Feedback da 5 negozi reali.
6. Migliorie UI operative.
7. Eventuale licenza multi-device futura.
8. Eventuale cloud opzionale futuro.
9. Eventuale abbonamento solo se esiste valore ricorrente reale.

---

# 10. Decisione prodotto attuale

Decisione valida per la fase corrente:

```text
OnlyTechTake resta una app standalone Android per 1 device.
La Demo disattiva backup, ripristino ed export avanzati.
La Full abilita tutte le funzioni locali.
La monetizzazione iniziale e' una licenza una tantum, non un abbonamento.
```
