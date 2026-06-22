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

Gli asset usano il marchio `OnlyTechTakeMark`, la palette Material 3 dell'app e screenshot reali generati dalla pipeline demo.

Prima di aggiornare gli asset devono esistere gli screenshot Full e Demo sotto `reports\demo-assets`.

Gli asset generali non destinati allo store sono separati sotto `media/brand/identity`, `media/brand/icons` e `media/brand/social`.

### Dimensioni Store Listing

| Variante | Icona | Feature graphic |
|---|---:|---:|
| Pro | `pro/icon-512.png` 512x512 | `pro/feature-graphic-1024x500.png` 1024x500 |
| Demo | `demo/icon-512.png` 512x512 | `demo/feature-graphic-1024x500.png` 1024x500 |
| Showcase | `showcase/icon-512.png` 512x512 | `showcase/feature-graphic-1024x500.png` 1024x500 |

### Note Operative

- Pro e Demo sono asset per due app Google Play distinte.
- Showcase resta asset commerciale fuori store.
- Non inserire sito web o URL pubblico inventato negli asset.
- Gli screenshot e i video applicativi restano output generati e ignorati da Git sotto `reports/demo-assets/Full` e `reports/demo-assets/Demo`.
- Le feature graphic mostrano solo UI reale; non sostituirla con mockup che promettono funzioni non presenti.
