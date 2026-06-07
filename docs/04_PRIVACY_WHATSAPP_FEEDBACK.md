# 04 - Privacy, WhatsApp e feedback cliente

## Template modificabili

L'admin puo' modificare i template salvati in Room tramite Config > Template.

Template predefiniti implementati:

- `privacy_breve`;
- `condizioni_presa_in_carico`;
- `whatsapp_presa_in_carico`;
- `whatsapp_aggiornamento`;
- `whatsapp_diagnosi`;
- `whatsapp_risultato`;
- `whatsapp_riparazione`;
- `whatsapp_lavorazione`;
- `whatsapp_completamento`;
- `whatsapp_pronto_ritiro`.

Ogni template salva:

```text
code
title
channel
body
active
version
updatedAt
updatedByUserId
```

Ogni salvataggio incrementa la versione del template esistente.

## Firma

La firma viene acquisita con canvas Compose nel modulo Nuova commessa. Nel dettaglio commessa le firme gia' salvate sono mostrate in sola lettura.
Privacy e condizioni si aprono in dialog cliccando sui rispettivi testi prima dell'accettazione.
Ogni commessa accetta una sola firma cliente.

Alla firma vengono salvati:

- immagine PNG della firma;
- accettazione privacy;
- accettazione condizioni;
- snapshot testo privacy;
- versione privacy;
- snapshot testo condizioni;
- versione condizioni;
- id commessa;
- operatore;
- data/ora.

I file sono salvati in directory privata app:

```text
files/orders/<orderId>/signatures/
```

Il salvataggio crea anche evento timeline "Firma acquisita".

## Variabili template

Variabili supportate:

```text
{{numero_commessa}}
{{titolo_commessa}}
{{stato_commessa}}
{{cliente}}
```

Template con variabili diverse vengono rifiutati al salvataggio. L'anteprima WhatsApp nel dettaglio commessa mostra il testo renderizzato e modificabile prima dell'apertura di WhatsApp.

## WhatsApp manuale

La sola modalita implementata e' `MANUAL_WHATSAPP`.

Flusso:

```text
Operatore apre una commessa
Prepara il messaggio WhatsApp
L'app renderizza il template con i dati commessa
Operatore modifica il testo se necessario
Operatore apre WhatsApp
Operatore invia manualmente
Operatore torna in app e conferma "Inviato"
Timeline e message log vengono aggiornati
```

La preparazione registra `WHATSAPP_PREPARED`; la conferma manuale registra `WHATSAPP_SENT_CONFIRMED`.

## Feedback cliente

Il modello dati supporta feedback su timeline e message log:

```text
NONE
READ
ACCEPT
REJECT
ASK_INFO
CALL_ME
CUSTOM
```

Stato operativo implementato:

- non esiste una schermata dedicata "Registra feedback";
- non esiste lettura automatica delle risposte WhatsApp;
- l'accettazione/rifiuto viene registrata dalle azioni operative del dettaglio commessa;
- `ACCEPT` porta lo stato a `REPAIRING`;
- `REJECT` porta lo stato a `CLOSED` con motivo `CUSTOMER_DECLINED`.

La chiusura operativa usa un solo pulsante "Chiudi commessa" e richiede motivo esplicito e nota manuale. Dopo una chiusura riuscita l'app torna all'elenco commesse.

## API WhatsApp non implementata

Non sono presenti backend, WhatsApp Business Platform, webhook, token o invio API. Non salvare token sensibili nell'app installata su device. Per introdurre una modalita API serve un backend dedicato.
