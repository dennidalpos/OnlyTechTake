# Landing statica

La cartella `landing/` contiene una bozza HTML/CSS/JS statica. Non richiede build tool o server side logic.

## Stato reale

- `index.html`: pagina statica.
- `styles.css`: stili della landing.
- `script.js`: smooth scroll e validazione base del form.
- `assets/`: presente solo con `.gitkeep`.
- `assets/screenshots/`: non presente nel repository corrente.

La landing non pubblica APK, non contiene link Play Store reali, non contiene video e non contiene contatti pubblici verificati. Prima della pubblicazione vanno aggiunti asset reali e link approvati.

## Uso locale

Aprire `landing/index.html` nel browser. Non serve avviare un dev server.

## Screenshot e video

Per sostituire i placeholder:

1. Generare screenshot reali con `scripts\windows\create_demo_screenshots.ps1` oppure da device reale.
2. Salvare immagini tracciabili sotto `landing\assets\screenshots\`.
3. Aggiornare `index.html` sostituendo i blocchi `screenshot-placeholder` con tag `img`.
4. Aggiungere un video locale o un embed solo quando il file/link e' reale e approvato.

## Build app collegate

La landing non genera installer. Usare gli script Android documentati nella root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk
```

Le build release richiedono `keystore.properties` locale non tracciato.
