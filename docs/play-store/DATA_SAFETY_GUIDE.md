# Google Play Data Safety — OnlyTechTake

Fonte ufficiale verificata il 2026-10-07: [Google Play Data safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en). Guida canonica per Full e Demo, pubblicati con package separati. Ricontrollare l'artefatto finale e il comportamento degli SDK prima dell'invio.

## Raccolta e condivisione

Google definisce raccolta la trasmissione di dati fuori dal dispositivo. Il trattamento esclusivamente locale è escluso. Il codice esaminato non integra backend proprietari o analytics; il manifest principale non richiede `INTERNET`. Questa verifica del sorgente non sostituisce l'esame del manifest release unito e degli SDK inclusi.

Un trasferimento a un'altra app può costituire condivisione anche sullo stesso dispositivo. Google esclude i trasferimenti attivati da un'azione specifica dell'utente quando questi si aspetta ragionevolmente la condivisione. OnlyTechTake apre WhatsApp con numero e testo, condivide PDF tramite Android e scrive esportazioni nella destinazione scelta con il selettore documenti. Il provider scelto può trasferire file nel cloud. Verificare l'eccezione per ogni flusso: la sola assenza di invio automatico non basta.

Per il comportamento esaminato, la risposta proposta a **L'app raccoglie o condivide i tipi di dati utente richiesti?** è **No**, a condizione che la verifica degli SDK release confermi l'assenza di altra raccolta e i trasferimenti manuali rispettino l'eccezione. Non dichiarare "Sì, in locale" come raccolta.

Le app distribuite esclusivamente nel test interno sono esenti dalla sezione Data Safety. Completare modulo e privacy policy pubblica prima della distribuzione sui canali che li richiedono.

## Dati trattati localmente

| Dati | Finalità |
|---|---|
| Nome, ragione sociale, identificativi fiscali, indirizzo, email e telefono | Anagrafica clienti e comunicazioni manuali |
| Descrizioni, note, preventivi e stato pagamento | Gestione commesse |
| Foto e video, con audio del microfono quando abilitato | Documentazione tecnica |
| Firme autografe su schermo | Accettazione e consensi |
| Credenziali locali operatori e audit | Autenticazione e cronologia operazioni |
| PDF, XLSX, configurazione e ZIP | Documenti ed esportazioni manuali |

La tabella descrive il trattamento locale per la privacy policy; non è un elenco da selezionare come dati raccolti.

## Sicurezza e cancellazione

- SQLCipher cifra il database a riposo. Media, firme, PDF e DataStore sono file privati separati: memoria privata non significa cifratura dei file.
- La password ZIP attuale cifra solo le voci database. Configurazione, media e DataStore restano in chiaro. Il percorso attuale di esportazione XLSX e i PDF/JSON singoli non applicano cifratura con password. Vedere [limiti del backup](../BACKUP_FLOW.md).
- Non usare SQLCipher come prova di cifratura in transito. Dopo la risposta "No" a raccolta/condivisione, Google salta le domande dettagliate sulla sicurezza della raccolta. Se altri trasferimenti richiedono "Sì", valutarne la protezione di trasporto effettiva.
- Reset e cancellazione admin riguardano i dati locali. Copie esportate, ricevute da terzi o conservate dai provider cloud richiedono cancellazione separata. Gli account operatori sono locali, non account cloud dello sviluppatore.
- Il manifest principale richiede `CAMERA` e `RECORD_AUDIO`. Verificare anche i permessi del manifest release unito.

## Compilazione

Usare la [guida Play Console](PLAY_CONSOLE_GUIDE_IT.md) dopo aver verificato l'artefatto release. Finalizzare contatto privacy del publisher e URL HTTPS pubblico, senza inventare riferimenti. Mantenere coerenti policy, accesso alla policy nell'app e dichiarazioni.
