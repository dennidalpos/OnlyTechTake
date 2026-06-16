# Marketing e asset demo

Questa cartella contiene solo indicazioni operative per produrre asset marketing reali. Il flusso tecnico di generazione screenshot/video resta documentato in [`../DEMO_ASSET_WORKFLOW.md`](../DEMO_ASSET_WORKFLOW.md).

## Asset generati

Gli output locali sono ignorati da Git e vengono scritti sotto:

```text
reports/demo-assets/screenshots
reports/demo-assets/videos
```

Comando principale:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_assets.ps1
```

Registrazione device:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -ScenarioId full-workflow -StartImmediately
```

## Regole contenuto

- Usare solo dati fittizi o seed demo.
- Non mostrare account reali, notifiche personali, rubrica o chat vere.
- Descrivere WhatsApp come preparazione e invio manuale, non come automazione.
- Usare CTA coerenti con il canale reale: non scrivere "Disponibile su Google Play" finche' la pubblicazione non e' effettiva.
- Verificare dimensioni e leggibilita' degli asset prima del caricamento store.

## Copy suggerito

- Dashboard: "Tutto sotto controllo."
- Nuova commessa: "Presa in carico guidata."
- Firma e media: "Documentazione completa."
- WhatsApp: "Messaggio pronto per invio manuale."
- Backup: "Backup ZIP e export XLSX."

