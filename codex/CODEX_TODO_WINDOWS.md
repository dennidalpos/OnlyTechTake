# CODEX TODO WINDOWS - Prompt autosufficienti in ordine di esecuzione

Usare questo file con Codex su Windows.

Regole:

- Eseguire un prompt alla volta.
- Dopo ogni prompt, compilare e correggere errori.
- Non saltare i test base.
- Mantenere il progetto locale, offline-first, senza backend.
- Non rendere rigidi i campi commessa: usare campi personalizzati configurabili.
- WhatsApp manuale attivo di default; API solo predisposta.
- Solo admin elimina e modifica configurazione.
- Tutti gli operatori vedono tutte le commesse; gli operatori gestiscono le proprie e possono riassegnarle.

---

## 00 - Verifica progetto

PROMPT PER CODEX:

```text
Analizza questo progetto Android su Windows. Verifica struttura Gradle, Kotlin, Compose, SDK, package name, file mancanti e problemi di configurazione. Non modificare ancora la logica applicativa. Se mancano file base per un progetto Android moderno, proponi e applica le modifiche minime per avere una build compilabile. Usa Kotlin, Jetpack Compose, Material 3, Gradle Kotlin DSL e Version Catalog. Alla fine esegui o indica il comando Gradle corretto per verificare la build.
```

---

## 01 - Setup architettura base

PROMPT PER CODEX:

```text
Crea la struttura architetturale dell'app Android locale per gestione commesse riparazioni. Usa pacchetti: core, data, domain, features. Mantieni un solo modulo app. Implementa struttura MVVM semplice con Repository. Aggiungi dipendenze per Room, DataStore, Navigation Compose, CameraX, Material 3, Coroutines e Lifecycle. Usa Version Catalog in gradle/libs.versions.toml. Non implementare ancora UI completa, solo struttura e dipendenze. Assicurati che il progetto compili.
```

---

## 02 - Database Room iniziale

PROMPT PER CODEX:

```text
Implementa database Room locale per app gestione riparazioni. Crea entità, DAO e database per: users, customers, order_categories, repair_orders, custom_field_definitions, custom_field_values, media, signatures, timeline_events, message_templates, message_logs, app_settings, audit_logs. La commessa deve restare generica: solo campi base nella tabella repair_orders, tutti i dati specifici devono andare in custom_field_definitions e custom_field_values. Usa enum Kotlin con TypeConverter Room. Aggiungi database version 1 e DAO minimi per insert/update/search. Assicurati che la build compili.
```

---

## 03 - Primo avvio admin

PROMPT PER CODEX:

```text
Implementa il flusso primo avvio. All'apertura app, se nel database non esiste alcun utente admin attivo, mostra schermata FirstSetupScreen. La schermata deve chiedere nome visualizzato, username, password e conferma password. Crea un utente ADMIN attivo. Non salvare password in chiaro: implementa PasswordHasher con PBKDF2WithHmacSHA256, salt casuale e hash. Dopo creazione admin, naviga alla dashboard. Se admin esiste, mostra login. Aggiungi validazioni base e messaggi errore.
```

---

## 04 - Login e sessione

PROMPT PER CODEX:

```text
Implementa login locale con username e password. Usa UserRepository e PasswordHasher. Crea SessionManager in memoria con utente corrente e ruolo. Dopo login naviga alla dashboard. Aggiungi logout. Implementa blocco UI base: se non c'è sessione, l'app torna al login. Non salvare token o password. Assicurati che admin e operatore abbiano ruoli distinti.
```

---

## 05 - Permessi applicativi

PROMPT PER CODEX:

```text
Implementa PermissionManager applicativo per ruoli ADMIN e OPERATOR. Regole: ADMIN può tutto; OPERATOR vede tutte le commesse, crea clienti, crea commesse, gestisce commesse assegnate a sé, può riassegnare proprie commesse, può registrare feedback cliente, non può eliminare commesse, non può modificare testi privacy/WhatsApp, non può creare utenti, non può esportare/importare backup salvo consultazione se previsto. Usa funzioni Kotlin semplici tipo canDeleteOrder(user), canEditTemplates(user), canManageUsers(user). Integra i controlli nelle schermate dove già disponibili.
```

---

## 06 - Gestione operatori admin

PROMPT PER CODEX:

```text
Crea schermata admin "Gestione operatori". Solo ADMIN può accedere. Funzioni: lista utenti, crea operatore, modifica nome/username, reimposta password, attiva/disattiva operatore. Non permettere di eliminare fisicamente utenti. Impedisci di disattivare l'ultimo admin. Usa Room e UserRepository. Aggiungi audit log per creazione, modifica, disattivazione e reset password.
```

---

## 07 - Clienti privati e aziende

PROMPT PER CODEX:

```text
Implementa gestione clienti. Customer può essere PRIVATO o AZIENDA. Campi privato: nome, cognome, telefono, email, codice fiscale opzionale, indirizzo, note. Campi azienda: ragione sociale, P.IVA, codice fiscale opzionale, referente, telefono, email, indirizzo, note. Crea schermate ricerca cliente e modifica/creazione cliente. La ricerca deve cercare per nome, cognome, telefono, email, ragione sociale, P.IVA, codice fiscale. Tutti gli operatori autenticati possono creare e modificare clienti. Aggiungi validazioni minime.
```

---

## 08 - Categorie e campi personalizzati

PROMPT PER CODEX:

```text
Implementa gestione categorie commessa e campi personalizzati configurabili. Solo ADMIN può modificare categorie e definizioni campo. Ogni campo personalizzato ha: categoria opzionale, codice, etichetta, tipo, obbligatorio, visibile in riepilogo, visibile al cliente, placeholder, opzioni JSON, ordinamento, attivo. Tipi campo: SHORT_TEXT, LONG_TEXT, NUMBER, MONEY, DATE, BOOLEAN, SINGLE_CHOICE, MULTI_CHOICE, PHONE, EMAIL, URL, BARCODE, PHOTO, DOCUMENT, SIGNATURE. Crea schermata admin per lista/creazione/modifica campi. Non collegare campi specifici alla tabella repair_orders.
```

---

## 09 - Creazione commessa generica

PROMPT PER CODEX:

```text
Implementa flusso creazione commessa. Step: seleziona o crea cliente, titolo, categoria, descrizione libera, campi personalizzati della categoria, assegnazione operatore default utente corrente, riepilogo e creazione. Genera numero commessa progressivo formato RIP-YYYY-000001. Salva RepairOrder e CustomFieldValue. Stato iniziale PRESA_IN_CARICO. Crea evento timeline "Commessa creata". Tutti gli operatori possono creare commesse. La commessa deve essere generica e non contenere campi hardcoded di settore.
```

---

## 10 - Lista e dettaglio commesse

PROMPT PER CODEX:

```text
Implementa lista commesse e dettaglio commessa. La lista deve avere filtri: tutte, mie, per stato, per cliente, per operatore assegnato, archivio. Tutti gli operatori vedono tutte le commesse. Nel dettaglio mostra: numero, cliente, titolo, categoria, stato, operatore assegnato, descrizione, campi personalizzati, media, timeline, messaggi, feedback, note interne. Gli operatori possono modificare solo le commesse assegnate a loro o riassegnare le proprie ad altro operatore. Admin può modificare tutto.
```

---

## 11 - Timeline

PROMPT PER CODEX:

```text
Implementa timeline commessa. Ogni evento ha tipo, titolo, descrizione, oldStatus, newStatus, visibleToCustomer, requiresCustomerFeedback, expectedFeedbackType, customerFeedback, customerFeedbackNote, customerFeedbackAt, createdByUserId, createdAt. Aggiungi funzioni per creare evento al cambio stato, riassegnazione, messaggio WhatsApp preparato/inviato, feedback cliente registrato, note aggiunte. Mostra timeline in ordine cronologico nel dettaglio commessa.
```

---

## 12 - Cambio stato e riassegnazione

PROMPT PER CODEX:

```text
Implementa cambio stato commessa. Stati: PRESA_IN_CARICO, IN_DIAGNOSI, PREVENTIVO_INVIATO, IN_ATTESA_CLIENTE, PREVENTIVO_ACCETTATO, PREVENTIVO_RIFIUTATO, IN_LAVORAZIONE, IN_ATTESA_RICAMBIO, COMPLETATO, NON_COMPLETABILE, PRONTO_AL_RITIRO, CONSEGNATO, CHIUSO, ANNULLATO. Ogni cambio stato crea evento timeline. Implementa riassegnazione ad altro operatore con evento timeline. Operatore può riassegnare le proprie commesse; admin può riassegnare tutto.
```

---

## 13 - Foto, documenti e CameraX

PROMPT PER CODEX:

```text
Integra CameraX per aggiungere foto alla commessa. Permetti foto multiple. Salva file in directory privata app files/orders/<orderId>/photos/. Salva metadati in MediaEntity. Aggiungi anche opzione per allegare documento o immagine esistente usando Storage Access Framework. Mostra gallery nel dettaglio commessa. Ogni aggiunta media può creare evento timeline opzionale. Gestisci permesso CAMERA in modo corretto.
```

---

## 14 - Firma privacy

PROMPT PER CODEX:

```text
Implementa schermata firma cliente con canvas Compose. Mostra testo privacy breve, condizioni di presa in carico e checkbox richieste. La firma deve essere salvata come immagine nella directory files/orders/<orderId>/signatures/. Salva SignatureEntity con snapshot dei testi privacy/condizioni, versioni, nome firmatario, data/ora, utente operatore. Aggiungi evento timeline "Firma acquisita". Non sovrascrivere testi firmati se l'admin modifica i template in futuro.
```

---

## 15 - Template privacy e WhatsApp

PROMPT PER CODEX:

```text
Implementa gestione template modificabili da admin. Template: privacy breve, privacy completa, condizioni presa in carico, WhatsApp presa in carico, diagnosi, preventivo, in lavorazione, attesa ricambio, completato, pronto al ritiro, chiusura, generico. Ogni template ha codice, titolo, canale, body, active, version, updatedByUserId, updatedAt. Solo admin può modificare. Ogni modifica incrementa version e crea audit log. Supporta variabili con sintassi {{nome_variabile}}.
```

---

## 16 - WhatsApp manuale default

PROMPT PER CODEX:

```text
Implementa WhatsApp manuale come modalità default. Crea interfaccia WhatsAppSender e implementazione ManualWhatsAppSender. Dal dettaglio commessa, quando si invia aggiornamento, genera messaggio da template, mostra anteprima modificabile, apre WhatsApp con Intent ACTION_VIEW e url wa.me o schema compatibile con numero cliente. Dopo ritorno all'app, chiedi "Messaggio inviato?" con pulsanti sì/no. Se sì, crea MessageLog status CONFIRMED_SENT e evento timeline. Se no, status PREPARED o CANCELLED. Non usare API WhatsApp in questa versione.
```

---

## 17 - Predisposizione WhatsApp API futura

PROMPT PER CODEX:

```text
Predisponi modalità WhatsApp API futura senza attivarla. Aggiungi enum MessageMode MANUAL_WHATSAPP e API_WHATSAPP. Crea ApiWhatsAppSenderStub che restituisce errore controllato "Modalità API non configurata". Aggiungi schermata admin WhatsApp Settings con scelta modalità, ma lascia default MANUAL_WHATSAPP e mostra avviso che API richiede backend. Non salvare token reali in app. Mantieni codice pronto per futura integrazione.
```

---

## 18 - Feedback cliente

PROMPT PER CODEX:

```text
Implementa registrazione feedback cliente su timeline e messaggi. Tipi feedback: NONE, READ, ACCEPT, REJECT, ASK_INFO, CALL_ME, CUSTOM. Nel dettaglio commessa mostra per ogni evento che richiede feedback pulsanti: Letto, Accetta, Rifiuta, Chiede info, Richiamare, Altro. In modalità WhatsApp manuale il feedback viene registrato dall'operatore. Se feedback è ACCEPT su preventivo, porta stato a PREVENTIVO_ACCETTATO; se REJECT, porta stato a PREVENTIVO_RIFIUTATO. Crea evento timeline per feedback registrato.
```

---

## 19 - Eliminazione admin e soft delete

PROMPT PER CODEX:

```text
Implementa eliminazione commessa solo per ADMIN. Non cancellare fisicamente: usa soft delete con deletedAt, deletedByUserId, deleteReason. Nascondi le commesse eliminate dalle liste normali e mostrale solo in archivio admin. Ogni eliminazione crea audit log e timeline event se possibile. Operatore non deve vedere pulsante elimina.
```

---

## 20 - Export configurazione e database

PROMPT PER CODEX:

```text
Implementa sezione admin Backup/Export. Funzioni: export configurazione JSON, export database locale, export completo ZIP con database, config, media, firme e metadata. Usa Storage Access Framework per far scegliere la destinazione file. Crea BackupRepository. Ogni export crea audit log. Non richiedere permessi storage globali. Prepara anche funzione import configurazione ma mettila dietro conferma esplicita.
```

---

## 21 - PDF ricevuta opzionale

PROMPT PER CODEX:

```text
Aggiungi export PDF commessa opzionale, se fattibile senza complicare troppo. Il PDF deve includere numero commessa, cliente, titolo, categoria, stato, campi personalizzati visibili in riepilogo, data, operatore, eventuale firma e condizioni principali. Se la generazione PDF richiede dipendenze complesse, crea invece una schermata riepilogo stampabile e lascia TODO tecnico per PDF.
```

---

## 22 - Rifinitura UI

PROMPT PER CODEX:

```text
Rifinisci UI con Material 3 e Jetpack Compose. Schermate devono essere semplici, utilizzabili su smartphone e tablet. Usa componenti grandi per uso al banco. Aggiungi ricerca, filtri, empty states, loading states, error states. Evita animazioni inutili. La dashboard deve avere azioni principali: Nuova commessa, Cerca cliente/azienda, Le mie commesse, Tutte le commesse, In attesa cliente, Pronte al ritiro, Impostazioni.
```

---

## 23 - Test base

PROMPT PER CODEX:

```text
Aggiungi test unitari base per PasswordHasher, TemplateRenderer, PermissionManager, generazione numero commessa e CustomField validation. Aggiungi test DAO principali se configurato Room in-memory. Assicurati che i test siano eseguibili da Gradle. Correggi gli errori senza cambiare requisiti funzionali.
```

---

## 24 - Script build installer Android

PROMPT PER CODEX:

```text
Aggiungi cartella scripts/windows con due script PowerShell: check_android_env.ps1 e build_android_installer.ps1. Lo script check deve verificare Java, gradlew.bat, Android SDK e adb se disponibile. Lo script build deve supportare -BuildType Debug/Release e -OutputType Apk/Aab, pulire opzionalmente, eseguire Gradle wrapper, mostrare percorso file generato e permettere installazione via adb opzionale. Non inserire password o keystore nel repository.
```

---

## 25 - Verifica finale

PROMPT PER CODEX:

```text
Esegui revisione finale del progetto. Verifica che i requisiti siano rispettati: app locale, primo avvio admin, admin crea operatori, operatori vedono tutte le commesse, operatori gestiscono proprie e riassegnano, solo admin elimina, campi commessa generici con custom fields, testi privacy/WhatsApp modificabili solo da admin, WhatsApp manuale default, API solo predisposta, feedback cliente manuale, export config e database locale. Correggi incoerenze e aggiorna README tecnico del progetto.
```
