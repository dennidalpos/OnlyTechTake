# Landing Page — OnlyTechTake

Pagina statica semplice per la presentazione e il download dell'app OnlyTechTake.

## Struttura
- `index.html`: Struttura principale della pagina.
- `styles.css`: Stili (Mobile-first, Material-inspired).
- `script.js`: Piccole interazioni (smooth scroll).
- `assets/screenshots/`: Cartella per gli screenshot dell'app.

## Personalizzazione

### Modificare il Prezzo
Cerca la sezione `<section id="prezzo">` in `index.html` e modifica i testi all'interno della classe `.price` e `.pricing-card`.

### Aggiungere Screenshot
1. Cattura gli screenshot dall'emulatore o dal dispositivo reale (formato 9:16).
2. Salvali nella cartella `assets/screenshots/` con i seguenti nomi:
   - `01-dashboard.png`
   - `02-nuova-commessa.png`
   - `03-firma.png`
   - `04-foto.png`
   - `05-diagnosi.png`
   - `06-pdf.png`
3. Sostituisci i placeholder `div.screenshot-placeholder` in `index.html` con tag `<img>`:
   ```html
   <img src="assets/screenshots/01-dashboard.png" alt="Dashboard OnlyTechTake">
   ```

### Aggiornare Link APK
Trova i pulsanti con testo "Scarica APK" e aggiorna l'attributo `href` con il link diretto al file `.apk` (es. caricato su GitHub Releases o Firebase App Distribution).

### Video Demo
Sostituisci il contenuto di `.video-container` in `index.html` con un iframe di YouTube o un tag `<video>` locale.

## Pubblicazione

### GitHub Pages (Consigliato)
1. Vai nelle impostazioni del repository su GitHub.
2. Sezione **Pages**.
3. Seleziona la cartella `/landing` nel branch principale (se supportato) oppure sposta questi file in un branch separato `gh-pages`.
4. In alternativa, se il repository è solo l'app, puoi usare un'azione GitHub per distribuire la sottocartella `landing/`.

### Netlify / Vercel
1. Collega il repository.
2. Imposta la "Base directory" su `landing/`.
3. Lascia vuoto il comando di build (è un sito statico).
4. La directory di output è la stessa (`.`).

## Requisiti
Nessuno. La landing è pura HTML/CSS/JS e non richiede server side logic o build tools.
