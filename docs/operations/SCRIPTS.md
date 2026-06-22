# Script PowerShell

Gli script Android di ambiente, Gradle, build e avvio sono centralizzati in `scripts\powershell\android`; `scripts\powershell\support` contiene le funzioni condivise. `scripts\powershell\project` contiene check di progetto, qualità, pulizia e `scripts\powershell\assets` la pipeline asset (demo e social).

Tutti gli script sono ora **interattivi per impostazione predefinita**: se lanciati senza parametri obbligatori, mostrano un menu o richiedono conferme. Per l'uso in pipeline o script automatizzati, è disponibile lo switch `-NonInteractive`.

Gli script calcolano automaticamente la root del progetto dal proprio percorso, quindi funzionano anche se il terminale non è aperto nella root. Il rilevamento accetta JDK/JBR 17 o 21 e ignora Java 26; la compilazione usa il toolchain Java 21 configurato da Gradle.

| script | path | purpose | notes |
|---|---|---|---|
| Check ambiente Android | `scripts\powershell\android\check_env.ps1` | Verifica JDK supportato, Gradle Wrapper, Android SDK Platform 37, Build Tools e adb. | Mostra banner e attende invio se interattivo. |
| Gradle wrapper Android | `scripts\powershell\android\gradle.ps1` | Esegue `gradlew.bat` con JDK/SDK risolti dallo script. | Se interattivo, propone un menu di task comuni. |
| Build installer Android | `scripts\powershell\android\build_installer.ps1` | Produce APK o AAB tramite Gradle Wrapper e opzionalmente installa APK via adb. | Menu interattivo per Flavor, BuildType e OutputType. |
| Start app Android | `scripts\powershell\android\start_app.ps1` | Installa l'APK selezionato e avvia `MainActivity` via adb. | Menu interattivo per selezione Flavor e BuildType. |
| Clean repository | `scripts\powershell\project\clean_repo.ps1` | Rimuove output temporanei, cache e artifact generati dentro il repository. | Richiede conferme per DryRun, IDE cache e Gradle stop se interattivo. |
| Quality check | `scripts\powershell\project\quality.ps1` | Esegue Spotless/ktlint su tutti i sorgenti configurati e Android Lint su tutte le varianti debug/release. | Default interattivo con prompt per autofix; supporta `-Apply`, `-NonInteractive` e `-SkipLint`. |
| Check progetto | `scripts\powershell\project\check_project.ps1` | Esegue lint, compile Kotlin debug, test unitari, build APK debug e, senza `-SkipReleaseBuild`, build APK release firmata. | Menu interattivo per Flavor e opzioni test/release. |
| Audit vulnerabilità dipendenze | `scripts\powershell\project\check_dependency_vulnerabilities.ps1` | Interroga OSV sulle coordinate Maven presenti nei lockfile Gradle. | Chiede conferma prima di avviare la scansione online. |
| Asset unificati | `scripts\powershell\assets\create_demo_assets.ps1` | Genera screenshot (storie/feed) e video MP4 montati. Centralizza demo e social. | Menu per selezione asset/caso specifico se non indicati. |
| Registrazione demo device | `scripts\powershell\assets\record_demo_device.ps1` | Registra video MP4 manuali o automatici tramite `adb screenrecord`. | Menu per selezione scenario demo se non indicato. |

## Comandi per obiettivo

| Obiettivo | Comando |
|---|---|
| setup / env | `powershell -ExecutionPolicy Bypass -File scripts\powershell\android\check_env.ps1` |
| clean repo (anteprima) | `powershell -ExecutionPolicy Bypass -File scripts\powershell\project\clean_repo.ps1 -DryRun` |
| clean repo | `powershell -ExecutionPolicy Bypass -File scripts\powershell\project\clean_repo.ps1` |
| quality check | `powershell -ExecutionPolicy Bypass -File scripts\powershell\project\quality.ps1` |
| quality autofix | `powershell -ExecutionPolicy Bypass -File scripts\powershell\project\quality.ps1 -Apply` |
| asset completi (demo + social) | `powershell -ExecutionPolicy Bypass -File scripts\powershell\assets\create_demo_assets.ps1` |
| asset Demo separati | `powershell -ExecutionPolicy Bypass -File scripts\powershell\assets\create_demo_assets.ps1 -Flavor Demo` |
| asset su device USB fisico | `powershell -ExecutionPolicy Bypass -File scripts\powershell\assets\create_demo_assets.ps1 -UseConnectedDevice` |
| registrazione device USB guidata | `powershell -ExecutionPolicy Bypass -File scripts\powershell\assets\record_demo_device.ps1 -UseConnectedDevice` |

## Manutenzione asset demo

[`DEMO_ASSET_WORKFLOW.md`](DEMO_ASSET_WORKFLOW.md) descrive il flusso completo, la corrispondenza tra test strumentati e output e il registro degli impatti. Dopo modifiche a UI, navigazione, copy, test tag, permessi, schema o seed demo, aggiornare insieme scenari, script, helper e `demo\video\operator-intake.story.json`; riportare in `PROJECT_STATUS.json` solo i residui ancora incompleti.
