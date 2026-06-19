# Play Store Assets

Asset statici generati per Google Play e presentazioni commerciali.

Gli asset usano il marchio `OnlyTechTakeMark`, la palette Material 3 dell'app e
screenshot reali generati dalla pipeline demo. Per rigenerarli:

```powershell
& "<python-con-Pillow>" scripts\windows\generate_media_assets.py
```

Prima del comando devono esistere gli screenshot Full e Demo sotto
`reports\demo-assets`. Il generatore aggiorna anche gli asset brand e le copie
pubblicate in `landing\assets`.

Gli asset generali non destinati allo store sono separati sotto
`media/brand/identity`, `media/brand/icons` e `media/brand/social`.

## Dimensioni

| Variante | Icona | Feature graphic |
|---|---:|---:|
| Pro | `pro/icon-512.png` 512x512 | `pro/feature-graphic-1024x500.png` 1024x500 |
| Demo | `demo/icon-512.png` 512x512 | `demo/feature-graphic-1024x500.png` 1024x500 |
| Showcase | `showcase/icon-512.png` 512x512 | `showcase/feature-graphic-1024x500.png` 1024x500 |

## Note operative

- Pro e Demo sono asset per due app Google Play distinte.
- Showcase resta asset commerciale fuori store.
- Non inserire sito web o URL pubblico inventato negli asset.
- Gli screenshot e i video applicativi restano output generati e ignorati da Git sotto `reports/demo-assets/Full` e `reports/demo-assets/Demo`.
- Le feature graphic mostrano solo UI reale; non sostituirla con mockup che promettono funzioni non presenti.
