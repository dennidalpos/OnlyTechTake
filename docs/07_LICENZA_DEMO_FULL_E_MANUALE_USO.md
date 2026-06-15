# Demo/Full e manuale operativo

Documento operativo per usare e verificare i tre flavor Android reali del repository. Non descrive prezzi, roadmap commerciale o funzionalita' non implementate.

## Flavor implementati

Il modulo `app` definisce tre product flavor:

| Voce | Demo | Showcase | Full |
|---|---|---|---|
| Flavor Gradle | `demo` | `showcase` | `full` |
| App release ID | `com.onlytechtake.repairapp.demo` | `com.onlytechtake.repairapp.showcase` | `com.onlytechtake.repairapp` |
| App debug ID | `com.onlytechtake.repairapp.demo.debug` | `com.onlytechtake.repairapp.showcase.debug` | `com.onlytechtake.repairapp.debug` |
| Nome app | `OnlyTechTake Demo` | `OnlyTechTake Showcase` | `OnlyTechTake` |
| `IS_DEMO_VERSION` | `true` | `false` | `false` |
| `IS_DEMO_MODE_ENABLED` | `true` | `false` | `false` |
| `MAX_ORDERS` | `10` | `-1` | `-1` |

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

## Flavor Showcase (Dati precaricati)

Il flavor Showcase e' progettato per presentazioni rapide con dati reali gia' inseriti.

Caratteristiche:
- **Popolamento automatico**: al primo avvio vengono creati 10 clienti (5 privati, 5 business) e 30 commesse (3 per cliente, incluse chiuse).
- **Media di esempio**: ogni commessa include una foto dell'oggetto e una firma del cliente generate dinamicamente.
- **Utenti predefiniti**: creati 3 operatori con permessi diversi e 1 admin.
- **Auto-login**: l'app esegue l'accesso automatico come Admin al termine dell'inizializzazione.
- **Icona dedicata**: riconoscibile dal banner blu "SHOW" sull'icona dell'app.

Credenziali (password per tutti: `ShowcasePassword123`):
- Admin: `admin`
- Reception: `anna.reception`
- Tecnico: `marco.tech`
- Manager: `elena.manager`

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

## Primo avvio (Flavor Full/Demo)

Su installazione pulita:

1. Aprire l'app.
2. Creare l'admin locale con nome, username, password e conferma password.
3. Entrare nella dashboard.

Dagli avvii successivi:

1. Inserire username e password.
2. Toccare `Accedi`.

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

## Backup operativo

Il backup primario e' il ZIP completo manuale tramite Storage Access Framework.

Percorso UI:

```text
Config > Backup > Esegui backup ZIP completo
```

Il promemoria backup admin e' attivo di default ogni 7 giorni, si basa sull'ultimo ZIP completo riuscito e non blocca l'uso dell'app.

## Comandi build

APK Showcase debug:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Showcase -BuildType Debug -OutputType Apk
```

APK Demo debug:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Demo -BuildType Debug -OutputType Apk
```

APK Full debug:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk
```
