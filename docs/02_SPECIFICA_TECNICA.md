# 02 - Specifica tecnica

## Stack effettivo

- Kotlin
- Jetpack Compose
- Material 3
- Android Architecture Components
- Room
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

Database locale Room.

Nome database:

```text
repair_app.db
```

Versionamento database obbligatorio.

Abilitare migrazioni Room quando cambia lo schema.

## Entità principali

- UserEntity
- CustomerEntity
- RepairOrderEntity
- OrderMediaEntity
- SignatureEntity
- TimelineEventEntity
- MessageTemplateEntity
- MessageLogEntity
- AppSettingEntity
- AuditLogEntity
- OrderMediaEntity, per foto CameraX locali collegate alla commessa

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
WorkspaceViewModel
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

## Permessi runtime Android

Permessi manifest:

- Camera;
- nessun permesso storage globale.

Import allegati ed export usano Storage Access Framework, quindi non richiedono
permessi larghi di lettura/scrittura storage.

## Foto e firme

Salvare file in directory private app:

```text
files/order_media/<orderId>/
files/orders/<orderId>/signatures/
files/orders/<orderId>/documents/
```

Nel database salvare path locale privato e metadati essenziali. Le foto acquisite
con CameraX sono registrate in `order_media` e generano evento timeline. Le foto
opzionali del lavoro finito usano `mediaType = FINAL_PHOTO` nello stesso modello,
senza schema dedicato. Le
firme sono registrate in `signatures`, salvate come PNG in
`files/orders/<orderId>/signatures/` e includono snapshot/versione dei testi
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

## Export

L'export completo crea ZIP con:

```text
metadata/backup.json
config/configuration.json
database/repair_app.db
database/repair_app.db-*
files/orders/...
files/order_media/...
```

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
```

Variabili diverse da questo elenco devono essere rifiutate al salvataggio o al
rendering del template.

Implementare un semplice motore replace controllato.

Regole implementate:

- i template con variabili fuori allowlist vengono rifiutati al salvataggio;
- il rendering fallisce se riceve una variabile non supportata;
- l'anteprima del messaggio e' disponibile nel dettaglio commessa prima dell'apertura WhatsApp.

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

- nessun ripristino/import backup;
- nessuna cifratura applicativa degli export;
- nessuna CI tracciata nel repository;
- nessun backend o invio WhatsApp API.

## Build

Output richiesti:

- APK debug per test;
- APK release firmato per installazione diretta;
- AAB release generabile con lo script PowerShell ufficiale.

## Configurazione Gradle

Usare Gradle Version Catalog.

File:

```text
gradle/libs.versions.toml
```

Evitare numeri versione sparsi nei file Gradle.
