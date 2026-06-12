# Play Console Questionnaire Draft

Bozza delle risposte per i questionari obbligatori della Google Play Console.

## 1. Sicurezza dei dati (Data Safety)

| Domanda | Risposta | Note |
|---------|----------|------|
| L\'app raccoglie o condivide dati utente? | **Sì** | Pur essendo locale, i dati inseriti dall\'utente sono considerati raccolti. |
| Tutti i dati raccolti dall\'app sono crittografati in transito? | **Sì** | L\'app non trasmette dati a un proprio server. Le condivisioni via Intent (WhatsApp/PDF) usano i meccanismi sicuri di Android. |
| Offri un modo per richiedere l\'eliminazione dei dati? | **Sì** | L\'utente può usare la funzione "Reset totale dati" nelle impostazioni Admin. |

### Dati Raccolti e Scopi
*Nota: L\'app opera in modalità LOCAL_ONLY. I dati rimangono sul dispositivo.*

- **Informazioni personali:** Nome, Indirizzo, Numero di telefono, Email.
  - **Scopo:** Gestione ordini, Comunicazione con il cliente.
- **Foto:** Foto del dispositivo in riparazione.
  - **Scopo:** Funzionalità dell\'app (documentazione danni).
- **Firma:** Immagine della firma del cliente.
  - **Scopo:** Funzionalità dell\'app (accettazione termini).
- **File e documenti:** PDF generati, Export Excel/ZIP.
  - **Scopo:** Funzionalità dell\'app.

## 2. Annunci (Ads)
- **L\'app contiene annunci?** No.

## 3. Classificazione contenuti (Content Rating)
- **App di riferimento, notizie o didattica?** No.
- **Social network o condivisione contenuti?** No (condivisione manuale 1-a-1).
- **Contenuti generati dagli utenti?** No (solo dati gestionali).
- **L\'app vende beni fisici?** No.

## 4. Target di età
- **Target:** 18 anni o superiore (professionisti/aziende).
- **App per bambini?** No.

## 5. Permessi Sensibili e Accesso
- **Accesso fotocamera:** Sì, necessaria per documentare lo stato dei dispositivi in riparazione.
- **Accesso ai file:** L\'app usa lo storage interno e `FileProvider`. Su versioni Android recenti non richiede permessi broad di lettura/scrittura storage grazie all\'uso del sistema di export tramite Intent.
- **Localizzazione:** No.
- **Contatti:** No.

## 6. Sicurezza e Backup
- **Backup Android:** Disabilitato nel manifest (`android:allowBackup="false"`) e configurato in `backup_rules.xml` / `data_extraction_rules.xml` per escludere tutti i dati locali (database, file, preferenze) dai backup cloud di sistema e dai trasferimenti tra dispositivi.
- **Backup Manuale:** Gestito dall'utente tramite export ZIP locale o verso Cloud scelto dall'utente.

---
**VERIFICHE COMPLETATE:**
- `android:hasFragileUserData="true"`: Confermato nel manifest. Permette di mantenere i dati se l'utente sceglie di farlo durante la disinstallazione.
- `DataExtractionRules`: Verificato. Esclude esplicitamente tutti i dati sensibili dai backup automatici di Android, garantendo che i dati rimangano sotto il controllo dell'utente tramite backup manuali.
