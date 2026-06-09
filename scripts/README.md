# Script PowerShell

Gli script ufficiali Android sono in `scripts\android`; `scripts\support` contiene le funzioni condivise. I vecchi script in `scripts\windows` restano wrapper compatibili o script Windows specifici per demo/check.

Gli script calcolano automaticamente la root del progetto dal proprio percorso, quindi funzionano anche se il terminale non e' aperto nella root. Il rilevamento JDK accetta Android Studio JBR o JDK 17/21 e ignora Java 26 nel `PATH`.

| script | path | purpose | notes |
|---|---|---|---|
| Check ambiente Android | `scripts\android\check_env.ps1` | Verifica JDK supportato, Gradle Wrapper, Android SDK Platform 36, Build Tools e adb. | Exit code non zero se l'ambiente minimo per Gradle non e' valido. |
| Gradle wrapper Android | `scripts\android\gradle.ps1` | Esegue `gradlew.bat` con JDK/SDK risolti dallo script. | Usare `--` prima degli argomenti Gradle. |
| Build installer Android | `scripts\android\build_installer.ps1` | Produce APK o AAB tramite Gradle Wrapper e opzionalmente installa APK via adb. | Supporta `-BuildType`, `-OutputType`, `-Clean`, `-RerunTasks`, `-Install`; blocca release senza signing locale. |
| Start app Android | `scripts\android\start_app.ps1` | Installa l'APK selezionato e avvia `MainActivity` via adb. | Supporta `-NoInstall`, `-ApkPath` e `-CleanInstall` per reinstallazione pulita. |
| Check progetto | `scripts\windows\check_project.ps1` | Esegue lint, compile Kotlin debug, test unitari, build APK debug e, senza `-SkipReleaseBuild`, build APK release firmata. | Usa l'helper condiviso tramite wrapper compatibility; `-AndroidTests` esegue `connectedDebugAndroidTest`. |
| Audit vulnerabilita' dipendenze | `scripts\windows\check_dependency_vulnerabilities.ps1` | Interroga OSV sulle coordinate Maven presenti nei lockfile Gradle. | Separato dai check offline; non invia codice sorgente, solo group/artifact/version. |
| Screenshot demo | `scripts\windows\create_demo_screenshots.ps1` | Genera PNG demo verticali 1080x1920 divisi per caso d'uso. | Usa AVD `OnlyTechTakeDemo` o un device USB con `-UseConnectedDevice`. |
| Video demo operatore | `scripts\windows\create_demo_video.ps1` | Genera MP4 demo del flusso operatore al banco. | Richiede ffmpeg; usa AVD demo o device USB con `-UseConnectedDevice`. |
| Wrapper compatibility | `scripts\windows\check_android_env.ps1` | Delega a `scripts\android\check_env.ps1`. | Mantenuto per comandi esistenti. |
| Wrapper compatibility | `scripts\windows\build_android_installer.ps1` | Delega a `scripts\android\build_installer.ps1`. | Mantenuto per comandi esistenti. |
| Wrapper compatibility | `scripts\windows\start_android_app.ps1` | Delega a `scripts\android\start_app.ps1`. | Mantenuto per comandi esistenti. |

## Comandi per obiettivo

| Obiettivo | Comando |
|---|---|
| setup / env | `powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1` |
| check locale standard | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild` |
| check locale standard con device | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild -AndroidTests` |
| check completo con release firmata | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1` |
| audit vulnerabilita' dipendenze | `powershell -ExecutionPolicy Bypass -File scripts\windows\check_dependency_vulnerabilities.ps1` |
| test unitari | `powershell -ExecutionPolicy Bypass -File scripts\android\gradle.ps1 -- --no-configuration-cache :app:testDebugUnitTest` |
| build debug APK | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Debug -OutputType Apk` |
| build release APK | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Release -OutputType Apk` |
| package release AAB | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Release -OutputType Aab` |
| dev smoke | `powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -BuildType Debug -OutputType Apk -Install` |
| installazione pulita e start | `powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -CleanInstall` |
| start senza reinstallare | `powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -NoInstall` |
| screenshot demo | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_screenshots.ps1` |
| screenshot demo su device USB | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_screenshots.ps1 -UseConnectedDevice` |
| video demo operatore | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_video.ps1` |
| video demo su device USB | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_video.ps1 -UseConnectedDevice` |
