# Pubblicazione Google Play - OnlyTechTake Pro

## Identità

- Flavor Gradle: `full`
- Package: `com.onlytechtake.repairapp`
- Nome Play: `OnlyTechTake Pro`
- Prezzo base pianificato: `19,99 EUR`
- Sito sviluppatore: lasciare vuoto, non esiste.
- Privacy policy: inserire solo un URL HTTPS reale della privacy policy quando disponibile.

## Build

```powershell
powershell -ExecutionPolicy Bypass -File scripts\powershell\android\check_env.ps1
powershell -ExecutionPolicy Bypass -File scripts\powershell\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Aab
```

Output atteso:

```text
app\build\outputs\bundle\fullRelease\app-full-release.aab
```

## Play Console

1. Creare l'app come app a pagamento prima della pubblicazione.
2. Configurare Play App Signing e caricare l'AAB firmato.
3. Completare App Access: su installazione pulita il revisore crea un admin locale e accede con quelle credenziali.
4. Compilare Ads: no annunci.
5. Compilare Data Safety come app local-only, senza raccolta server proprietaria.
6. Compilare Content Rating come app gestionale/produttività, comunicazione solo tramite WhatsApp manuale.
7. Caricare listing, icona 512x512, feature graphic 1024x500 e screenshot Pro.
8. Eseguire Internal testing e attendere Pre-launch Report senza crash bloccanti.

## Gate prima produzione

- Test unitari Full verdi.
- AAB Full release generato e accettato.
- Privacy policy definitiva pubblicata a URL HTTPS reale.
- Nessun sito sviluppatore inventato nei campi Console o nei testi listing.

## Asset statici

Vedi [`../marketing/ASSETS.md`](../marketing/ASSETS.md).
