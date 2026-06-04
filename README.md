# OnlyTechTake Repair App

App Android offline-first per gestione commesse di riparazione su un device locale.

## Target operativo

- Sistema di sviluppo supportato: Windows + PowerShell.
- Runtime app: Android, package `com.onlytechtake.repairapp`.
- Versione app corrente: `0.1.0`.
- SDK Android: compile/target SDK 36, min SDK 26.
- Distribuzione: APK installato direttamente su device; AAB release firmato generabile con script PowerShell quando il signing locale e configurato.
- Dati: Room locale, file privati app, export tramite Storage Access Framework.
- Backend: non richiesto per questo rilascio.

## Prerequisiti

- PowerShell.
- Android Studio recente.
- JDK 17+ oppure JBR incluso in Android Studio.
- Android SDK Platform `android-36`.
- Android SDK Build Tools.
- Android Platform Tools per installazione, avvio e test su device via `adb`.
- Debug USB abilitato sul device per comandi `start` e instrumentation test.

Gli script in `scripts\windows` rilevano automaticamente Android Studio JBR, Android SDK e platform-tools nei percorsi standard. Se l'autodetect non basta, impostare nel processo PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:ANDROID_HOME\platform-tools;$env:JAVA_HOME\bin;$env:Path"
```

## Comandi ufficiali

Gli script PowerShell calcolano automaticamente la root del progetto dal proprio percorso, quindi possono essere eseguiti anche da una sottocartella. I comandi ufficiali sono PowerShell + Gradle Wrapper.

| Obiettivo | Comando |
|---|---|
| Setup / env check | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_android_env.ps1` |
| Check locale standard senza signing | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild` |
| Check locale standard + instrumentation test | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild -AndroidTests` |
| Check completo con release firmata | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1` |
| Audit vulnerabilita' dipendenze | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_dependency_vulnerabilities.ps1` |
| Test unitari | `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest` |
| Build APK debug | `powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Debug -OutputType Apk` |
| Build APK release | `powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Apk` |
| Build AAB release | `powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Aab` |
| Dev smoke su device | `powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Debug -OutputType Apk -Install` |
| Start app su device | `powershell -ExecutionPolicy Bypass -File scripts\windows\start_android_app.ps1` |

## Output build

- APK debug: `app\build\outputs\apk\debug\app-debug.apk`
- APK release firmato: `app\build\outputs\apk\release\app-release.apk`
- AAB release: `app\build\outputs\bundle\release\app-release.aab`

## Release signing

`app\build.gradle.kts` legge `keystore.properties` dalla root solo se il file esiste. Gli script `check_project.ps1` e `build_android_installer.ps1` bloccano APK/AAB release quando il signing locale non e configurato. Il file e il keystore sono locali e ignorati da `.gitignore`.

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

## Verifica device

```powershell
adb devices
powershell -ExecutionPolicy Bypass -File scripts\windows\start_android_app.ps1
```

Per instrumentation test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild -AndroidTests
```

## Struttura

- `app\`: modulo Android.
- `app\src\main\java\com\onlytechtake\repairapp\core\`: utilità applicative, sicurezza, validazione, media.
- `app\src\main\java\com\onlytechtake\repairapp\data\`: Room, DataStore, repository.
- `app\src\main\java\com\onlytechtake\repairapp\domain\`: modelli e contratti.
- `app\src\main\java\com\onlytechtake\repairapp\features\`: UI e ViewModel.
- `app\src\test\`: test unitari.
- `app\src\androidTest\`: test strumentali su device/emulatore.
- `docs\`: documentazione operativa e funzionale.
- `scripts\windows\`: script PowerShell ufficiali.

## Funzioni operative

- Dashboard full-screen con Home, Nuova commessa, Clienti, Commesse, Config e Admin.
- Home operativa con quattro code: da diagnosticare, attesa cliente, da lavorare, pronte/chiusura; ogni coda apre l'elenco commesse filtrato.
- Creazione commessa in un modulo rapido: cliente esistente o nuovo, problema, categoria obbligatoria, foto/allegati, privacy e firma.
- La presa in carico e' bloccata se non esiste almeno una categoria attiva configurata.
- Workspace commessa guidato dallo stato operativo, con avanzamenti diretti per azioni routinarie e conferme solo per chiusura/annullamento, export e WhatsApp manuale.
- Config admin come hub con sezioni dedicate per categorie, template privacy/WhatsApp, modalita WhatsApp manuale ed export.
- Reset totale dati in Admin riservato agli amministratori con conferma testuale `RESET`.
- Lo schema Room corrente parte da v1 pulito: la commessa usa `repair_orders.status`, timeline, media e firme senza workflow paralleli.
- Template WhatsApp manuali con anteprima modificabile e conferma manuale di invio.
- Firma cliente su canvas Compose salvata come PNG in spazio privato app.
- Foto e allegati collegati alla commessa in spazio privato app, con import SAF limitato a immagini JPEG/PNG/WebP, PDF, TXT, DOC/DOCX e XLS/XLSX fino a 25 MiB.
- Export admin di configurazione, database e ZIP completo tramite Storage Access Framework.
- Export PDF della singola commessa dal dettaglio commessa.

## Troubleshooting

- `gradlew.bat non trovato`: eseguire i comandi dalla root del progetto.
- `Java non trovato` o `javac non trovato`: installare JDK 17+ o usare Android Studio JBR, poi rieseguire `check_android_env.ps1`.
- `Platform android-36 non trovata`: installare Android SDK Platform 36 da Android Studio SDK Manager.
- `Android SDK Build Tools non trovati`: installare Android SDK Build Tools da Android Studio SDK Manager.
- `adb non trovato`: installare Android Platform Tools o aggiungere `platform-tools` al `PATH`.
- Nessun device in `adb devices`: abilitare debug USB e autorizzare il computer sul device.
- `Activity class ... does not exist`: installare prima l'APK con `start_android_app.ps1` senza `-NoInstall`.
- File lock Gradle/Kotlin su Windows: chiudere Android Studio se sta compilando, eseguire `.\gradlew.bat --stop` e rilanciare uno script alla volta.
- Build release bloccata per signing: manca `keystore.properties` locale o il file non punta a un keystore valido.
