# Demo/Full e manuale operativo

Documento operativo per usare e verificare i due flavor Android reali del repository. Non descrive prezzi, roadmap commerciale o funzionalita' non implementate.

## Flavor implementati

Il modulo `app` definisce due product flavor:

| Voce | Demo | Full |
|---|---|---|
| Flavor Gradle | `demo` | `full` |
| App release ID | `com.onlytechtake.repairapp.demo` | `com.onlytechtake.repairapp` |
| App debug ID | `com.onlytechtake.repairapp.demo.debug` | `com.onlytechtake.repairapp.debug` |
| Nome app | `OnlyTechTake Demo` | `OnlyTechTake` |
| `IS_DEMO_VERSION` | `true` | `false` |
| `IS_DEMO_MODE_ENABLED` | `true` | `false` |
| `MAX_ORDERS` | `10` | `-1` |

## Limiti Demo implementati

Nel flavor Demo sono implementati questi blocchi:

- massimo 10 commesse totali;
- massimo 2 operatori oltre all'admin;
- export database bloccato;
- export ZIP completo bloccato;
- ripristino ZIP bloccato;
- export XLSX bloccato;
- PDF riepilogo con watermark `VERSIONE DEMO`;
- banner `VERSIONE DEMO` nella Home;
- pulsante `Avvia Modalita Demo` nel login, con dati demo in memoria.

Non risultano implementati:

- licensing online;
- attivazione remota;
- trial a tempo;
- pagamento in-app;
- sincronizzazione cloud;
- WhatsApp Business API.

## Funzioni Full implementate

Nel flavor Full non ci sono limiti hardcoded su numero commesse o operatori. Le funzioni restano vincolate ai permessi dell'utente locale.

Funzioni admin Full rilevanti:

- gestione utenti e permessi;
- categorie multilivello;
- template privacy, condizioni e WhatsApp;
- modalita WhatsApp manuale;
- backup ZIP completo tramite Config > Backup;
- ripristino ZIP tramite Config > Backup;
- export XLSX tramite Config > Backup;
- export database e XLSX pre-cleanup dalla sezione Admin;
- reset totale dati con conferma `RESET`;
- cancellazione totale commesse con conferma `CANCELLA TUTTO`.

## Primo avvio

Su installazione pulita:

1. Aprire l'app.
2. Creare l'admin locale con nome, username, password e conferma password.
3. Entrare nella dashboard.

Dagli avvii successivi:

1. Inserire username e password.
2. Toccare `Accedi`.

Regole auth implementate:

- password minima 8 caratteri;
- lockout temporaneo dopo 5 tentativi falliti;
- scadenza sessione dopo 30 minuti di inattivita';
- logout manuale dalla top bar.

## Flusso operativo minimo

1. Accedere come admin o operatore.
2. Aprire `Nuova`.
3. Selezionare o creare un cliente valido.
4. Inserire problema/sintesi.
5. Selezionare una categoria attiva.
6. Aggiungere almeno una foto o un allegato.
7. Far accettare privacy e condizioni.
8. Acquisire la firma cliente.
9. Confermare la commessa.
10. Gestire diagnosi, preventivo, WhatsApp manuale, riparazione o pronto ritiro dal dettaglio commessa.
11. Chiudere la commessa con motivo e nota.

## WhatsApp manuale

OnlyTechTake non invia messaggi automaticamente.

Flusso implementato:

1. L'operatore prepara il messaggio dal dettaglio commessa.
2. L'anteprima e' modificabile.
3. L'app apre WhatsApp o il browser tramite intent/link.
4. L'operatore invia manualmente.
5. L'operatore torna in app e conferma l'invio.
6. Timeline e message log vengono aggiornati.

Non sono presenti backend, webhook, token o API WhatsApp Business nell'app.

## Backup operativo

Il backup primario e' il ZIP completo manuale tramite Storage Access Framework.

Percorso UI:

```text
Config > Backup > Esegui backup ZIP completo
```

Il promemoria backup admin e' attivo di default ogni 7 giorni, si basa sull'ultimo ZIP completo riuscito e non blocca l'uso dell'app.

Prima di cancellazioni o reset:

- eseguire ZIP completo se i dati devono essere conservati;
- verificare che il file sia stato salvato fuori dal dispositivo principale;
- ricordare che disinstallazione o cancellazione dati Android possono eliminare i dati locali.

## Comandi build

APK Demo debug:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Demo -BuildType Debug -OutputType Apk
```

APK Full debug:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk
```

APK Demo release:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Demo -BuildType Release -OutputType Apk
```

APK Full release:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Apk
```

AAB Full release:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Aab
```

Le build release richiedono `keystore.properties` locale configurato nella root.

## Checklist consegna

Prima di consegnare una build a un tester o a un'altra persona:

- verificare `versionCode` e `versionName` in `app\build.gradle.kts`;
- eseguire `scripts\android\check_env.ps1`;
- eseguire `scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild`;
- generare l'APK/AAB del flavor richiesto;
- installare su device pulito con `start_app.ps1 -CleanInstall` quando si vuole evitare incompatibilita' dati locali;
- verificare setup admin, creazione cliente, nuova commessa con media e firma, diagnosi, PDF, WhatsApp manuale, ZIP backup e restore se richiesto;
- non usare dati reali in screenshot, video o demo pubbliche.
