# Script PowerShell

Gli script ufficiali Android sono in `scripts\android`; `scripts\support` contiene le funzioni condivise. I vecchi script in `scripts\windows` restano wrapper compatibili o script Windows specifici per demo/check.

Gli script calcolano automaticamente la root del progetto dal proprio percorso, quindi funzionano anche se il terminale non e' aperto nella root. Il rilevamento JDK accetta Android Studio JBR o JDK 17/21 e ignora Java 26 nel `PATH`.

| script | path | purpose | notes |
|---|---|---|---|
| Check ambiente Android | `scripts\android\check_env.ps1` | Verifica JDK supportato, Gradle Wrapper, Android SDK Platform 37, Build Tools e adb. | Exit code non zero se l'ambiente minimo per Gradle non e' valido. |
| Gradle wrapper Android | `scripts\android\gradle.ps1` | Esegue `gradlew.bat` con JDK/SDK risolti dallo script. | Usare `--` prima degli argomenti Gradle. |
| Build installer Android | `scripts\android\build_installer.ps1` | Produce APK o AAB tramite Gradle Wrapper e opzionalmente installa APK via adb. | Supporta `-Flavor Full|Demo`, `-BuildType`, `-OutputType`, `-Clean`, `-RerunTasks`, `-Install`; blocca release senza signing locale. |
| Start app Android | `scripts\android\start_app.ps1` | Installa l'APK selezionato e avvia `MainActivity` via adb. | Supporta `-Flavor Full|Demo`, `-NoInstall`, `-ApkPath` e `-CleanInstall` per reinstallazione pulita. |
| Check progetto | `scripts\windows\check_project.ps1` | Esegue lint, compile Kotlin debug, test unitari, build APK debug e, senza `-SkipReleaseBuild`, build APK release firmata. | Supporta `-Flavor Full|Demo`; `-AndroidTests` esegue il connected test del flavor selezionato. |
| Audit vulnerabilita' dipendenze | `scripts\windows\check_dependency_vulnerabilities.ps1` | Interroga OSV sulle coordinate Maven presenti nei lockfile Gradle. | Separato dai check offline; non invia codice sorgente, solo group/artifact/version. |
| Asset demo unificati | `scripts\windows\create_demo_assets.ps1` | Genera screenshot verticali e video MP4 montati. | Richiede ffmpeg per i video; usa AVD `OnlyTechTakeDemo` o device USB. |
| Registrazione demo USB | `scripts\windows\record_demo_device.ps1` | Registra video MP4 direttamente da device fisico USB. | Pulisce dati app, avvia app e usa `adb screenrecord`. |

## Comandi per obiettivo

| Obiettivo | Comando |
|---|---|
| setup / env | `powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1` |
| asset demo (screen + video) | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_assets.ps1` |
| asset demo su device USB | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_assets.ps1 -UseConnectedDevice` |
| registrazione demo USB guidata | `powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1` |
