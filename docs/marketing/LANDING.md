# Landing Statica

La cartella `landing/` contiene una bozza HTML/CSS/JS statica. Non richiede build tool o server side logic.

## Stato Reale

- `index.html`: pagina statica.
- `styles.css`: stili della landing.
- `script.js`: smooth scroll e validazione base del form.
- `assets/brand/`: logo e marchio duplicati da `media/brand/identity`.
- `assets/icons/`: favicon e anteprima icona duplicate da `media/brand/icons`.
- `assets/social/`: cover e social preview duplicate da `media/brand/social`.
- `assets/screenshots/`: soli screenshot applicativi approvati per la pubblicazione.

La landing non pubblica APK, non contiene link Play Store reali e non contiene video. Prima della pubblicazione vanno verificati contatti, link, privacy policy e canale di distribuzione.

## Uso Locale

Aprire `landing/index.html` nel browser. Non serve avviare un dev server.

## Screenshot e Video

Per sostituire i placeholder:

1. Generare screenshot reali con `scripts\powershell\assets\create_demo_assets.ps1 -Flavor Full` oppure da device reale. Gli output locali sono in `reports\demo-assets\Full\screenshots`.
2. Copiare intenzionalmente le sole immagini approvate sotto `landing\assets\screenshots\`.
3. Aggiornare `index.html` sostituendo i blocchi `screenshot-placeholder` con tag `img`.
4. Aggiungere un video locale o un embed solo quando il file/link è reale e approvato.

## Build App Collegate

La landing non genera installer. Usare gli script Android documentati in [`../operations/SCRIPTS.md`](../operations/SCRIPTS.md):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\powershell\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk
```

Le build release richiedono `keystore.properties` locale non tracciato.
