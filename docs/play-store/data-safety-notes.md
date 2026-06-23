# Note per Data Safety (Google Play)

Questo documento riassume le informazioni necessarie per compilare la sezione "Data Safety" in Google Play Console per OnlyTechTake.

## Principio Fondamentale
OnlyTechTake è un'applicazione **local-first**. Tutti i dati vengono memorizzati esclusivamente sul dispositivo dell'utente. Non esiste un backend cloud proprietario per la sincronizzazione o la raccolta dei dati.

## Dichiarazioni Modulo Data Safety

### Raccolta Dati (Collection)
Secondo le definizioni di Google Play, i dati trattati esclusivamente on-device non costituiscono "raccolta" se non vengono trasmessi fuori dal dispositivo.
- **Dichiarazione**: L'app non raccoglie dati dell'utente (nel senso di trasmissione a server remoti).

### Condivisione Dati (Sharing)
La condivisione dei dati avviene solo su azione esplicita e manuale dell'utente (Handoff):
- **WhatsApp**: L'app prepara un messaggio che l'utente deve confermare e inviare manualmente.
- **PDF/XLSX**: L'utente sceglie di condividere il file tramite il sistema Android (Email, Drive, etc.).
- **Backup**: L'utente esporta manualmente un file ZIP in una cartella locale o su cloud tramite SAF (Storage Access Framework).

### Sicurezza dei Dati
- **Dati in transito**: Non applicabile (nessun trasferimento automatico).
- **Dati a riposo**: Il database locale è cifrato tramite **SQLCipher**.
- **Cancellazione**: L'utente può eliminare tutti i dati tramite la funzione di "Reset totale" nelle impostazioni dell'app.

## Risposte per il questionario
- **L'app raccoglie o condivide i tipi di dati utente richiesti?** No (se si segue l'interpretazione local-only di Google).
- **Tutti i dati utente raccolti dall'app vengono crittografati in transito?** Sì (anche se non c'è transito, si può dichiarare per sicurezza se richiesto dal modulo).
- **Fornisci agli utenti un modo per richiedere l'eliminazione dei loro dati?** Sì (Reset locale).
