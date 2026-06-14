# Piano di Recupero Autenticazione (Auth Recovery Plan)

Questo documento delinea la strategia per il recupero delle credenziali admin e la gestione della sicurezza delle password, come richiesto dai task P0.

## 1. Separazione Reset Operatori vs Recovery Admin

### Reset Operatori (Standard)
- **Attore**: Utente con permesso `MANAGE_USERS` (tipicamente Admin).
- **Meccanismo**: Pannello Amministratore -> Gestione Utenti -> Reset Password.
- **Vincolo**: Non è possibile resettare la propria password da questo pannello (richiede flusso "Cambio Password").
- **Audit**: Loggato come `PASSWORD_RESET` con ID dell'operatore target.

### Recovery Admin (Emergenza)
- **Scenario**: L'unico amministratore ha dimenticato la password o l'account è bloccato.
- **Meccanismo**: Progetto di recupero offline (v. Sezione 2).

## 2. Progetto Recupero Password Admin Offline

Poiché l'app funziona in modalità `LOCAL_ONLY`, non è possibile un recupero via email/SMS senza server.

### Strategia: Recovery File / Codice
1. **Generazione**: Al momento della creazione del primo admin o su richiesta, l'app genera un "Recovery Kit" (file PDF o testo cifrato) contenente un codice master monouso.
2. **Conservazione**: L'utente deve salvare il kit fuori dal dispositivo (es. stampa, cloud sicuro, chiavetta USB).
3. **Utilizzo**: Nella schermata di login, un'opzione "Recupero Admin" richiede l'inserimento del codice master.

### Reset Distruttivo (Ultima Spiaggia)
Se il Recovery Kit è perso:
1. L'utente può richiedere un "Reset Totale di Fabbrica".
2. **Sicurezza**: Richiede la cancellazione completa di TUTTI i dati (Database e Media) per prevenire accessi non autorizzati ai dati sensibili tramite reset dell'account.
3. **Backup Obbligatorio**: Prima di procedere, l'app avverte che senza un backup ZIP esterno recente, i dati andranno persi per sempre.

## 3. Sicurezza Password e Cambio Obbligatorio

### Cambio Password al Primo Accesso
- Aggiungere campo `forcePasswordChange: Boolean` a `UserEntity`.
- Quando un Admin crea un operatore o resetta una password, `forcePasswordChange` viene impostato a `true`.
- Al login, se `forcePasswordChange == true`, l'utente viene reindirizzato forzatamente a una schermata "Cambio Password Obbligatorio".

### Riduzione Uso String per Password
- Utilizzo di `CharArray` in tutto il flusso `AuthRepository` -> `AdminUserRepository` -> `PasswordHasher`.
- Azzeramento (`fill('\u0000')`) dei `CharArray` immediatamente dopo l'uso (hashing o verifica).
- Limitare la persistenza delle password in chiaro nella memoria dei ViewModel.

## 4. Casi Limite e Policy Anti-Abuso
- **Lockout**: Il recovery non azzera il contatore di tentativi falliti per prevenire brute-force sul codice di recupero.
- **Admin Unico**: Impedire la cancellazione o disattivazione dell'ultimo admin rimasto.
- **Notifica**: Se possibile, mostrare un warning persistente se non è ancora stato generato un Recovery Kit.
