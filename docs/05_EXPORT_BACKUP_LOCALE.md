# Export e backup locale

## Stato operativo

Export configurazione, database, ZIP completo, XLSX commesse e ripristino ZIP completo sono implementati per admin tramite Storage Access Framework.

## Sezione app

```text
Config
Backup
```

Visibile solo ad admin.

## Tipi export

### Configurazione

Output: `configuration.json`.

Contiene categorie, template WhatsApp e impostazioni applicative esportate in JSON.

### Database

Output: `repair_app.db`.

Copia il database Room locale quando esiste.

### Backup completo

Output suggerito dal selettore: `onlytechtake-backup.zip`. L'utente puo'
scegliere un nome diverso tramite Storage Access Framework.

Contiene:

```text
metadata/backup.json
config/configuration.json
database/repair_app.db
database/repair_app.db-*
files/orders/<orderId>/signatures/...
files/order_media/<orderId>/...
```

Foto CameraX, allegati importati e firme restano in spazio privato app e vengono copiati nel backup completo.

### XLSX commesse

Output: `onlytechtake-commesse.xlsx`.

Contiene l'export tabellare delle commesse per uso admin.

### Ripristino backup

Output accettato: ZIP completo generato da `Export ZIP`.

Il ripristino:

- richiede ruolo admin;
- valida metadata, versione database e presenza di `database/repair_app.db`;
- rifiuta percorsi ZIP non sicuri o file app fuori dalle cartelle supportate;
- sostituisce database locale e file privati `orders` / `order_media`;
- cancella sessione locale e richiede un nuovo login.

Il ripristino e' pensato per trasferire l'app su un altro telefono con lo stesso pacchetto applicativo. Prima di ripristinare, salvare un export ZIP recente dal telefono sorgente.

## Metadata

Il backup completo include metadati con data creazione, nome database, versione database e sezioni incluse.

## Sicurezza

- L'app usa backup manuale tramite ZIP selezionato dall'admin. Il backup cloud Android resta disattivato: `android:allowBackup="false"` e le regole XML escludono file, database e shared preferences.
- Il salvataggio usa Storage Access Framework; non usare percorsi assoluti fragili.
- Export e ripristino richiedono ruolo admin.
- Ogni export registra audit `DATA_EXPORTED`.
- Configurazione, database, PDF e backup ZIP non sono cifrati dall'app. La
  cifratura non e' attivata in questo rilascio per evitare gestione passphrase e
  recupero chiavi dentro l'app locale; salvare gli export solo su supporti,
  cartelle o volumi protetti secondo procedura operativa.
