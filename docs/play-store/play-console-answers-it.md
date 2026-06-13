# Guida alla compilazione Play Console — OnlyTechTake

Questo documento fornisce le risposte suggerite per i questionari della Google Play Console, basate sull'analisi del codice sorgente di **OnlyTechTake Repair App**.

---

## 1. Riepilogo App

- **Nome app:** OnlyTechTake
- **Package:** `com.onlytechtake.repairapp`
- **Versione attuale:** `0.1.0` (Alpha/Development)
- **Uso principale:** Gestione ordini di riparazione, catalogazione interventi, raccolta firme e foto per laboratori tecnici.
- **Architettura:** LOCAL-ONLY (i dati risiedono sul dispositivo dell'utente).

---

## 2. Permessi Android rilevati

| Permesso | Perché serve | Funzione nell'app | Risposta per Google |
| :--- | :--- | :--- | :--- |
| `android.permission.CAMERA` | Acquisizione foto dispositivi | Documentazione dello stato del dispositivo in ingresso/uscita | L'app utilizza la fotocamera per permettere ai tecnici di scattare foto ai dispositivi riparati come prova dello stato estetico e funzionale. |

*Nota: Non sono richiesti permessi di Storage globale (MANAGE_EXTERNAL_STORAGE) poiché l'app utilizza il FileProvider e lo storage interno/privato.*

---

## 3. Sicurezza dei Dati (Data Safety)

### Raccolta e Condivisione
L'app **raccoglie** dati sul dispositivo, ma **non li trasmette** a server esterni automaticamente. La condivisione avviene solo su azione esplicita dell'utente (tramite Share Intent).

| Tipo dato | Raccolto? | Finalità | Salvataggio | Condiviso con terzi? | Obbligatorio? | Eliminabile? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Nome Cliente** | Sì | Gestione Ordine | Room DB (Locale) | No* | Sì | Sì |
| **Telefono/Email** | Sì | Comunicazione | Room DB (Locale) | No* | Opzionale | Sì |
| **Indirizzo** | Sì | Anagrafica | Room DB (Locale) | No* | Opzionale | Sì |
| **Foto** | Sì | Documentazione | App Storage (Privato) | No* | Opzionale | Sì |
| **Firma Digitale** | Sì | Aspetti legali/Privacy | App Storage (Privato) | No* | Sì | Sì |
| **Password (Admin)** | Sì | Autenticazione Locale | Room DB (Hashed) | No | Sì | Sì (Reset app) |
| **ID Dispositivo** | No | - | - | No | - | - |

*\*I dati possono essere condivisi dall'utente tramite PDF esportati su app esterne (WhatsApp, Email) scelte manualmente.*

### Sicurezza (Security Practices)
1. **Dati in transito:** Non applicabile (l'app non ha un backend proprio).
2. **Dati a riposo:** Le password sono memorizzate tramite hash e salt (`passwordHash`, `passwordSalt`).
3. **Backup:** Il backup di Android Cloud è disattivato (`android:allowBackup="false"`). L'app promuove backup manuali in formato ZIP locale scelto dall'admin; nello stato attuale lo ZIP non è cifrato dall'app.
4. **Cancellazione:** L'admin può usare il reset totale dati dell'app oppure la cancellazione dati/disinstallazione dalle impostazioni Android per rimuovere i dati locali.

---

## 4. Annunci (Ads)
- **Risposta:** "L'app non contiene annunci".
- **Verifica:** Non sono presenti SDK di AdMob, Facebook Ads o altri provider nel file `build.gradle.kts`.

---

## 5. Target Audience e Contenuti

- **Target:** 18+ (Professionisti, tecnici riparatori, negozianti).
- **Non destinata ai bambini:** Sì.
- **Content Rating (IARC):**
    - Violenza: No.
    - Sesso/Nudità: No.
    - Linguaggio offensivo: No.
    - Sostanze controllate: No.
    - Gambling: No.
    - UGC (User Generated Content): No (i dati sono privati dell'utente).

---

## 6. Account e Eliminazione Dati

- **L'app consente la creazione di account?** Sì, account locali per gestire gli accessi dei tecnici al database locale.
- **Eliminazione account:** Poiché gli account sono locali, l'eliminazione dei dati avviene tramite la disinstallazione dell'app o la funzione "Cancella dati" nelle impostazioni di Android.
- **Link Eliminazione Dati:** Se Google richiede un URL, fornire il link alla sezione Privacy Policy che spiega come resettare i dati locali.

---

## 7. Gap da risolvere prima del rilascio

1. **Privacy Policy:** È necessario un URL pubblico che spieghi che i dati sono trattati esclusivamente in locale sul dispositivo.
2. **Email di Supporto:** Definire un indirizzo email valido per la Play Console.
3. **Test Policy Camera:** Assicurarsi che l'app gestisca correttamente il rifiuto del permesso Camera senza crash.

---
*Documento generato automaticamente basato sull'analisi del codice - Revisione manuale consigliata prima dell'invio.*
