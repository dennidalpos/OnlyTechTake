# Documentazione Script Asset (PowerShell)

Questa cartella contiene gli script per la generazione automatizzata di screenshot e video demo dell'applicazione OnlyTechTake.

---

## 1. create_demo_assets.ps1
Genera screenshot dell'interfaccia utente eseguendo test strumentati automatizzati.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-Flavor` | String | Variante dell'app (`Full`, `Demo`, `Showcase`). |
| `-AssetProfile` | String | Nome del profilo JSON che definisce i test. |
| `-Locale` | String | Codice lingua per il dispositivo (es. `it`, `en`). |
| `-Dark` | Switch | Attiva il tema scuro prima della cattura. |
| `-UseConnectedDevice` | Switch | Usa un dispositivo fisico invece dell'emulatore. |
| `-SkipBuild` | Switch | Salta la fase di compilazione dell'APK. |

### Cosa fa
Esegue test UI che navigano l'app e scattano screenshot. Le immagini vengono poi distribuite automaticamente nelle cartelle di marketing (Play Store) e nella landing page.

---

## 2. record_demo_device.ps1
Registra video dei flussi utente dal dispositivo.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-Flavor` | String | Variante dell'app (`Full`, `Demo`, `Showcase`). |
| `-AssetProfile` | String | Profilo JSON per la registrazione automatica. |
| `-MaxDurationSeconds`| Int | Durata massima della registrazione (default: 180s). |
| `-BitRate` | Int | Qualità del video (default: 4Mbps). |
| `-WhatsAppOptimized` | Switch | Riduce risoluzione (720p) e aumenta compressione per restare sotto i 10MB. |

### Cosa fa
Cattura l'attività dello schermo durante l'esecuzione di uno scenario. Se `ffmpeg` è disponibile, il video viene ottimizzato e ritagliato automaticamente per rimuovere i tempi morti.

---

## 3. regenerate_public.ps1
Master script per aggiornare tutti gli asset pubblici.

### Cosa fa
Esegue una pulizia completa e rigenera in sequenza tutti gli screenshot e i video per i profili principali (Pro, Demo, Social, Workflow). È lo strumento principale per garantire che il materiale promozionale sia sempre aggiornato all'ultima versione della UI.
