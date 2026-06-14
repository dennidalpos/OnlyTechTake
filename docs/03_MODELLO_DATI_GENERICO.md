# 03 - Modello dati generico

## Obiettivo

Tenere la commessa generica e prevedibile.

La commessa contiene dati base minimi. I dati specifici della lavorazione si
salvano in descrizione, note interne, media, firme, timeline e messaggi.

## Schema concettuale

```text
Customer
  ↓
RepairOrder
  ↓
Media
  ↓
Signature
  ↓
TimelineEvent
  ↓
MessageLog
  ↓
OrderDocument
```

## Customer

```kotlin
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val type: CustomerType,
    val firstName: String?,
    val lastName: String?,
    val companyName: String?,
    val vatNumber: String?,
    val taxCode: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

## RepairOrder

Campi base, volutamente generici:

```kotlin
@Entity(tableName = "repair_orders")
data class RepairOrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val customerId: String,
    val title: String,
    val categoryId: String,
    val status: OrderStatus,
    val priority: OrderPriority,
    val assignedToUserId: String?,
    val createdByUserId: String,
    val description: String?,
    val internalNotes: String? = null,
    val diagnosisOutcome: DiagnosisOutcome? = null,
    val estimateAmountCents: Long? = null,
    val finalAmountCents: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long? = null,
    val closureReason: ClosureReason? = null,
    val customerActionStatus: CustomerActionStatus? = null,
    val closeNote: String? = null,
    val closedByUserId: String? = null,
    val categoryPathSnapshot: String? = null,
    val lastCustomerContactAt: Long? = null,
    val deletedAt: Long? = null,
    val deletedByUserId: String? = null,
    val deleteReason: String? = null
)
```

`DiagnosisOutcome` ammette solo `REPAIRABLE` e `NOT_REPAIRABLE`. Non viene dedotto
dall'importo: il preventivo deve essere positivo nel primo ramo ed e' sempre
azzerato nel secondo. `RESULT` rappresenta l'attesa del riscontro cliente o
dell'avviso per il ritiro, senza introdurre uno stato `WAITING_CUSTOMER`.

## OrderCategory

```kotlin
@Entity(tableName = "order_categories")
data class OrderCategoryEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val level: Int,
    val name: String,
    val description: String?,
    val active: Boolean
)
```

Esempi categorie:

- generica;
- smartphone;
- computer;
- sartoria;
- elettrodomestico;
- altro.

L'admin può rinominarle, crearne di nuove o archiviarle.

## Media

```kotlin
@Entity(tableName = "order_media")
data class OrderMediaEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val mediaType: String,
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val createdByUserId: String?,
    val capturedAt: Long,
    val createdAt: Long
)
```

Le foto CameraX vengono salvate nello spazio privato app sotto
`files/order_media/<orderId>/` e collegate alla timeline con evento
`MEDIA_ADDED`.

## Signature

```kotlin
@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val signerName: String,
    val privacyAccepted: Boolean,
    val termsAccepted: Boolean,
    val privacyTextSnapshot: String,
    val privacyTemplateVersion: Int,
    val termsTextSnapshot: String,
    val termsTemplateVersion: Int,
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val createdByUserId: String?,
    val signedAt: Long,
    val createdAt: Long
)
```

## TimelineEvent

```kotlin
@Entity(tableName = "timeline_events")
data class TimelineEventEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val type: TimelineEventType,
    val title: String,
    val description: String?,
    val oldStatus: String?,
    val newStatus: String?,
    val visibleToCustomer: Boolean,
    val requiresCustomerFeedback: Boolean,
    val expectedFeedbackType: CustomerFeedbackType?,
    val customerFeedback: CustomerFeedbackType?,
    val customerFeedbackNote: String?,
    val customerFeedbackAt: Long?,
    val createdByUserId: String,
    val createdAt: Long
)
```

## MessageTemplate

```kotlin
@Entity(tableName = "message_templates")
data class MessageTemplateEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val channel: MessageChannel,
    val body: String,
    val active: Boolean,
    val version: Int,
    val updatedByUserId: String?,
    val updatedAt: Long
)
```

## MessageLog

```kotlin
@Entity(tableName = "message_logs")
data class MessageLogEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val timelineEventId: String?,
    val customerId: String,
    val channel: MessageChannel,
    val mode: MessageMode,
    val body: String,
    val status: MessageStatus,
    val expectedReply: CustomerFeedbackType?,
    val actualReply: CustomerFeedbackType?,
    val actualReplyNote: String?,
    val createdByUserId: String,
    val createdAt: Long,
    val sentAt: Long?
)
```

## OrderDocument

```kotlin
@Entity(tableName = "order_documents")
data class OrderDocumentEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val type: OrderDocumentType,
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val includesCustomerSignature: Boolean,
    val createdByUserId: String,
    val createdAt: Long,
    val sharedAt: Long?
)
```

Tipo documento implementato:

```text
STATUS_PROGRESS_PDF
```

I PDF di stato vengono salvati nello spazio privato app sotto
`files/orders/<orderId>/documents/` e condivisi tramite `FileProvider`.

## User

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: UserRole,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

## AppSetting

```kotlin
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedByUserId: String?,
    val updatedAt: Long
)
```

## Regola importante sui dati commessa

Non usare una tabella diversa per ogni tipo di negozio.
Non aggiungere colonne tipo `imei`, `marca`, `modello`, `taglia`, `colore`, ecc. nella tabella commesse.

Usare categorie, descrizione, note interne, media, firme, timeline e messaggi.
Lo schema Room corrente nel codice e' v2, con export tracciati in
`app/schemas/com.onlytechtake.repairapp.data.local.db.RepairAppDatabase/1.json`
e `app/schemas/com.onlytechtake.repairapp.data.local.db.RepairAppDatabase/2.json`.
Non sono presenti classi di migrazione Room: in questa fase fresh-app, dati
locali incompatibili richiedono reinstallazione pulita o vengono gestiti dal
fallback distruttivo configurato nel database builder. Non include tabelle custom
field o workflow parallele a `repair_orders.status`.
