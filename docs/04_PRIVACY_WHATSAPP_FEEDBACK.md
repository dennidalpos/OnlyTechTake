# 04 - Privacy, WhatsApp e feedback cliente

## Testi modificabili dall'admin

L'admin può modificare:

- informativa privacy breve;
- condizioni presa in carico;
- messaggio presa in carico;
- messaggio diagnosi;
- messaggio preventivo;
- messaggio accettazione preventivo;
- messaggio in lavorazione;
- messaggio completato;
- messaggio pronto al ritiro.

## Versionamento testi

Ogni testo ha:

```text
code
title
body
version
updatedAt
updatedByUserId
active
```

Quando l'admin modifica un testo:

- non sovrascrivere la versione usata da firme passate;
- incrementare la versione;
- salvare snapshot del testo firmato nella firma.

## Firma

Alla firma salvare:

- immagine firma;
- nome firmatario;
- data/ora;
- operatore;
- testo privacy usato;
- testo condizioni usato;
- versione privacy;
- versione condizioni;
- id commessa.

Nell'MVP Android la firma viene acquisita con un canvas Compose nel dettaglio
commessa, salvata come PNG nella directory privata
`files/orders/<orderId>/signatures/` e registrata nella tabella Room
`signatures`. Il salvataggio crea anche evento timeline "Firma acquisita".

## Variabili template

Variabili supportate:

```text
{{numero_commessa}}
{{titolo_commessa}}
{{stato_commessa}}
{{cliente}}
```

Template con variabili diverse da questo elenco vengono rifiutati.

## Modalità WhatsApp

### Manuale, default

Flusso:

```text
Operatore cambia stato
↓
L'app propone messaggio
↓
Operatore può modificare il testo
↓
Operatore invia il messaggio con WhatsApp
↓
Si apre WhatsApp con testo e destinatario
↓
Operatore invia
↓
Ritorna all'app
↓
L'app chiede "Messaggio inviato?"
↓
Se confermato, timeline aggiornata
```

### API WhatsApp

Non supportata in questo rilascio.

Non salvare token sensibili nell'app installata su device. Per una modalità API serve un backend.

## Feedback cliente

Ogni evento timeline può avere feedback richiesto.

Tipi:

```text
NONE
READ
ACCEPT
REJECT
ASK_INFO
CALL_ME
CUSTOM
```

## Mappatura stati / feedback

| Stato | Feedback consigliato |
|---|---|
| Presa in carico | READ |
| In diagnosi | READ |
| Preventivo inviato | ACCEPT / REJECT / ASK_INFO |
| In attesa cliente | ACCEPT / REJECT / ASK_INFO |
| In lavorazione | READ |
| In attesa ricambio | READ / ASK_INFO |
| Completato | READ |
| Non completabile | READ / ASK_INFO |
| Pronto al ritiro | READ |
| Consegnato | NONE |
| Chiuso | NONE |

## Messaggi manuali con codici risposta

Per la modalità manuale, il cliente risponde in chat WhatsApp.

Esempio preventivo compatibile con le variabili supportate:

```text
Ciao {{cliente}},
il preventivo per la commessa {{numero_commessa}} e pronto per conferma.

Rispondi con ACCETTO, RIFIUTO o INFO indicando il numero commessa.
```

L'operatore registra manualmente la risposta.

## Schermata "Registra feedback"

Campi:

- tipo feedback;
- nota;
- data/ora automatica;
- operatore;
- collegamento a messaggio/timeline.

Pulsanti rapidi:

```text
Letto
Accetta preventivo
Rifiuta preventivo
Chiede informazioni
Richiamare
Altro
```
