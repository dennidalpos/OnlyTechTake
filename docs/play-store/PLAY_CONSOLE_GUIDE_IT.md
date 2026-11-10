# Guida Play Console

Riferimento per Full e Demo. La preparazione locale non compila i moduli in Console. Usare le [fonti e la checklist release](release-checklist.md), verificate il 2026-10-07.

## Accesso all'app

Selezionare accesso limitato e spiegare il login locale. Su installazione pulita Full mostra il setup: il revisore crea un admin locale e accede con le credenziali appena create. Non fornire credenziali di negozi reali; non è richiesto un account su un server.

Demo consente setup admin oppure il pulsante di avvio demo. Il codice configura una commessa e fino a tre operatori attivi; export e restore sono limitati e i PDF hanno watermark. Showcase è destinato a presentazioni fuori Play. Ricontrollare l'effettivo flusso sull'AAB installato prima di salvare App Access.

## Annunci, pubblico e classificazione

- Annunci: assenti nel codice esaminato.
- Pubblico previsto: professionisti e laboratori; confermare le fasce effettive in Console.
- Content Rating: gestionale/produttività, con apertura manuale di WhatsApp. Rispondere al questionario effettivo senza equiparare questa apertura a un servizio chat proprietario.
- News, COVID e servizi governativi: non sono funzionalità dell'app.

## Data Safety

Seguire soltanto la [guida canonica](DATA_SAFETY_GUIDE.md). Il trattamento locale non si dichiara come "raccolta locale". La risposta proposta è "No" a raccolta/condivisione solo dopo aver confermato il comportamento degli SDK release e l'eccezione per i trasferimenti manuali.

Non dichiarare cifratura in transito sulla base del database SQLCipher. La password ZIP attuale cifra solo il database; configurazione, media e DataStore restano in chiaro, mentre XLSX/PDF/JSON singoli non sono cifrati dal percorso di export. I limiti sono descritti nel [flusso backup](../BACKUP_FLOW.md).

Il reset admin riguarda dati locali, non copie esportate o condivise. Gli account operatori sono locali. Per un'app distribuita esclusivamente nel test interno, Google esenta dalla sezione Data Safety; ricontrollare i requisiti prima di cambiare canale.

## Privacy policy e permessi

Finalizzare contatto privacy del publisher e URL HTTPS pubblico reale nella [bozza policy](PRIVACY_POLICY_IT.md), con accesso alla policy anche dall'app. Non inserire URL o dati del titolare inventati. Un sito sviluppatore è un campo distinto dall'URL policy.

Il manifest principale richiede fotocamera e microfono (`CAMERA`, `RECORD_AUDIO`) per foto e video con audio. Non richiede permessi generali di storage o `INTERNET`. Controllare il manifest release unito e le eventuali dichiarazioni richieste da Console dopo l'upload.

`android:hasFragileUserData="true"` può offrire la conservazione dei dati durante la disinstallazione sui sistemi supportati; non garantisce recupero o migrazione. La chiave SQLCipher dipende dal Keystore locale: non promettere ripristino su un altro dispositivo o dopo perdita della chiave.
