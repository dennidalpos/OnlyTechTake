# Google Play Data Safety Guide - OnlyTechTake

Questa guida fornisce le informazioni necessarie per compilare il modulo **Data Safety** su Google Play Console per l'app OnlyTechTake.

## Filosofia dell'App
OnlyTechTake è un'app **Local-First**. Tutti i dati raccolti sono memorizzati esclusivamente sul dispositivo dell'utente. Non esiste una raccolta dati automatica verso i server dello sviluppatore o di terze parti.

---

## 1. Raccolta e Condivisione dei Dati

### L'app raccoglie i seguenti tipi di dati?
**SÌ**, ma solo localmente. Nel modulo Google Play, se i dati rimangono sul dispositivo, Google permette di dichiarare "No" alla raccolta se i dati non vengono trasmessi fuori dal dispositivo. Tuttavia, per trasparenza, è meglio dichiarare cosa l'app gestisce.

### I dati vengono condivisi con terze parti?
**NO**. L'app non condivide dati automaticamente. La condivisione avviene solo tramite iniziativa manuale dell'utente (es. invio di un PDF tramite WhatsApp).

---

## 2. Dettaglio dei Dati Gestiti

### Informazioni Personali
- **Nome e Cognome**: Gestiti per l'anagrafica clienti.
- **Indirizzo Email**: Gestito per i contatti clienti.
- **Indirizzo Fisico**: Gestito per l'anagrafica clienti.
- **Numero di Telefono**: Gestito per i contatti clienti e integrazione WhatsApp.
- **Codice Fiscale / Partita IVA**: Gestiti per finalità fiscali/amministrative.

### Foto e Video
- **Foto**: Acquisite tramite CameraX per documentare lo stato dei dispositivi.
- **Video**: Acquisiti per documentare il funzionamento dei dispositivi.

### Firme
- **Firma Digitale**: Acquisita per l'accettazione dei termini di servizio.

---

## 3. Pratiche di Sicurezza

- **Dati criptati in transito?**: N/A (i dati non vengono trasmessi). Tuttavia, il database locale è criptato con **SQLCipher**.
- **Metodo di eliminazione dei dati**: L'utente può eliminare singoli record o eseguire un "Reset Totale" dalle impostazioni dell'app per cancellare tutto permanentemente.

---

## 4. Finalità
- **Funzionalità dell'app**: Tutti i dati sopra elencati sono necessari per il core business dell'app (gestione commesse).
- **Comunicazione**: Il numero di telefono è usato per la funzione WhatsApp.

---

## Riassunto per Google Play
- **Data Collection**: No (i dati rimangono sul dispositivo).
- **Data Sharing**: No.
- **Data Deletion**: Disponibile tramite l'app.
