# OnlyTechTake Repair App

![OnlyTechTake Repair App](media/brand/github-cover-repair-flow.png)

App Android locale per tracciare commesse clienti dal banco alla diagnosi, alla riparazione e alla consegna su un singolo device. L'app prepara gli aggiornamenti WhatsApp di avanzamento, lasciando all'operatore il controllo dell'invio manuale al cliente.

## Stato Reale

- Sistema di sviluppo supportato: Windows + PowerShell.
- Runtime app: Android, package `com.onlytechtake.repairapp`.
- Versione app: `0.1.0`, `versionCode` 1.
- SDK Android: compile/target SDK 36, min SDK 26.
- Stack: Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room, DataStore, CameraX, Coroutines/Flow.
- Dati: Room locale, DataStore per preferenze auth, file privati app, export tramite Storage Access Framework.
- Backend: assente. WhatsApp e' solo manuale.
- CI: non configurata nel repository; i gate sono locali.

## Documentazione

- [Guida funzionale](docs/01_GUIDA_FUNZIONALE.md)
- [Specifica tecnica](docs/02_SPECIFICA_TECNICA.md)
- [Modello dati](docs/03_MODELLO_DATI_GENERICO.md)
- [Privacy, WhatsApp e feedback](docs/04_PRIVACY_WHATSAPP_FEEDBACK.md)
- [Export e backup locale](docs/05_EXPORT_BACKUP_LOCALE.md)
- [Build, installazione e verifica](docs/06_BUILD_INSTALLER_ANDROID.md)
- [Script PowerShell](scripts/README.md)

## Prerequisiti

- PowerShell.
- Android Studio recente.
- JDK 17/21 oppure JBR incluso in Android Studio.
- Android SDK Platform `android-36`.
- Android SDK Build Tools.
- Android Platform Tools per `adb`, se servono installazione, start o instrumentation test su device.
- Debug USB abilitato e PC autorizzato sul device per i comandi via `adb`.

Gli script ufficiali in `scripts\android` rilevano automaticamente Android Studio JBR, Android SDK e platform-tools nei percorsi standard. I vecchi comandi in `scripts\windows` restano wrapper compatibili. Se l'autodetect non basta, impostare nel processo PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:ANDROID_HOME\platform-tools;$env:JAVA_HOME\bin;$env:Path"
```

## Setup Fresh-Install

Da una copia pulita del repository:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Debug -OutputType Apk
```

Per provare l'app su device:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Debug -OutputType Apk -Install
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1
```

Al primo avvio, se non esiste un admin locale, l'app mostra il setup admin. Dopo la creazione dell'admin mostra la dashboard; dagli avvii successivi mostra il login.

## Comandi Ufficiali

Gli script PowerShell calcolano la root del progetto dal proprio percorso, quindi possono essere eseguiti anche da una sottocartella. Il comando diretto `.\gradlew.bat` va invece lanciato dalla root e usa la JVM gia' configurata nel processo PowerShell; se il processo punta a Java 8 o Java 26, usare gli script o impostare `JAVA_HOME` su JDK/JBR 17/21.

| Obiettivo | Comando |
|---|---|
| Setup / env check | `powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1` |
| Check locale standard senza signing | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild` |
| Check locale standard + instrumentation test | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild -AndroidTests` |
| Check completo con release firmata | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1` |
| Audit vulnerabilita' dipendenze | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_dependency_vulnerabilities.ps1` |
| Test unitari | `powershell -ExecutionPolicy Bypass -File scripts\android\gradle.ps1 -- --no-configuration-cache :app:testDebugUnitTest` |
| Build APK debug | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Debug -OutputType Apk` |
| Build APK release | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Release -OutputType Apk` |
| Build AAB release | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Release -OutputType Aab` |
| Dev smoke su device | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Debug -OutputType Apk -Install` |
| Installazione pulita e start su device | `powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -CleanInstall` |

## Output Build

- APK debug: `app\build\outputs\apk\debug\app-debug.apk`
- APK release firmato: `app\build\outputs\apk\release\app-release.apk`
- AAB release: `app\build\outputs\bundle\release\app-release.aab`

## Release Signing

`app\build.gradle.kts` legge `keystore.properties` dalla root solo se il file esiste. Gli script `check_project.ps1` e `build_android_installer.ps1` bloccano APK/AAB release quando il signing locale non e' configurato. Il file e il keystore sono locali e ignorati da `.gitignore`.

Schema richiesto:

```properties
storeFile=C:\Percorso\locale\repair-app-release.jks
storePassword=CAMBIA_QUESTA_PASSWORD
keyAlias=repairapp
keyPassword=CAMBIA_QUESTA_PASSWORD
```

Regole:

- non inserire password, token, keystore o percorsi personali nei file tracciati;
- non committare `keystore.properties`;
- non committare file `.jks` o `.keystore`;
- senza `keystore.properties`, ogni build release fallisce prima della compilazione release.

## Funzioni Implementate

- Setup primo admin, login admin/operatore, logout, lockout dopo 5 tentativi falliti e scadenza sessione dopo 30 minuti di inattivita'.
- Dashboard con tab Home, Nuova, Clienti, Commesse/Commessa, Config e Admin.
- Home con metriche e quattro code operative che seguono il flusso banco, diagnosi, preventivo/riparazione e consegna.
- Creazione commessa a step con cliente esistente o nuovo, categoria attiva obbligatoria, problema, almeno un media/allegato, privacy e firma.
- Workspace commessa con azioni di stato, prossima azione, note interne, foto/allegati, foto lavoro finito, firma, timeline, anteprima WhatsApp manuale ed export PDF.
- Admin: utenti, reset password, pulizia commesse scartate/archiviate e reset totale dati con conferma `RESET`.
- Config admin: categorie, template privacy/condizioni/WhatsApp, modalita WhatsApp manuale, export config/database/ZIP/XLSX.
- Import allegati tramite SAF per JPEG/PNG/WebP, PDF, TXT, DOC/DOCX e XLS/XLSX fino a 25 MiB.
- Export admin di configurazione, database, ZIP completo e XLSX commesse tramite SAF. Ripristino ZIP completo tramite SAF per admin. Export PDF della singola commessa dal dettaglio.

## Limiti Noti

- Nessun backend e nessuna API WhatsApp.
- Export JSON, database, PDF e ZIP non sono cifrati dall'app.
- Non esiste un gate CI remoto tracciato nel repository.
- La build release richiede signing locale non tracciato.

## Struttura

- `app\`: modulo Android.
- `app\src\main\java\com\onlytechtake\repairapp\core\`: design, DI, media, ordini, permessi, sicurezza, template.
- `app\src\main\java\com\onlytechtake\repairapp\data\`: Room, DataStore, repository, sessione.
- `app\src\main\java\com\onlytechtake\repairapp\domain\`: modelli, policy e contratti.
- `app\src\main\java\com\onlytechtake\repairapp\features\`: UI Compose e ViewModel.
- `app\src\test\`: test unitari JVM.
- `app\src\androidTest\`: test strumentali su device/emulatore.
- `app\schemas\`: schema Room esportato.
- `docs\`: documentazione operativa e funzionale.
- `media\brand\`: logo, favicon, anteprime e cover README per GitHub.
- `scripts\android\`: script PowerShell ufficiali per ambiente, Gradle, build/install e start app.
- `scripts\support\`: funzioni condivise per rilevamento Android/JDK.
- `scripts\windows\`: wrapper compatibili e script Windows specifici.

## Troubleshooting

- `gradlew.bat non trovato`: eseguire i comandi Gradle dalla root o usare gli script in `scripts\android`.
- `Java non trovato` o `javac non trovato`: installare JDK 17/21 o usare Android Studio JBR, poi rieseguire `check_env.ps1`.
- Java 26 nel `PATH`: gli script lo ignorano e preferiscono Android Studio JBR o un JDK 17/21 supportato.
- `Dependency requires at least JVM runtime version 11` con `.\gradlew.bat`: il processo sta usando una JVM troppo vecchia; usare gli script PowerShell ufficiali o impostare `JAVA_HOME` su JDK/JBR 17/21 nello stesso terminale.
- `Platform android-36 non trovata`: installare Android SDK Platform 36 da Android Studio SDK Manager.
- `Android SDK Build Tools non trovati`: installare Android SDK Build Tools da Android Studio SDK Manager.
- `adb non trovato`: installare Android Platform Tools o aggiungere `platform-tools` al `PATH`.
- Nessun device in `adb devices`: abilitare debug USB e autorizzare il computer sul device.
- `Activity class ... does not exist`: installare prima l'APK con `start_app.ps1` senza `-NoInstall`.
- File lock Gradle/Kotlin su Windows: chiudere Android Studio se sta compilando, eseguire `.\gradlew.bat --stop` e rilanciare uno script alla volta.
- Build release bloccata per signing: manca `keystore.properties` locale o il file non punta a un keystore valido.
