# Media

Asset visuali canonici del progetto, separati per destinazione.

```text
brand/identity/   logo e marchio SVG
brand/icons/      favicon, ICO e anteprima icona
brand/social/     cover GitHub e social preview
play-store/       icone e feature graphic per variante
```

Le copie necessarie alla landing sono pubblicate sotto `landing/assets` con la
stessa separazione `brand`, `icons` e `social`. Non modificare manualmente le
copie: rigenerare e sincronizzare tutto con:

```powershell
& "<python-con-Pillow>" scripts\windows\generate_media_assets.py
```

Il comando richiede gli screenshot Full e Demo già presenti sotto
`reports\demo-assets`.
