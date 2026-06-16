# 02 - Specifica tecnica

## Stack effettivo

- Kotlin
- Jetpack Compose
- Material 3
- Android Architecture Components
- Room
- SQLCipher
- DataStore
- CameraX
- Kotlin Coroutines
- Kotlin Flow
- Navigation Compose
- Storage Access Framework per import allegati ed export
- Gradle Wrapper

## Architettura

Architettura semplice:

```text
UI Compose
↓
ViewModel
↓
UseCase opzionali
↓
Repository
↓
DAO Room / DataStore / File storage
```

Non creare microservizi o moduli complessi nella prima versione.

## Struttura pacchetti

```text
app/src/main/java/<package>/
  core/
    design/
    di/
    media/
    orders/
    permissions/
    security/
    templates/
  data/
    local/
      db/
      dao/
      entities/
    preferences/
    repositories/
    session/
  domain/
    auth/
    models/
    repositories/
    workspace/
  features/
    auth/
    dashboard/
    workspace/
  navigation/
```

## Database

Database locale Room cifrato con SQLCipher.

Nome database:

```text
repair_app.db
```

Lo schema corrente nel codice e' Room v4:

```kotlin
const val DATABASE_VERSION = 4
```

Sono presenti export schema `1.json`, `2.json` e `4.json`. Il progetto e' trattato come
fresh app in sviluppo iniziale: non sono presenti classi di migrazione Room e
la builder usa `fallbackToDestructiveMigration(dropAllTables = true)`. Dopo
modifiche incompatibili o quando si passa tra build con schema diverso, usare
una reinstallazione pulita del package installato.

Nota governance: alcune istruzioni repository richiedono approvazione owner per
ulteriori cambi schema. Lo stato reale del codice al momento di questo handoff
e' v4; non incrementare ancora `DATABASE_VERSION` senza richiesta esplicita.

Schema esportato corrente:

```text
app/schemas/com.onlytechtake.repairapp.data.local.db.RepairAppDatabase/1.json
app/schemas/com.onlytechtake.repairapp.data.local.db.RepairAppDatabase/2.json
app/schemas/com.onlytechtake.repairapp.data.local.db.RepairAppDatabase/4.json
```

## Entità principali

- UserEntity
- CustomerEntity
- OrderCategoryEntity, con parentId e level coerente per albero categorie a livelli 0..N
- RepairOrderEntity, con `diagnosisOutcome` nullable finche' la diagnosi non viene registrata
- OrderMediaEntity
- SignatureEntity
- TimelineEventEntity
- MessageTemplateEntity
- MessageLogEntity
- AppSettingEntity
- AuditLogEntity
- OrderDocumentEntity, per PDF/documenti generati e condivisi dalla commessa

## Repository

Repository principali:

```text
AdminUserRepository
BackupRepository
CatalogRepository
CustomerRepository
DefaultAuthRepository
DefaultRepairAppRepository
OrderMediaRepository
OrderWorkflowRepository
SignatureRepository
TemplateRepository
```

## ViewModel principali

```text
AuthGateViewModel
DashboardViewModel
SharedWorkspaceViewModel
SettingsViewModel
AdminViewModel
```

## Navigazione

Schermate principali:

```text
auth
dashboard
```

## Sicurezza password

Non salvare password in chiaro.

Salvare:

```text
passwordHash
passwordSalt
```

Algoritmo adottato:

- PBKDF2WithHmacSHA256.

Parametri minimi suggeriti:

```text
salt casuale 16/32 byte
iterazioni almeno 120000
hash 256 bit
```

## Sessione

La sessione può essere locale in memoria.

Salvare in DataStore solo informazioni non sensibili:

```text
lastUserId opzionale
lastLoginAt opzionale
appConfigured true/false
```

Richiedere nuovamente password dopo inattività.

## Cifratura database

`RepairAppDatabase` usa `SupportOpenHelperFactory` di SQLCipher. La password
SQLCipher viene generata localmente da `DatabaseKeyManager`, salvata in
SharedPreferences e protetta da una master key Android Keystore.

Limiti operativi correnti:

- la chiave database non viene esportata nel backup ZIP;
- un database copiato su un'altra installazione senza la stessa chiave non e'
  apribile;
- se Android Keystore non riesce piu' a decifrare la chiave, il codice genera
  una nuova chiave e cancella il database locale non apribile.

## Permessi runtime Android

Permessi manifest:

- Camera;
- nessun permesso storage globale.

L'app usa `FileProvider` per la condivisione sicura dei PDF generati in `files/app_media/documents/<orderId>/`. Le autorizzazioni sono concesse tramite `FLAG_GRANT_READ_URI_PERMISSION`.

Import allegati ed export usano Storage Access Framework, quindi non richiedono
permessi larghi di lettura/scrittura storage.

## Foto, firme e documenti

Salvare file in directory private app:

```text
files/app_media/order_media/<orderId>/
files/app_media/signatures/<orderId>/
files/app_media/documents/<orderId>/
files/app_media/intake_media/<sessionId>/
```

Nel database salvare path locale privato e metadati essenziali. Le foto acquisite
con CameraX sono registrate in `order_media` e generano evento timeline. I PDF
di stato vengono generati in `documents` e condivisi tramite `ShareDocumentIntentFactory`. Le foto
opzionali del lavoro finito usano `mediaType = FINAL_PHOTO` nello stesso modello,
senza schema dedicato. `ImageCapture.targetRotation` viene impostato dalla rotazione
del `PreviewView` prima del bind e prima dello scatto. Le anteprime locali applicano
l'orientamento EXIF senza riscrivere il file originale. Le
firme sono registrate in `signatures`, salvate come PNG in
`files/app_media/signatures/<orderId>/` e includono snapshot/versione dei testi
privacy e condizioni usati al momento della firma.

La presa in carico usa `CreateIntakeOrderUseCase`: dopo la validazione crea
cliente/ordine, firma e media staged dentro una transazione Room unica fornita
dal container. Se un passaggio fallisce, la commessa non viene confermata nel
database.

Gli allegati importati tramite Storage Access Framework devono rispettare la
policy `AttachmentImportPolicy`: MIME ammessi `image/jpeg`, `image/png`,
`image/webp`, `application/pdf`, `text/plain`, Word DOC/DOCX ed Excel XLS/XLSX.
Il limite massimo per singolo allegato e' 25 MiB; contenuti con MIME sconosciuto
o non in allowlist vengono rifiutati prima della copia, e la copia interrompe lo
stream se il provider non dichiara la dimensione ma il contenuto supera il
limite.

## Backup

L'export completo crea ZIP con:

```text
metadata/backup.json
config/configuration.json
database/repair_app.db
database/repair_app.db-*
files/app_media/...
```

Il ripristino ZIP completo e' supportato per admin tramite Storage Access Framework e sostituisce database e `filesDir/app_media`.
ZIP completo e XLSX possono essere cifrati con password AES-256 quando
l'opzione e' attivata in Config > Backup. PDF, configurazione JSON e documenti
condivisi singolarmente non hanno cifratura applicativa propria.

Limite: poiche' il ZIP non contiene la chiave SQLCipher/Keystore, il ripristino
e' affidabile solo quando database e chiave locale sono compatibili. Non usarlo
come procedura garantita di migrazione verso una nuova installazione o un nuovo
telefono finche' non esiste un flusso esplicito di export/import chiave.

## WhatsApp

Interfaccia:

```kotlin
interface WhatsAppSender {
    suspend fun prepare(message: WhatsAppOutgoingMessage): Result<WhatsAppSendAction>
}
```

Implementazioni:

```text
ManualWhatsAppSender
```

Il rilascio corrente usa solo invio manuale. `MessageMode` contiene solo
`MANUAL_WHATSAPP`: non esistono stub API o modalita future finche' non viene
introdotto un backend reale.

## Template engine

I testi admin devono supportare variabili:

```text
{{numero_commessa}}
{{titolo_commessa}}
{{stato_commessa}}
{{cliente}}
{{esito_diagnosi}}
{{importo_preventivo}}
{{prossima_azione}}
{{motivo_chiusura}}
{{categoria_percorso}}
```

Variabili diverse da questo elenco devono essere rifiutate al salvataggio o al
rendering del template.

Implementare un semplice motore replace controllato.

Regole implementate:

- i template con variabili fuori allowlist vengono rifiutati al salvataggio;
- il rendering fallisce se riceve una variabile non supportata;
- l'anteprima del messaggio e' disponibile solo nel dettaglio della commessa corrente;
- cambio commessa, navigazione, cambio stato, feedback, conferma invio WhatsApp e chiusura preview eliminano lo stato locale della preview; l'apertura di WhatsApp la mantiene per la conferma al ritorno.

Limite: nella sezione Config non e' presente una preview renderizzata separata durante la modifica del template.

## Audit log e limiti

Le azioni sensibili tracciate includono:

- login admin;
- creazione operatore;
- aggiornamento utente;
- reset password;
- modifica template;
- modifica privacy;
- export dati;
- archiviazione/cancellazione commessa;
- cambio assegnazione;
- cambio stato;
- reset dati, scritto prima della cancellazione totale locale.

Limiti attuali:

- CI Android tracciata in `.github/workflows/android.yml` per lint/compile/test JVM e in `.github/workflows/android-instrumented.yml` per `:app:connectedFullDebugAndroidTest` su emulatore;
- la CI non genera release firmate perche' il signing resta locale;
- nessun backend o invio WhatsApp API.
- eventuali provider Drive/OneDrive/Dropbox sono solo destinazioni SAF esposte dal device, non integrazioni cloud proprietarie.

## Build

Flavor Gradle correnti:

- `full`
- `demo`
- `showcase`

Output principali:

- APK debug Full: `app/build/outputs/apk/full/debug/app-full-debug.apk`;
- APK debug Demo: `app/build/outputs/apk/demo/debug/app-demo-debug.apk`;
- APK debug Showcase: `app/build/outputs/apk/showcase/debug/app-showcase-debug.apk`;
- APK release Full: `app/build/outputs/apk/full/release/app-full-release.apk`;
- AAB release Full: `app/build/outputs/bundle/fullRelease/app-full-release.aab`.

## Configurazione Gradle

Usare Gradle Version Catalog.

File:

```text
gradle/libs.versions.toml
```

Evitare numeri versione sparsi nei file Gradle.
