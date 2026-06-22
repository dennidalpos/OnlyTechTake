# Media & Play Store Assets

Asset visuali canonici del progetto, separati per destinazione, e materiali per Google Play e presentazioni commerciali.

## Struttura Media

```text
media/brand/identity/   logo e marchio SVG
media/brand/icons/      favicon, ICO e anteprima icona
media/brand/social/     cover GitHub e social preview
media/play-store/       icone e feature graphic per variante
```

Le copie necessarie alla landing sono pubblicate sotto `landing/assets` con la stessa separazione `brand`, `icons` e `social`. Non modificare manualmente le copie: sincronizzare tutto tramite gli script di progetto quando richiesto.

## Play Store Assets

Gli asset usano il marchio `OnlyTechTakeMark`, la palette Material 3 dell'app e screenshot reali generati dalla pipeline asset salvati in `reports/generated-assets/`.

Prima di aggiornare gli asset devono esistere gli screenshot Full/Pro e, solo per la pubblicazione Demo, gli screenshot Demo sotto `reports/generated-assets/`.

Gli asset generali non destinati allo store sono separati sotto `media/brand/identity`, `media/brand/icons` e `media/brand/social`.

## Ruoli ufficiali

- **Full / Pro**: fonte degli asset principali Play Store, social, video promozionali e materiali vendita. I dati visualizzati devono essere realistici ma neutri.
- **Demo**: fonte di pochi asset secondari per la prova limitata. Deve mostrare limiti reali e funzioni Pro non disponibili.
- **Showcase**: fonte per presentazioni guidate, walkthrough e registrazioni controllate fuori dallo store principale.

### Screenshot & Video (Generati)

Gli screenshot e i video applicativi sono generati automaticamente e salvati sotto `reports/generated-assets/<AssetProfile>/`. Ogni flusso ha una sottocartella specifica.

| Flusso | Cartella Screenshot | Note |
|---|---|---|
| **Social Campaign** | `social/vertical`, `social/square` | Formati per storie e post feed. |
| **Operator Intake** | `demo/screenshots/01-operatore-banco` | Flusso accoglienza e apertura commessa. |
| **Order Lifecycle** | `demo/screenshots/02-commessa-nuovo-cliente-chiusura` | Dall'apertura alla chiusura con timeline. |
| **Configurazione** | `demo/screenshots/03-categorie-template` | Gestione categorie e template PDF. |
| **Anagrafiche** | `demo/screenshots/04-clienti-privato-azienda` | Archivio clienti e dettagli. |
| **Manutenzione** | `demo/screenshots/05-manutenzione-backup` | Storage, export Excel e backup. |
| **Demo Limit** | `demo/screenshots/06-demo-limit` | Messaggio di limite (solo flavor Demo). |
| **Video Device** | `demo/videos/device` | Registrazioni MP4 dirette dello schermo. |
| **Video Montati** | `demo/videos/automated` | Video con caption generati da `ffmpeg`. |

### Dimensioni Store Listing

| Variante | Icona | Feature graphic |
|---|---:|---:|
| Pro | `pro/icon-512.png` 512x512 | `pro/feature-graphic-1024x500.png` 1024x500 |
| Demo | `demo/icon-512.png` 512x512 | `demo/feature-graphic-1024x500.png` 1024x500 |

### Note Operative

- Pro e Demo sono asset per due app Google Play distinte; gli asset principali commerciali partono da Full/Pro.
- Showcase resta asset di rappresentanza fuori store e non va presentato come prodotto Play finale.
- Non inserire sito web o URL pubblico inventato negli asset.
- Gli screenshot e i video applicativi restano output generati e ignorati da Git sotto `reports/generated-assets/<AssetProfile>`.
- La variante `Full` genera gli asset pubblici Pro, social e walkthrough; la variante `Demo` genera solo gli asset Play Store necessari alla listing Demo.
- La variante `Showcase` e' interna e non ha asset Play Store pubblicabili.
- Le feature graphic mostrano solo UI reale; non sostituirla con mockup che promettono funzioni non presenti.
