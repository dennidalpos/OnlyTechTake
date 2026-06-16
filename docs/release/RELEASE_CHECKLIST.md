# Release Checklist

Checklist tecnica da completare prima di caricare l\'AAB (Android App Bundle) sulla Play Console.

## 1. Configurazione Build
- [ ] **VersionCode:** Incrementato rispetto all\'ultima release (es. `1`).
- [ ] **VersionName:** Aggiornato secondo semver (es. `0.1.0`).
- [ ] **Package Name:** Confermato `com.onlytechtake.repairapp` per flavor `full` release.
- [ ] **MinSDK / TargetSDK:** Verificati (26 / 37).
- [ ] **Signing:** `keystore.properties` presente e configurato correttamente.
- [ ] **Build Type:** Generata build di tipo `release`; nello stato attuale `isMinifyEnabled = true` e `isShrinkResources = true`.
- [ ] **Comando AAB:** `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Aab`.

## 2. Test Funzionali (Clean Install)
- [ ] **Primo Setup:** Il flusso di creazione admin al primo avvio funziona correttamente.
- [ ] **AndroidTests CI:** La workflow `Android Instrumented Tests` e/o `scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild -AndroidTests` completano `:app:connectedFullDebugAndroidTest`.
- [ ] **Crea Commessa:** Flusso completo da Step 1 a Step 6 senza crash.
- [ ] **Camera:** Scatto foto e salvataggio media funzionanti su Android 8, 11 e 14+.
- [ ] **Firma:** Acquisizione e visualizzazione firma funzionante.
- [ ] **PDF:** Generazione PDF e apertura selettore di condivisione.
- [ ] **WhatsApp:** Apertura WhatsApp con il testo preimpostato corretto.
- [ ] **Backup:** Export ZIP e verifica che il file generato sia integro.
- [ ] **Ripristino:** Test di ripristino di un backup ZIP su un\'installazione pulita.

## 3. UI & Asset
- [ ] **Icona:** L\'icona appare correttamente nel launcher e nelle notifiche.
- [ ] **Adaptive Icon:** Verificata la compatibilità con le icone adattive.
- [ ] **Splash Screen:** Visualizzazione corretta (se implementato).
- [ ] **Orientamento:** La fotocamera gestisce correttamente la rotazione delle foto.
- [ ] **Keyboard:** I campi di input non vengono coperti dalla tastiera (adjustResize).
- [ ] **Accessibilita' UI:** Verificare su device fisico contrasti, touch target e segnalazioni del Pre-launch Report; i token critici sono coperti anche da test automatici.

## 4. Privacy & Compliance
- [ ] **Permessi:** La richiesta del permesso CAMERA è chiara e giustificata.
- [ ] **Privacy Link:** URL della Privacy Policy funzionante e accessibile.
- [ ] **Data Safety:** Risposte del questionario console allineate al comportamento reale.

## 5. Passaggi Finali in Console
- [ ] Caricamento AAB nel canale di test interno (Internal Testing).
- [ ] Risoluzione di eventuali segnalazioni del Pre-launch Report (crash o warning accessibilità).
- [ ] Configurazione della scheda dello store (Store Listing) in IT ed EN.
- [ ] Caricamento degli asset da `reports/demo-assets/screenshots` e `reports/demo-assets/videos`.
- [ ] Invio per la revisione.
