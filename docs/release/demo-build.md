# Build Demo e Distribuzione Beta

Questa guida descrive come generare e distribuire la build **Demo** dell\'applicazione OnlyTechTake Repair App per i test presso i negozi beta.

## Architettura Build

L\'app utilizza i **Product Flavors** di Gradle per separare la configurazione di test da quella di produzione.

| Caratteristica | Flavor `demo` | Flavor `production` |
| :--- | :--- | :--- |
| **Application ID** | `com.onlytechtake.repairapp.demo` | `com.onlytechtake.repairapp` |
| **Nome App** | OnlyTechTake Demo | OnlyTechTake |
| **Version Name** | `X.Y.Z-demo` | `X.Y.Z` |
| **Banner Demo** | Presente in Home | Assente |
| **Reset Dati** | Disponibile in Admin | Disponibile in Admin |
| **Uso** | Test, Beta shop, Training | Pubblicazione Play Store |

## Comandi di Generazione

Eseguire i comandi dalla radice del progetto utilizzando il terminale (PowerShell consigliato su Windows).

### Generare APK Demo (per installazione diretta)
Per distribuire l\'APK ai tester via WhatsApp, email o download diretto:
```powershell
./gradlew :app:assembleDemoRelease
```
L\'APK generato si troverà in:
`app/build/outputs/apk/demo/release/app-demo-release.apk`

### Generare AAB Production (per Play Store)
Per la pubblicazione ufficiale:
```powershell
./gradlew :app:bundleProductionRelease
```
Il bundle generato si troverà in:
`app/build/outputs/bundle/productionRelease/app-production-release.aab`

## Checklist Pre-Invio Demo

Prima di inviare l\'APK a un negozio per il test:

1. [ ] **Pulizia Dati**: Se l\'APK è stato usato localmente per test, assicurarsi che non contenga dati reali di altri clienti. (Nota: l\'installazione pulita su un nuovo dispositivo non ha questo problema).
2. [ ] **Versione**: Verificare che il `versionCode` in `app/build.gradle.kts` sia aggiornato se si sta sovrascrivendo una build precedente.
3. [ ] **Keystore**: La build `release` richiede che il file `keystore.properties` sia presente nella root e configurato correttamente.

## Avvertenze di Sicurezza

- **APK Demo**: Solo per test autorizzati. Non pubblicare l\'APK demo su store pubblici o forum.
- **Dati Locali**: L\'app opera in modalità `LOCAL_ONLY`. I dati salvati nella demo risiedono esclusivamente sul dispositivo.
- **Firma**: Entrambe le varianti `release` sono firmate con la stessa chiave di produzione (se configurata). Non condividere mai il file di keystore o le password.

## Reset dei Dati
In caso di necessità di riportare l\'app allo stato iniziale durante una demo:
1. Accedere alla sezione **Admin**.
2. Scorrere fino a **Reset totale dati**.
3. Digitare `RESET` nel campo di conferma.
4. Premere il pulsante di reset.
