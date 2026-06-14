# OnlyTechTake Repair App

![OnlyTechTake Repair App](media/brand/github-cover-repair-flow.png)

App Android locale per tracciare commesse clienti dal banco alla diagnosi, alla riparazione e alla consegna su un singolo device. L'app prepara gli aggiornamenti WhatsApp di avanzamento, lasciando all'operatore il controllo dell'invio manuale al cliente.

## Stato Reale

- Sistema di sviluppo supportato: Windows + PowerShell.
- Runtime app: Android, package `com.onlytechtake.repairapp`.
- Versione app: `0.1.0`, `versionCode` 1.
- Flavor Gradle: `full` e `demo`; gli script usano `Full` come default.
- SDK Android: compile/target SDK 37, min SDK 26.
- Stack: Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room, DataStore, CameraX, Coroutines/Flow.
- Dati: Room locale, DataStore per preferenze auth, file privati app, export tramite Storage Access Framework.
- Schema Room corrente: `DATABASE_VERSION = 2`; sono tracciati gli export `1.json` e `2.json`. Il codice non contiene classi di migrazione e usa reinstallazione pulita o fallback distruttivo in questa fase fresh-app.
- Backend: assente. WhatsApp e' solo manuale.
- CI: non configurata nel repository; i gate sono locali.

## Documentazione

- [Guida funzionale](docs/01_GUIDA_FUNZIONALE.md)
- [Specifica tecnica](docs/02_SPECIFICA_TECNICA.md)
- [Modello dati](docs/03_MODELLO_DATI_GENERICO.md)
- [Schema flusso commesse (Draw.io)](docs/07_SCHEMA_FLUSSO_COMMESSE.drawio)
- [Privacy, WhatsApp e feedback](docs/04_PRIVACY_WHATSAPP_FEEDBACK.md)
- [Export e backup locale](docs/05_EXPORT_BACKUP_LOCALE.md)
- [Build, installazione e verifica](docs/06_BUILD_INSTALLER_ANDROID.md)
- [Demo/Full e manuale operativo](docs/07_LICENZA_DEMO_FULL_E_MANUALE_USO.md)
- [Handoff futuro backend, non implementato nell'app attuale](docs/07_BACKEND_AGENT_TRACKER.md)
- [Script PowerShell](scripts/README.md)

## Prerequisiti

- PowerShell.
- Android Studio recente.
- JDK 17/21 oppure JBR incluso in Android Studio.
- Android SDK Platform `android-37`.
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

Da una copia pulita del repository o dopo modifiche incompatibili allo schema locale:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -CleanInstall
```

Per provare l'app senza cancellare dati locali esistenti:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk -Install
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1
```

Al primo avvio, se non esiste un admin locale, l'app mostra il setup admin. Dopo la creazione dell'admin mostra la dashboard; dagli avvii successivi mostra il login.

## Comandi Ufficiali

Gli script PowerShell calcolano la root del progetto dal proprio percorso, quindi possono essere eseguiti anche da una sottocartella. Il comando diretto `.\gradlew.bat` va invece lanciato dalla root e usa la JVM gia' configurata nel processo PowerShell; se il processo punta a Java 8 o Java 26, usare gli script o impostare `JAVA_HOME` su JDK/JBR 17/21.

`scripts\android\build_installer.ps1` e `scripts\android\start_app.ps1` accettano `-Flavor Full|Demo`; se omesso usano `Full`. I package reali sono:

- `Full Debug`: `com.onlytechtake.repairapp.debug`
- `Full Release`: `com.onlytechtake.repairapp`
- `Demo Debug`: `com.onlytechtake.repairapp.demo.debug`
- `Demo Release`: `com.onlytechtake.repairapp.demo`

| Obiettivo | Comando |
|---|---|
| Setup / env check | `powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1` |
| Check locale standard senza signing | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild` |
| Check locale standard + instrumentation test | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild -AndroidTests` |
| Check completo con release firmata | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full` |
| Audit vulnerabilita' dipendenze | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_dependency_vulnerabilities.ps1` |
| Test unitari Full debug | `powershell -ExecutionPolicy Bypass -File scripts\android\gradle.ps1 -- --no-configuration-cache :app:testFullDebugUnitTest` |
| Build APK debug Full | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk` |
| Build APK debug Demo | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Demo -BuildType Debug -OutputType Apk` |
| Build APK release Full | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Apk` |
| Build AAB release Full | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Aab` |
| Dev smoke su device | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk -Install` |
| Installazione pulita e start su device | `powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -CleanInstall` |

## Handoff Operativo

Prima di consegnare il repository:

1. Controllare lo stato con `git status --short` e distinguere modifiche tracciate da file non tracciati.
2. Leggere `PROJECT_STATUS.json`: contiene solo todo incompleti correnti.
3. Eseguire almeno `check_env.ps1`, `check_project.ps1 -Flavor Full -SkipReleaseBuild` e una build/install debug se serve validare su device.
4. Usare `start_app.ps1 -CleanInstall` quando lo schema Room locale e' incompatibile con dati gia' installati; il codice corrente e' Room v2 senza migrazioni conservative.

Nota: file non tracciati sotto `docs\` non fanno parte della documentazione ufficiale del repository finche' non vengono revisionati e aggiunti intenzionalmente.

## Output Build

- APK Full Debug: `app\build\outputs\apk\full\debug\app-full-debug.apk`
- APK Demo Debug: `app\build\outputs\apk\demo\debug\app-demo-debug.apk`
- APK Full Release: `app\build\outputs\apk\full\release\app-full-release.apk`
- APK Demo Release: `app\build\outputs\apk\demo\release\app-demo-release.apk`
- AAB Full Release: `app\build\outputs\bundle\fullRelease\app-full-release.aab`
- AAB Demo Release: `app\build\outputs\bundle\demoRelease\app-demo-release.aab`

## Release Signing

`app\build.gradle.kts` legge `keystore.properties` dalla root solo se il file esiste. Gli script `check_project.ps1` e `scripts\android\build_installer.ps1` bloccano APK/AAB release quando il signing locale non e' configurato. Il file e il keystore sono locali e ignorati da `.gitignore`.

Schema richiesto (copiare da `keystore.properties.example` e rinominare in `keystore.properties`):

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
- Creazione commessa a step con cliente esistente o nuovo, categoria ad albero obbligatoria, problema, almeno un media/allegato, privacy e firma.
- Workspace commessa con esito diagnosi esplicito `REPAIRABLE`/`NOT_REPAIRABLE`, preventivo obbligatorio solo nel ramo riparabile, azioni post-diagnosi guidate, note, media, firma, timeline, WhatsApp manuale locale, stato pagamento e condivisione PDF.
- Le anteprime immagini locali rispettano l'orientamento EXIF; CameraX usa la rotazione reale del display prima del bind e dello scatto.
- Admin: utenti, reset password, pulizia commesse scartate/archiviate e reset totale dati con conferma `RESET`.
- Config admin: categorie multilivello, template privacy/condizioni/WhatsApp, modalita WhatsApp manuale, backup ZIP, ripristino ZIP ed export XLSX.
- Import allegati tramite SAF per JPEG/PNG/WebP, PDF, TXT, DOC/DOCX e XLS/XLSX fino a 25 MiB.
- Export admin: ZIP completo e XLSX da Config > Backup; database e XLSX pre-cleanup dalla sezione Admin; configurazione JSON nel repository ma non esposta come pulsante dedicato nella schermata Backup corrente. Ripristino ZIP completo tramite SAF per admin. Condivisione PDF della singola commessa dal dettaglio.
- Promemoria backup admin: attivo di default ogni 7 giorni, basato sull'ultimo ZIP completo riuscito; non blocca l'uso normale dell'app.
- Demo release: massimo 10 commesse, massimo 2 operatori, export database/ZIP/XLSX e restore ZIP bloccati, PDF con watermark `VERSIONE DEMO`.
- Se il device espone provider come Drive, OneDrive o Dropbox nel selettore documenti Android, vengono usati tramite SAF come destinazione scelta dall'utente; non esiste integrazione cloud proprietaria.

## Limiti Noti

- Nessun backend e nessuna API WhatsApp.
- Export JSON, database, PDF e ZIP non sono cifrati dall'app.
- Non esiste un gate CI remoto tracciato nel repository.
- La build release richiede signing locale non tracciato.
- Il codice sorgente e' gia' a Room v2, mentre alcune istruzioni di governance repository richiedono approvazione owner per ulteriori cambi schema; non aggiungere altri cambi schema senza decisione esplicita.
- Le verifiche manuali residue sono elencate in `PROJECT_STATUS.json`.

## Struttura

- `app\`: modulo Android.
- `app\src\main\java\com\onlytechtake\repairapp\core\`: design, DI, media, ordini, permessi, sicurezza, template.
- `app\src\main\java\com\onlytechtake\repairapp\data\`: Room, DataStore, repository, sessione.
- `app\src\main\java\com\onlytechtake\repairapp\domain\`: modelli, policy e contratti.
- `app\src\main\java\com\onlytechtake\repairapp\features\`: UI Compose e ViewModel.
- `app\src\test\`: test unitari JVM.
- `app\src\androidTest\`: test strumentali su device/emulatore.
- `app\schemas\`: schema Room esportato; sono presenti `1.json` e `2.json`, coerenti con `DATABASE_VERSION = 2`.
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
- `Platform android-37 non trovata`: installare Android SDK Platform 37 da Android Studio SDK Manager.
- `Android SDK Build Tools non trovati`: installare Android SDK Build Tools da Android Studio SDK Manager.
- `adb non trovato`: installare Android Platform Tools o aggiungere `platform-tools` al `PATH`.
- Nessun device in `adb devices`: abilitare debug USB e autorizzare il computer sul device.
- `Activity class ... does not exist`: installare prima l'APK con `start_app.ps1` senza `-NoInstall`.
- File lock Gradle/Kotlin su Windows: chiudere Android Studio se sta compilando, eseguire `.\gradlew.bat --stop` e rilanciare uno script alla volta.
- Build release bloccata per signing: manca `keystore.properties` locale o il file non punta a un keystore valido.
