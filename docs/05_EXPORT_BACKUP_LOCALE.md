# Export e backup locale

## Stato operativo

L'export è implementato per admin tramite Storage Access Framework. Il ripristino/import non è implementato in questo rilascio.

## Sezione app

```text
Config
Backup / Export
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

Output: `repair-app-backup-YYYY-MM-DD-HH-mm.zip`.

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

## Metadata

Il backup completo include metadati con data creazione, nome database, versione database e sezioni incluse.

## Sicurezza

- L'app non usa backup cloud Android: `android:allowBackup="false"` e le regole XML escludono file, database e shared preferences.
- Il salvataggio usa Storage Access Framework; non usare percorsi assoluti fragili.
- Gli export richiedono ruolo admin.
- Ogni export registra audit `DATA_EXPORTED`.
- Configurazione, database, PDF e backup ZIP non sono cifrati dall'app. La
  cifratura non e' attivata in questo rilascio per evitare gestione passphrase e
  recupero chiavi dentro l'app locale; salvare gli export solo su supporti,
  cartelle o volumi protetti secondo procedura operativa.
