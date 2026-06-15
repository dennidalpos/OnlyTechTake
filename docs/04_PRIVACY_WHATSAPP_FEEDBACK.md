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
- `whatsapp_preventivo_intervento`;
- `whatsapp_non_riparabile`;
- `whatsapp_riparazione`;
- `whatsapp_lavorazione`;
- `whatsapp_completamento`;
- `whatsapp_pronto_ritiro`;
- `whatsapp_stato_avanzamento_pdf`.

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

La firma viene acquisita con canvas Compose nel modulo Nuova commessa. Nel dettaglio commessa la firma salvata e' mostrata in sola lettura.
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
files/app_media/signatures/<orderId>/
```

Il salvataggio crea anche evento timeline "Firma acquisita" (`SIGNATURE_CAPTURED`).

## Variabili template

Variabili supportate:

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

Template con variabili diverse vengono rifiutati al salvataggio. L'anteprima WhatsApp e' confinata al dettaglio della commessa corrente, e' modificabile e puo' essere chiusa senza invio.

## WhatsApp manuale

La sola modalita implementata e' `MANUAL_WHATSAPP`.

Flusso:

```text
Operatore apre una commessa
Prepara il messaggio WhatsApp con il pulsante unico
L'app sceglie il template in base allo stato corrente e ai dati della commessa
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
```

Stato operativo implementato:

- non esiste una schermata dedicata "Registra feedback";
- non esiste lettura automatica delle risposte WhatsApp;
- l'accettazione/rifiuto viene registrata dalle azioni operative del dettaglio commessa;
- `whatsapp_preventivo_intervento` e' usato solo per `REPAIRABLE` e include `importo_preventivo`;
- `whatsapp_non_riparabile` e' usato per `NOT_REPAIRABLE` gia' in `RESULT`, senza testo vuoto legato al motivo chiusura;
- `ACCEPT` porta lo stato a `REPAIRING`;
- `REJECT` porta lo stato a `CLOSED` con motivo `CUSTOMER_DECLINED`.

La preview viene eliminata su chiusura, cambio commessa, ritorno all'elenco,
cambio stato, feedback cliente e conferma invio. L'apertura di `wa.me` mantiene
la preview per consentire all'operatore di confermare manualmente l'invio al
ritorno. L'app non invia automaticamente.

- una chiusura come `NOT_REPAIRABLE` usa il template dedicato `whatsapp_non_riparabile`;
- un risultato con importo usa il template `whatsapp_preventivo_intervento` e resta in attesa di conferma cliente.

## Condivisione PDF di stato

Oltre a WhatsApp manuale, l'app permette di generare e condividere un PDF di riepilogo dello stato della commessa.
Il file viene salvato temporaneamente in `documents/` e condiviso tramite `FileProvider` con le app di sistema. La condivisione registra l'evento `PDF_STATUS_SHARED` nella timeline.

La chiusura operativa usa un solo pulsante "Chiudi commessa" e richiede motivo esplicito e nota manuale. Dopo una chiusura riuscita l'app torna all'elenco commesse.

## API WhatsApp non implementata

Non sono presenti backend, WhatsApp Business Platform, webhook, token o invio API. Non salvare token sensibili nell'app installata su device. Per introdurre una modalita API serve un backend dedicato.
