# Guida Play Console (Questionari e Risposte)

Documento di riferimento per la compilazione dei questionari obbligatori su Google Play Console per OnlyTechTake Repair App.

## 1. Accesso all'app (App Access)
- **Opzione:** Tutte o alcune parti dell'app sono limitate.
- **Istruzioni per i revisori:**
  - Fornire le credenziali dell'account Admin predefinito.
  - Esempio: "L'app richiede un login locale. Usare Username: `admin` e Password: `password123`. L'app non richiede connessione internet per il login."

## 2. Annunci (Ads)
- **Risposta:** No, l'app non contiene annunci.

## 3. Classificazione dei contenuti (Content Rating)
- **Categoria:** Utility, Productivity, Communication o Altro.
- **Domande chiave:**
  - L'app permette di acquistare beni digitali? **No**.
  - L'app condivide la posizione fisica dell'utente? **No**.
  - L'app permette agli utenti di comunicare? **Sì**, tramite l'integrazione con l'app WhatsApp installata sul dispositivo (invio manuale di messaggi).
  - L'app contiene contenuti offensivi o per adulti? **No**.

## 4. Sicurezza dei dati (Data Safety)
OnlyTechTake gestisce i dati esclusivamente in locale, ma Google richiede di dichiarare come vengono trattati.

| Tipo di Dato | Raccolto | Condiviso | Scopo |
| :--- | :--- | :--- | :--- |
| Nome/Email/Telefono Cliente | Sì (Locale) | No | Gestione commesse |
| Foto/Video | Sì (Locale) | No | Documentazione riparazione |
| Informazioni finanziarie (Importi) | Sì (Locale) | No | Preventivi |
| Identificatori dispositivo | No | No | - |

**Note per il modulo:**
- I dati sono criptati in transito? **Sì** (se si considera l'invio via WhatsApp o la condivisione PDF che usa canali sicuri del sistema).
- L'utente può richiedere l'eliminazione dei dati? **Sì**, l'admin può eliminare singoli record o eseguire il reset totale dall'app.

## 5. App di monitoraggio (News/COVID-19/Gov)
- **Risposte:** No a tutte (l'app non è un'app di news, né legata al COVID-19 o a enti governativi).

## 6. Sicurezza e Privacy (Handoff Hacking)
L'app utilizza `android:hasFragileUserData="true"` per suggerire al sistema di conservare i dati durante gli aggiornamenti, ma è fondamentale ricordare all'utente che la disinstallazione completa può comportare la perdita dei dati se non è stato eseguito un backup ZIP manuale.
