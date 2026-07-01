# Documentazione Script Asset (PowerShell)

Questa cartella contiene gli script per la generazione automatizzata di screenshot e video demo dell'applicazione OnlyTechTake, ottimizzati per Store (Play Store), Social Media e Landing Page.

---

## 1. create_demo_assets.ps1
Genera screenshot dell'interfaccia utente eseguendo test strumentati automatizzati basati su profili JSON.

### Parametri principali
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-AssetProfile` | String | Nome del profilo in `demo/assets/profiles` (es. `store-pro`). |
| `-Flavor` | String | Variante dell'app (`Full`, `Demo`, `Showcase`). Default: `Full`. |
| `-Locale` | String | Codice lingua per il dispositivo (es. `it`, `en`). Default: `en`. |
| `-Dark` | Switch | Attiva il tema scuro (Night Mode) prima della cattura. |
| `-UseConnectedDevice` | Switch | Forza l'uso di un dispositivo fisico invece dell'emulatore. |
| `-SkipBuild` | Switch | Salta la compilazione e installazione dell'APK. |
| `-NoDistribute` | Switch | Genera i file in `asset/` ma non li copia nelle cartelle marketing/landing. |

### Flusso di Lavoro
1. Carica la configurazione dal file `.asset.json` corrispondente.
2. Configura lingua e tema sul dispositivo tramite `adb`.
3. Esegue i test di strumentazione (es. `ScreengrabTest`).
4. **Post-Processing**: Elabora gli screenshot in base al campo `outputs` del profilo (es. conversione in formato Story, Square o Landscape tramite GDI+).
5. **Distribuzione**: Copia gli asset in `marketing/screenshots/` (organizzati per flavor e locale) e in `landing/assets/screenshots/`.

### Elaborazione Screenshot
Se il profilo JSON definisce degli `outputs` con un `variant`, lo script applica trasformazioni automatiche tramite GDI+:
- **vertical**: Ottimizzato per Stories/Reels (1080x1920).
- **square**: Formato 1:1 per feed social con ombra soft.
- **landscape**: Formato 16:9 per presentazioni o post orizzontali.

---

## 2. record_demo_device.ps1
Registra video dei flussi utente dal dispositivo, con post-processing automatico tramite FFmpeg.

### Parametri principali
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-AssetProfile` | String | Profilo JSON per la registrazione automatica del test. |
| `-WhatsAppOptimized` | Switch | Riduce risoluzione (720p) e aumenta compressione (CRF 28) per stare sotto i 10MB. |
| `-SeedData` | Switch | Esegue `SeedDemoDataTest` prima di iniziare la registrazione. |
| `-MaxDurationSeconds`| Int | Durata massima della registrazione (default: 180s). |
| `-AutomaticTrimStartSeconds`| Double | Secondi da tagliare all'inizio (default: 7.0s per saltare splash/setup). |

### Post-Processing
Se `ffmpeg` è installato nel sistema, lo script:
- Normalizza il framerate a 30fps.
- Converte in formato YUV420p per massima compatibilità.
- Applica il trimming (taglio) automatico di testa e coda.
- Ottimizza il bitrate per la distribuzione social se richiesto.

---

## 3. regenerate_public.ps1
Script master per l'aggiornamento massivo di tutto il materiale promozionale pubblico.

### Operazioni eseguite:
1. **Cleanup Totale**: Rimuove la cartella locale `asset/` e pulisce i file `.png` e `.mp4` nelle destinazioni pubbliche (`marketing/` e `landing/`).
2. **Iterazione Profili**: Esegue in sequenza la generazione per una lista predefinita di profili (Pro, Demo, Social, Workflow).
3. **Gestione Mista**: Per i profili che richiedono sia video che screenshot (es. `operator-intake`), gestisce l'esecuzione combinata: esegue prima la registrazione video (che lancia il test) e poi lo screenshot creator con `-SkipTest` per estrarre i frame.

---

## 4. generate_static_brands.ps1
Rigenera asset statici (PNG, ICO, Favicon) dai vettoriali sorgente (`SVG`) presenti in `marketing/` e `landing/`.

### Requisiti
- **ImageMagick** (`magick`) installato nel sistema (consigliato).
- Se non presente, lo script elenca le operazioni manuali necessarie.

### Asset Generati
- **Favicons**: SVG, PNG (192, 512), ICO (48).
- **Social**: GitHub Cover e Social Preview.
- **App Preview**: Icona app con sfondo Charcoal per la landing.

---

## Note Tecniche
- **Profili**: I file di configurazione si trovano in `demo/assets/profiles/*.asset.json`. Questi definiscono `testClass`, `testMethod`, `deviceDirectory` e le trasformazioni di `output` (es. `vertical` per Stories).
- **Tooling**: Richiede Android SDK configurato. `ffmpeg` è necessario per la normalizzazione video, `magick` per gli asset statici.
- **Distribuzione**: I file generati vengono salvati in `asset/<AssetProfile>/` e poi distribuiti automaticamente in:
  - `marketing/screenshots/play-store/` (organizzati per flavor e locale)
  - `marketing/videos/play-store/`
  - `landing/assets/screenshots/`
  - `landing/assets/videos/`
