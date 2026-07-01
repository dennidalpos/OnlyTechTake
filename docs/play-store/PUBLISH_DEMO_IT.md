# Pubblicazione Google Play - OnlyTechTake Demo

## Identità

- Flavor Gradle: `demo`
- Package: `com.onlytechtake.repairapp.demo`
- Nome Play: `OnlyTechTake Demo`
- Prezzo: gratuito.
- Sito sviluppatore: lasciare vuoto, non esiste.
- Privacy policy: inserire solo un URL HTTPS reale della privacy policy quando disponibile.

## Limiti da dichiarare nel listing

- Massimo 1 commessa totale.
- Nessun account operatore: uso solo con admin locale.
- Export database bloccato.
- Backup ZIP completo bloccato.
- Ripristino ZIP bloccato.
- Export XLSX bloccato.
- PDF con watermark `VERSIONE DEMO`.

## Build

```powershell
powershell -ExecutionPolicy Bypass -File scripts\powershell\android\check_env.ps1
powershell -ExecutionPolicy Bypass -File scripts\powershell\android\build_installer.ps1 -Flavor Demo -BuildType Release -OutputType Aab
```

Output atteso:

```text
app\build\outputs\bundle\demoRelease\app-demo-release.aab
```

## Play Console

1. Creare un'app Play separata gratuita.
2. Configurare Play App Signing e caricare l'AAB Demo firmato.
3. Completare App Access: il revisore può creare un admin locale o usare `Avvia modalità demo`; indicare chiaramente che il flavor Demo è admin-only.
4. Compilare Ads: no annunci.
5. Compilare Data Safety come app local-only, senza raccolta server proprietaria.
6. Compilare Content Rating come app gestionale/produttività, comunicazione solo tramite WhatsApp manuale.
7. Caricare listing, icona 512x512, feature graphic 1024x500 e screenshot Demo che mostrino i limiti reali.
8. Eseguire Internal testing e attendere Pre-launch Report senza crash bloccanti.

## Gate prima produzione

- Test unitari Demo verdi.
- AAB Demo release generato e accettato.
- Listing coerente con i limiti implementati nel codice.
- Nessun sito sviluppatore inventato nei campi Console o nei testi listing.

## Asset statici

Vedi [`../../marketing/README.md`](../../marketing/README.md).
