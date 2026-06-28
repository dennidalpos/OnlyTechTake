# Documentazione Script di Supporto (PowerShell)

Questa cartella contiene script di utilità e librerie di funzioni condivise dagli altri script del progetto. Non sono destinati ad essere eseguiti direttamente dall'utente nella maggior parte dei casi.

---

## 1. android_env.ps1
Gestione dell'ambiente e delle variabili di sistema.

### Funzioni principali
*   **Set-AndroidCliEnvironment**: Rileva automaticamente e configura `JAVA_HOME` (JDK 17/21) e `ANDROID_HOME`.
*   **Get-JavaMajorVersion**: Estrae la versione principale di Java dal percorso specificato.
*   **Get-CandidateJavaHomes**: Cerca installazioni valide di JDK nelle cartelle di sistema comuni.

### Cosa fa
Assicura che gli script PowerShell utilizzino i tool corretti (adb, gradle, javac) indipendentemente dalle variabili d'ambiente globali dell'utente, facilitando la portabilità tra diverse macchine Windows.

---

## 2. android_demo.ps1
Libreria di funzioni per l'automazione dei test e della generazione asset.

### Funzioni principali
*   **Invoke-DemoScreenshotTest**: Esegue un test strumentato specifico per la cattura di screenshot.
*   **Invoke-DemoApkBuildAndInstall**: Gestisce il ciclo build-install-clean per i test strumentati.
*   **Resolve-DemoDeviceSerial**: Identifica il seriale del dispositivo (fisico o emulatore) da utilizzare.
*   **Set-DeviceGlobalSettingTemporary**: Modifica impostazioni del dispositivo (es. lingua, tema) e ne tiene traccia per il ripristino.

### Cosa fa
Fornisce il "motore" per gli script di creazione asset, gestendo l'interazione a basso livello con `adb` e il runner dei test di Android.
