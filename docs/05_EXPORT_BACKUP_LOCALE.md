# Export e backup locale

## Stato operativo

Export ZIP completo, XLSX commesse, configurazione JSON e ripristino ZIP completo sono esposti agli admin tramite Config > Backup e Storage Access Framework.
L'export database e l'export XLSX pre-cleanup sono esposti nella sezione Admin prima della cancellazione totale commesse.
Eventuali provider come Drive, OneDrive o Dropbox compaiono solo come destinazioni del selettore documenti Android: non esiste un backend o una sincronizzazione cloud proprietaria.

## Sezione app

```text
Config
Backup
Admin
```

Visibile solo ad admin.

## Tipi export

### Configurazione

Output: `configuration.json`.

Contiene categorie, template WhatsApp e impostazioni applicative esportate in JSON.
La funzione esiste nel repository, ma non ha un pulsante dedicato nella schermata Backup corrente.

### Database

Output: `repair_app.db`.

Copia il file database Room/SQLCipher locale quando esiste. Nell'interfaccia corrente e' disponibile dalla sezione Admin come backup consigliato prima della cancellazione totale commesse, non dalla schermata Config > Backup. Il file resta legato alla chiave SQLCipher locale protetta da Android Keystore.

### Backup completo

Output suggerito dal selettore: `onlytechtake-backup.zip`. L'utente puo'
scegliere un nome diverso tramite Storage Access Framework.

Contiene:

```text
metadata/backup.json
config/configuration.json
database/repair_app.db
database/repair_app.db-*
files/app_media/order_media/<orderId>/...
files/app_media/signatures/<orderId>/...
files/app_media/documents/<orderId>/...
```

Foto CameraX, allegati importati e firme restano in spazio privato app e vengono copiati byte per byte nel backup completo, conservando metadati EXIF e orientamento. L'anteprima applica EXIF in lettura; le modifiche esplicite dell'operatore riscrivono invece l'immagine normalizzata.

### XLSX commesse

Output: `onlytechtake-commesse.xlsx`.

Contiene l'export tabellare delle commesse per uso admin, inclusi esito diagnosi e stato pagamento derivato dall'ultimo evento `PAYMENT_RECEIVED` o `PAYMENT_REMOVED` registrato nella timeline.

Il PDF della commessa elenca timeline e nomi dei media; non incorpora bitmap,
quindi non altera ne' ricodifica l'orientamento delle foto.

### Ripristino backup

Output accettato: ZIP completo generato da `Export ZIP`.

Il ripristino:

- richiede ruolo admin;
- valida metadata, versione database e presenza di `database/repair_app.db`;
- valida che il database sia apribile con la chiave SQLCipher locale corrente;
- rifiuta percorsi ZIP non sicuri o file app fuori dalle cartelle supportate;
- applica un meccanismo di snapshot/rollback: se la sostituzione di database o media fallisce, l'app tenta di ripristinare lo stato precedente da una copia temporanea in cache;
- gestisce la consistenza dell'attore: se l'utente che esegue il ripristino non esiste nel database appena caricato, l'audit log registra l'operazione senza riferimento utente (null) per evitare violazioni di integrità;
- sostituisce database locale e directory canonica `filesDir/app_media`;
- cancella sessione locale e richiede un nuovo login.

### Scenari di migrazione e limiti

Il backup ZIP completo **non include** la chiave di cifratura SQLCipher (protetta da Android Keystore).

1. **Stesso dispositivo, stessa installazione**: il ripristino è garantito poiché la chiave nel Keystore è identica.
2. **Nuovo dispositivo o reinstallazione totale**: il database nel backup risulterà illeggibile poiché la nuova installazione genererà una chiave diversa. Il ripristino fallirà con errore di validazione (chiave errata).

**Raccomandazione**: non usare il backup ZIP come unica procedura di migrazione verso un nuovo telefono. In assenza di un flusso di esportazione chiavi dedicato (non ancora implementato), la migrazione dei dati cifrati tra dispositivi diversi non è supportata dal solo file ZIP.

## Promemoria backup

Il promemoria backup admin e' implementato.

- E' attivo di default.
- L'intervallo predefinito e' 7 giorni.
- L'admin puo' abilitarlo/disabilitarlo e modificare l'intervallo in Config > Backup.
- La Home mostra il promemoria quando non esiste un ZIP completo riuscito o quando l'ultimo ZIP completo riuscito e' piu' vecchio dell'intervallo configurato.
- Un backup fallito non aggiorna la data dell'ultimo backup riuscito.
- Il promemoria non blocca il normale uso dell'app.

## Metadata

Il backup completo include metadati con data creazione, nome database, versione database e sezioni incluse.
La versione database attesa e' quella di `RepairAppDatabase.DATABASE_VERSION`; nello stato corrente del codice e' `1`. Lo schema Room e' bloccato su v1 fino a ordine esplicito dell'owner.

## Sicurezza

- L'app usa backup manuale tramite ZIP selezionato dall'admin. Il backup cloud Android resta disattivato: `android:allowBackup="false"` e le regole XML escludono file, database e shared preferences.
- Il database locale e' cifrato con SQLCipher; la chiave e' generata localmente e protetta tramite Android Keystore.
- Il salvataggio usa Storage Access Framework; non usare percorsi assoluti fragili.
- Export e ripristino richiedono ruolo admin.
- Ogni export registra audit `DATA_EXPORTED`.
- Prima di azioni distruttive, usare il backup ZIP completo quando serve preservare dati locali; la cancellazione/disinstallazione completa puo' eliminare i dati.
- L'app supporta la cifratura dei backup ZIP e XLSX tramite password AES-256 configurabile dall'admin in Config > Backup.
- I PDF e la configurazione JSON singola non sono cifrati dall'app; salvare gli export solo su supporti, cartelle o volumi protetti secondo procedura operativa.
