# 09 - Backend Agent Tracker

## Scopo del file

Questo file va consegnato all'agente AI incaricato di progettare o generare il backend cloud di **OnlyTechTake Repair App**.

Il backend deve nascere come estensione compatibile dell'app Android attuale, non come riscrittura del prodotto. L'app installata deve continuare a funzionare in modalità locale subito dopo l'installazione. L'aggancio al cloud deve essere opzionale, successivo e controllato dall'admin.

Obiettivo operativo:

```text
Installazione app
↓
Funzionamento locale completo
↓
Eventuale attivazione cloud da parte dell'admin
↓
Sincronizzazione/centralizzazione dati
↓
Uso multiutente simultaneo con backend come fonte autoritativa
```

---

## Regola principale

L'app locale esistente è il riferimento funzionale.

Il backend deve replicare e centralizzare la logica già prevista dall'app:

- clienti;
- utenti e ruoli;
- categorie;
- commesse;
- workflow operativo;
- media;
- firme;
- timeline;
- template;
- messaggi WhatsApp;
- feedback cliente;
- PDF/documenti;
- audit log;
- export/backup.

Non introdurre un gestionale diverso. Non trasformare la commessa in un modello verticale per un solo settore. La commessa deve restare generica.

---

## Contesto funzionale da rispettare

L'app gestisce commesse locali per oggetti portati in negozio da clienti privati o aziende. È pensata per uso al banco, con operatori e admin.

La commessa è generica: i dati specifici della lavorazione devono restare in:

- categoria;
- titolo;
- descrizione;
- note interne;
- media;
- firme;
- timeline;
- messaggi.

Non aggiungere colonne o campi specializzati come:

```text
imei
marca
modello
taglia
colore
seriale obbligatorio
tipo dispositivo rigido
```

Se servono informazioni specifiche, devono essere salvate in descrizione, note, media o eventualmente in metadati generici futuri, ma solo dopo approvazione del proprietario.

---

## Modalità operative previste

### 1. LOCAL_ONLY

Modalità predefinita dopo installazione.

Caratteristiche:

- nessun account cloud richiesto;
- nessun backend obbligatorio;
- database locale Android;
- file locali in spazio privato app;
- export e backup locali;
- WhatsApp manuale;
- funzionamento completo al banco su singolo dispositivo.

Questa modalità non deve essere rotta dal backend.

### 2. CLOUD_LINKED

Modalità opzionale attivata dall'admin.

Caratteristiche:

- l'app viene associata a un tenant/cloud workspace;
- il backend diventa fonte autoritativa per i dati condivisi;
- Room/DB locale diventa cache, staging e supporto offline limitato;
- più utenti possono operare simultaneamente;
- il backend valida stati, permessi, transizioni, audit e conflitti;
- media, firme, allegati e PDF passano a object storage cloud;
- WhatsApp Business può essere gestito solo tramite backend.

### 3. CLOUD_SYNC_PENDING

Stato transitorio durante il primo collegamento o una sincronizzazione parziale.

Serve per evitare che l'app sembri collegata quando i dati non sono ancora coerenti.

Durante questo stato:

- bloccare azioni distruttive;
- mostrare avanzamento o stato errore;
- consentire retry sicuro;
- non duplicare commesse;
- non perdere file locali.

---

## Principio cloud-first senza rompere local-first

Il progetto deve restare local-first per installazione e primo utilizzo, ma cloud-ready per evoluzione.

Regola pratica:

```text
LOCAL_ONLY:
- il database locale è fonte operativa.

CLOUD_LINKED:
- il backend è fonte autoritativa.
- il database locale è cache/outbox/staging.
```

Non implementare una sincronizzazione fragile basata su update liberi da entrambi i lati. Le azioni operative devono diventare comandi validati.

---

## Backend target consigliato

Soluzione preferita:

```text
API backend
PostgreSQL managed
Object storage S3-compatible / GCS / R2
Queue/job worker
Webhook endpoint
JWT/sessioni server-side
```

Alternative accettabili:

- Supabase, se usato con funzioni server-side per azioni critiche;
- Firebase solo se viene motivato e se si gestiscono correttamente conflitti, audit e modello workflow.

Per questa app gestionale è preferito un modello relazionale con PostgreSQL.

---

## Confini dei componenti

### Android app

Responsabilità:

- UI operativa al banco;
- acquisizione foto/firma;
- gestione modalità locale;
- cache locale;
- staging allegati;
- preparazione comandi;
- visualizzazione stato, timeline, messaggi e PDF;
- fallback WhatsApp manuale.

Non deve contenere:

- token WhatsApp Business;
- credenziali cloud;
- segreti backend;
- regole autorizzative definitive per modalità cloud;
- logica server-side duplicata come unica fonte di verità.

### Backend

Responsabilità:

- autenticazione cloud;
- tenant/workspace;
- ruoli e permessi;
- validazione comandi;
- state machine commessa;
- gestione conflitti multiutente;
- audit log;
- generazione o registrazione documenti;
- gestione media metadata;
- integrazione WhatsApp Business;
- webhook WhatsApp;
- export centralizzati;
- backup cloud;
- API per sync/cache mobile.

### Object storage

Responsabilità:

- foto;
- allegati;
- firme;
- PDF;
- eventuali export generati.

Nel database salvare solo metadati e chiavi storage.

---

## Entità backend minime

Il backend deve avere entità concettualmente compatibili con quelle locali.

```text
tenants
users
customers
order_categories
repair_orders
order_media
signatures
documents
timeline_events
message_templates
message_logs
audit_logs
sync_devices
sync_outbox / command_log
```

Campi trasversali consigliati:

```text
id
tenantId
createdAt
createdByUserId
updatedAt
updatedByUserId
version
deletedAt
deletedByUserId
```

Usare UUID/ULID stabili lato backend. Per import da locale, mantenere una mappa tra ID locale e ID cloud se necessario.

---

## Workflow commessa da rispettare

Workflow attuale:

```text
OPEN
IN_PROGRESS
DIAGNOSIS
RESULT
REPAIRING
READY_FOR_PICKUP
CLOSED
```

Il backend deve validare le transizioni.

Esempi di regole:

```text
OPEN -> IN_PROGRESS
consentito se commessa assegnata o presa in carico.

IN_PROGRESS -> DIAGNOSIS
consentito se operatore autorizzato.

DIAGNOSIS -> RESULT
consentito solo con sintesi diagnosi ed esito diagnosi.

RESULT + REPAIRABLE
richiede preventivo positivo.

RESULT + NOT_REPAIRABLE
non deve avere preventivo positivo.

REPAIRABLE + cliente accetta
passa a REPAIRING.

REPAIRABLE + cliente rifiuta
passa a CLOSED con motivo CUSTOMER_DECLINED.

NOT_REPAIRABLE
può passare a READY_FOR_PICKUP o CLOSED secondo logica approvata.

READY_FOR_PICKUP -> CLOSED
richiede motivo e nota.
```

Non aggiungere stati nuovi senza prima documentare:

- problema risolto;
- impatto UX;
- impatto tecnico;
- migrazione richiesta;
- compatibilità con locale.

---

## Comandi backend consigliati

In modalità cloud, l'app non deve aggiornare direttamente i record principali. Deve inviare comandi.

Comandi minimi:

```text
CreateCustomer
UpdateCustomer
CreateOrder
UpdateOrderDraftData
AssignOrder
ReassignOrder
AddMedia
RemoveStagedMedia
CaptureSignature
StartWork
StartDiagnosis
SaveDiagnosisResult
SendRepairEstimate
RegisterCustomerAccept
RegisterCustomerReject
MarkRepairing
MarkReadyForPickup
CloseOrder
ArchiveOrder
DeleteArchivedOrder
PrepareWhatsAppMessage
SendWhatsAppBusinessMessage
ConfirmManualWhatsAppSent
RegisterCustomerReply
GenerateStatusPdf
ShareStatusPdfEvent
ExportData
RestoreData
```

Ogni comando deve produrre, quando rilevante:

```text
- aggiornamento stato/entity;
- timeline event;
- audit log;
- message log;
- incremento version;
- risposta API con stato aggiornato.
```

---

## Concorrenza multiutente

Usare optimistic locking.

Ogni comando che modifica una commessa deve includere:

```text
orderId
expectedVersion
idempotencyKey
payload
```

Se la versione non coincide:

```text
HTTP 409 Conflict
```

Risposta suggerita:

```json
{
  "error": "ORDER_VERSION_CONFLICT",
  "message": "La commessa è stata aggiornata da un altro utente.",
  "currentOrder": {}
}
```

L'app deve ricaricare la commessa e far ripetere l'azione.

Non usare lock permanenti salvo casi specifici. Al banco un lock dimenticato può bloccare il lavoro.

---

## Idempotenza

Le azioni da app mobile possono essere ritentate.

Ogni comando critico deve accettare `idempotencyKey`.

Obiettivo:

- evitare doppia creazione commessa;
- evitare doppio invio messaggio;
- evitare doppia timeline;
- evitare doppia firma;
- evitare doppio upload confermato.

---

## Sincronizzazione iniziale locale -> cloud

Quando l'admin collega l'app al cloud:

1. creare o selezionare tenant;
2. verificare autorizzazione admin;
3. eseguire snapshot locale;
4. caricare metadati clienti, categorie, utenti compatibili, commesse, timeline, template, messaggi;
5. caricare file tramite upload controllato;
6. creare mapping ID locale/cloud;
7. verificare conteggi e checksum;
8. marcare device come collegato;
9. passare a CLOUD_LINKED solo a import concluso.

Non cancellare dati locali dopo il collegamento. Convertire il locale in cache/staging.

Caso cloud già popolato:

- non fondere automaticamente senza regole;
- richiedere scelta admin: nuovo tenant vuoto, import bloccato o procedura guidata di merge;
- segnalare conflitti esplicitamente.

---

## Offline in modalità cloud

Non implementare subito offline completo multiutente.

Supportato nella prima fase cloud:

```text
- lettura cache recente;
- bozze locali;
- staging foto/allegati;
- retry upload;
- visualizzazione dati già sincronizzati.
```

Da bloccare offline:

```text
- chiusura commessa;
- cambio stato critico;
- accettazione/rifiuto preventivo;
- cancellazione;
- archiviazione;
- invio WhatsApp Business;
- reset dati;
- restore.
```

Motivo: queste azioni richiedono validazione server e possono confliggere con altri utenti.

---

## Media, firme, allegati e PDF

In locale i file restano nello spazio privato app.

In cloud usare object storage.

Flusso consigliato:

```text
Android -> backend: richiesta upload URL
Backend -> Android: signed upload URL + mediaId
Android -> storage: upload file
Android -> backend: conferma upload con checksum/dimensione
Backend: crea record media + timeline event
```

Per firme:

- salvare PNG;
- salvare snapshot privacy e condizioni;
- salvare versione template;
- salvare signerName;
- salvare data/ora;
- evento timeline `SIGNATURE_CAPTURED`.

Per PDF:

- generazione locale consentita in LOCAL_ONLY;
- in CLOUD_LINKED valutare generazione backend per coerenza multiutente;
- registrare documento e timeline `PDF_STATUS_SHARED` quando condiviso.

---

## WhatsApp

### Modalità manuale

Deve restare disponibile.

```text
MANUAL_WHATSAPP
```

Flusso:

1. app prepara messaggio;
2. operatore modifica anteprima;
3. app apre WhatsApp installato;
4. operatore invia manualmente;
5. operatore torna in app;
6. operatore conferma invio;
7. backend/app registra message log e timeline.

### Modalità WhatsApp Business

Deve passare solo dal backend.

```text
BUSINESS_WHATSAPP
```

Requisiti:

- nessun token nell'app Android;
- backend conserva segreti in secret manager/env sicura;
- uso template approvati;
- invio tramite API backend;
- webhook per consegna, lettura, risposta;
- associazione messaggio -> commessa;
- aggiornamento message log;
- aggiornamento timeline;
- gestione errori/fallimenti.

Non implementare WhatsApp Business direttamente nell'app.

---

## Template messaggi

Template da mantenere compatibili:

```text
privacy_breve
condizioni_presa_in_carico
whatsapp_presa_in_carico
whatsapp_aggiornamento
whatsapp_diagnosi
whatsapp_risultato
whatsapp_preventivo_intervento
whatsapp_non_riparabile
whatsapp_riparazione
whatsapp_lavorazione
whatsapp_completamento
whatsapp_pronto_ritiro
whatsapp_stato_avanzamento_pdf
```

Variabili supportate:

```text
{{numero_commessa}}
{{titolo_commessa}}
{{stato_commessa}}
{{cliente}}
{{importo_preventivo}}
{{prossima_azione}}
{{motivo_chiusura}}
{{categoria_percorso}}
```

Il backend deve rifiutare template con variabili non ammesse.

---

## Sicurezza e ruoli

Ruoli minimi:

```text
ADMIN
OPERATOR
```

Admin:

- gestione utenti;
- categorie;
- template;
- export;
- archiviazione/cancellazione;
- reset dati;
- configurazione cloud;
- collegamento device;
- WhatsApp Business settings.

Operatore:

- clienti;
- commesse assegnate;
- note/media/foto/firma;
- workflow operativo consentito;
- messaggi WhatsApp manuali o richieste invio tramite backend;
- consultazione commesse secondo policy.

Il backend deve validare sempre ruolo e tenant.

---

## Audit log

Azioni sensibili da tracciare:

```text
login admin
creazione/modifica utente
reset password
modifica template
modifica privacy/condizioni
export dati
restore dati
archiviazione/cancellazione commessa
cambio assegnazione
cambio stato
reset dati
collegamento cloud
scollegamento device
invio WhatsApp Business
ricezione webhook WhatsApp
fallimento invio messaggio
```

Audit log append-only.

---

## API minime da progettare

Autenticazione:

```text
POST /auth/login
POST /auth/refresh
POST /auth/logout
GET  /auth/me
```

Tenant/device:

```text
POST /tenants
GET  /tenants/current
POST /devices/link
POST /devices/unlink
GET  /devices
```

Clienti:

```text
GET    /customers
POST   /customers
GET    /customers/{id}
PATCH  /customers/{id}
```

Commesse:

```text
GET    /orders
POST   /orders
GET    /orders/{id}
POST   /orders/{id}/commands/{commandName}
GET    /orders/{id}/timeline
GET    /orders/{id}/messages
GET    /orders/{id}/media
```

Media/documenti:

```text
POST /orders/{id}/media/upload-url
POST /orders/{id}/media/{mediaId}/confirm
GET  /orders/{id}/documents
POST /orders/{id}/documents/status-pdf
```

Template:

```text
GET   /templates
PATCH /templates/{code}
```

WhatsApp:

```text
POST /orders/{id}/whatsapp/prepare
POST /orders/{id}/whatsapp/send-business
POST /orders/{id}/whatsapp/confirm-manual-sent
POST /webhooks/whatsapp
```

Sync/cache:

```text
GET  /sync/bootstrap
GET  /sync/changes?since=cursor
POST /sync/import-local-snapshot
```

---

## Tracker funzionale backend

L'agente backend deve mantenere una tabella di avanzamento come questa nel repository backend.

| Area | Stato | Fonte app locale | Backend previsto | Note | Bloccanti |
|---|---:|---|---|---|---|
| Auth admin/operatori | TODO | login locale + ruoli | JWT/sessioni + ruoli tenant |  |  |
| Clienti | TODO | CustomerEntity | customers API | mantenere privato/azienda |  |
| Categorie | TODO | OrderCategoryEntity | order_categories API | albero 0..N |  |
| Commesse | TODO | RepairOrderEntity | repair_orders + commands | state machine server-side |  |
| Timeline | TODO | TimelineEventEntity | append-only timeline_events |  |  |
| Media | TODO | OrderMediaEntity | object storage + metadata | upload signed URL |  |
| Firme | TODO | SignatureEntity | signatures + storage | snapshot privacy/condizioni |  |
| Template | TODO | MessageTemplateEntity | templates API | allowlist variabili |  |
| WhatsApp manuale | TODO | ManualWhatsAppSender | conferma manuale backend | mantenere fallback |  |
| WhatsApp Business | TODO | non presente | backend + webhook | no token Android | backend richiesto |
| PDF | TODO | generazione locale | locale o backend | decidere fase cloud |  |
| Export/backup | TODO | SAF locale | export cloud tenant | compatibilità locale |  |
| Sync iniziale | TODO | backup locale | import snapshot | mapping ID | rischio conflitti |
| Offline cloud | TODO | Room locale | cache/outbox limitata | no azioni critiche offline |  |
| Audit | TODO | AuditLogEntity | audit_logs append-only | azioni sensibili |  |

Stati ammessi:

```text
TODO
ANALYSIS
DESIGNED
IMPLEMENTED
TESTED
BLOCKED
```

---

## Regola modifiche distruttive

Le modifiche distruttive sono ammesse solo come proposta, non come implementazione automatica.

Ogni proposta distruttiva deve indicare:

```text
Problema
Soluzione proposta
Impatto UX
Impatto Android locale
Impatto backend
Impatto dati
Rischio
Compatibilità con LOCAL_ONLY
Compatibilità con CLOUD_LINKED
Richiede cambio schema Android: sì/no
Richiede migrazione backend: sì/no
Decisione owner: approvata/bloccata
```

---

## Freeze schema Android

Direttiva obbligatoria per l'agente.

L'app Android è in fase di sviluppo iniziale e lo schema locale non va modificato senza approvazione esplicita del proprietario.

```text
Room schema version deve restare al valore reale/autorizzato del sorgente corrente.
Stato sorgente corrente: RepairAppDatabase.DATABASE_VERSION = 1.
Export schema tracciato: solo 1.json.
Vietato cambiare schemaVersion senza approvazione esplicita.
Vietato aggiungere migrazioni Room.
Vietato modificare entità Room, DAO con impatto schema o indici persistenti senza autorizzazione esplicita.
Ogni modifica che richiede cambio schema Android deve essere marcata:
RICHIEDE CAMBIO SCHEMA ANDROID - BLOCCATA.
```

Questa regola riguarda l'app Android locale.

Il backend può avere proprie migrazioni database server, ma devono essere versionate e documentate separatamente.

---

## Regola implementazione backend

Prima di scrivere codice backend, l'agente deve produrre:

1. mappa funzionale locale -> backend;
2. modello dati backend;
3. API contract;
4. state machine server-side;
5. strategia tenant/ruoli;
6. strategia media/object storage;
7. strategia WhatsApp manuale + Business;
8. strategia sync LOCAL_ONLY -> CLOUD_LINKED;
9. lista modifiche Android necessarie;
10. lista modifiche Android bloccate dallo schema freeze;
11. tracker aggiornato.

Non implementare codice prima di questi output.

---

## Criteri di accettazione

Il backend è accettabile solo se:

- non rompe il funzionamento locale dell'app;
- non richiede cloud al primo avvio;
- non mette segreti nell'app Android;
- mantiene la commessa generica;
- centralizza state machine e permessi in modalità cloud;
- gestisce concorrenza multiutente;
- usa audit append-only;
- gestisce media fuori dal DB;
- mantiene WhatsApp manuale come fallback;
- implementa WhatsApp Business solo tramite backend;
- documenta ogni modifica che richiede cambio schema Android;
- mantiene un tracker funzionale/logico aggiornato.

---

## Prompt operativo per l'agente backend

Usa questo comando operativo come istruzione iniziale:

```text
Analizza il progetto OnlyTechTake Repair App e progetta il backend cloud mantenendo compatibilità con il funzionamento locale attuale.

L'app appena installata deve funzionare in LOCAL_ONLY senza backend. Solo successivamente l'admin può collegarla a un servizio cloud. In CLOUD_LINKED il backend diventa fonte autoritativa per dati condivisi, workflow, permessi, audit, messaggi e media metadata, mentre Android mantiene cache, staging e supporto locale limitato.

Prima di implementare codice, produci:
- mappatura funzionale app locale -> backend;
- modello dati backend;
- API contract;
- state machine server-side;
- strategia sync locale -> cloud;
- strategia conflitti multiutente;
- strategia media/object storage;
- strategia WhatsApp manuale e WhatsApp Business;
- tracker funzionale/logico;
- elenco modifiche Android richieste;
- elenco modifiche bloccate dal freeze schema Android.

Non modificare lo schema Room Android. Non incrementare schemaVersion. Non creare migrazioni Room. Se serve una modifica incompatibile, marcarla come RICHIEDE CAMBIO SCHEMA ANDROID - BLOCCATA.

Proponi soluzioni semplici, affidabili e scalabili. Evita overengineering, ma non creare scorciatoie che impediscono multiutente, audit, WhatsApp Business o cloud centralizzato.
```
