# Script PowerShell

Gli script pubblici sono in `scripts\windows`; `scripts\windows\support` contiene solo funzioni condivise.
Gli script calcolano automaticamente la root del progetto dal proprio percorso, quindi funzionano anche se il terminale non è aperto nella root.

| script | path | purpose | when to use | called by | prerequisites | outputs | notes |
|---|---|---|---|---|---|---|---|
| Check ambiente Android | `scripts\windows\check_android_env.ps1` | Verifica JDK, Gradle Wrapper, Android SDK Platform 36, Build Tools e adb. | Setup iniziale, controllo env, diagnosi prima di build/test/start. | Manuale | PowerShell; Android Studio o JDK 17+; Android SDK. | Diagnostica a console; exit code non zero se l'ambiente minimo per Gradle non è valido. | Rileva JBR, SDK e platform-tools nel processo corrente; funziona da qualsiasi directory. |
| Check progetto | `scripts\windows\check_project.ps1` | Esegue lint, compile Kotlin debug, test unitari, build APK debug e, senza `-SkipReleaseBuild`, build APK release firmata. | Prima di consegnare modifiche locali o preparare un APK. | Manuale | Ambiente Android valido; `keystore.properties` locale solo per check release; device solo con `-AndroidTests`. | Report Gradle; APK in `app\build\outputs\apk\`. | Usa `--no-configuration-cache`; funziona da qualsiasi directory; `-SkipReleaseBuild` e' il check locale standard senza signing; `-AndroidTests` esegue `connectedDebugAndroidTest`. |
| Audit vulnerabilita' dipendenze | `scripts\windows\check_dependency_vulnerabilities.ps1` | Interroga OSV sulle coordinate Maven presenti nei lockfile Gradle. | Prima di handoff produzione o aggiornamenti dipendenze. | Manuale | PowerShell; lockfile Gradle generati; accesso internet verso OSV. | Output findings OSV; exit code 2 se trova vulnerabilita'. | Separato dai check offline; non invia codice sorgente, solo group/artifact/version. |
| Build installer Android | `scripts\windows\build_android_installer.ps1` | Produce APK o AAB tramite Gradle Wrapper. | Build debug/release, artefatto installabile o distribuibile, dev smoke con `-Install`. | Manuale | Ambiente Android valido; `keystore.properties` locale per release; `adb` solo con `-Install`. | APK/AAB in `app\build\outputs\`. | Funziona da qualsiasi directory; supporta `-BuildType Debug/Release`, `-OutputType Apk/Aab`, `-Clean`, `-RerunTasks`, `-Install`; non crea segreti e blocca release senza signing locale. |
| Start app Android | `scripts\windows\start_android_app.ps1` | Installa l'APK selezionato e avvia `MainActivity` via adb. | Smoke test rapido su device collegato. | Manuale | APK già generato; adb; debug USB attivo. | Installazione e output `am start -W`. | Funziona da qualsiasi directory; default: ultimo APK debug; usare `-NoInstall` per solo avvio o `-ApkPath` per un file specifico. |
| Screenshot demo | `scripts\windows\create_demo_screenshots.ps1` | Genera tutti i PNG demo verticali 1080x1920 divisi per caso d'uso. | Materiale social stories, demo statiche e revisione flussi. | Manuale | PowerShell; Android SDK; AVD `OnlyTechTakeDemo` o un device USB. | PNG in `reports\demo-screenshots\<caso-uso>\`. | Script unico per screenshot; non genera video e non richiede ffmpeg; pulisce i dati del package app sul device demo. |
| Video demo operatore | `scripts\windows\create_demo_video.ps1` | Genera MP4 demo del flusso operatore al banco. | Materiale promozionale o demo dopo modifiche UI rilevanti. | Manuale | PowerShell; Android SDK; AVD `OnlyTechTakeDemo`; ffmpeg. | MP4, slide e screenshot intermedi in `reports\demo-video\operator-intake\`. | Non e' lo script pubblico per screenshot; default emulatore demo; `-UseConnectedDevice` usa un solo device USB e pulisce i dati del package app su quel device. |
| Helper ambiente Android | `scripts\windows\support\android_env.ps1` | Funzioni condivise per rilevare JDK 17+, Android SDK, platform `android-36`, Build Tools e platform-tools. | Dot-source dagli script pubblici. | Script in `scripts\windows`. | PowerShell. | Variabili d'ambiente di processo. | Non eseguire direttamente come script pubblico. |

## Comandi per obiettivo

| Obiettivo | Comando |
|---|---|
| setup / env | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_android_env.ps1` |
| check locale standard | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild` |
| check locale standard con device | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild -AndroidTests` |
| check completo con release firmata | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1` |
| audit vulnerabilita' dipendenze | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_dependency_vulnerabilities.ps1` |
| test unitari | `.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest` |
| build debug APK | `powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Debug -OutputType Apk` |
| build release APK | `powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Apk` |
| package release AAB | `powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Aab` |
| dev smoke | `powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Debug -OutputType Apk -Install` |
| start | `powershell -ExecutionPolicy Bypass -File scripts\windows\start_android_app.ps1` |
| screenshot demo | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_screenshots.ps1` |
| screenshot demo su device USB | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_screenshots.ps1 -UseConnectedDevice` |
| video demo operatore | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_video.ps1` |
| video demo su device USB | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_video.ps1 -UseConnectedDevice` |
