# Marketing e asset demo

Questa cartella contiene solo indicazioni operative per produrre asset marketing reali. Il flusso tecnico di generazione screenshot/video resta documentato in [`../DEMO_ASSET_WORKFLOW.md`](../DEMO_ASSET_WORKFLOW.md).

## Asset generati

Gli asset statici Play tracciati sono in:

```text
media/play-store/pro/icon-512.png
media/play-store/pro/feature-graphic-1024x500.png
media/play-store/demo/icon-512.png
media/play-store/demo/feature-graphic-1024x500.png
media/play-store/showcase/icon-512.png
media/play-store/showcase/feature-graphic-1024x500.png
```

Pro e Demo sono per Google Play; Showcase resta per presentazioni commerciali fuori store.

Gli output locali di screenshot e video sono ignorati da Git e vengono scritti sotto:

```text
reports/demo-assets/Full/screenshots
reports/demo-assets/Full/videos
reports/demo-assets/Demo/screenshots
reports/demo-assets/Demo/videos
```

Comando principale:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_assets.ps1
powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_assets.ps1 -Flavor Demo
```

Registrazione device:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -ScenarioId full-workflow -StartImmediately
powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -Flavor Demo -ScenarioId demo-limit -StartImmediately
```

## Regole contenuto

- Usare solo dati fittizi o seed demo.
- Non mostrare account reali, notifiche personali, rubrica o chat vere.
- Descrivere WhatsApp come preparazione e invio manuale, non come automazione.
- Usare CTA coerenti con il canale reale: non scrivere "Disponibile su Google Play" finche' la pubblicazione non e' effettiva.
- Non inserire sito web o URL pubblico inventato negli asset: al momento non esiste un sito.
- Verificare dimensioni e leggibilita' degli asset prima del caricamento store.

## Copy suggerito

- Dashboard: "Tutto sotto controllo."
- Nuova commessa: "Presa in carico guidata."
- Firma e media: "Documentazione completa."
- WhatsApp: "Messaggio pronto per invio manuale."
- Backup: "Backup ZIP e export XLSX."
