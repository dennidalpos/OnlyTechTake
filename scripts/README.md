# Script PowerShell

Gli script ufficiali Android sono in `scripts\android`; `scripts\support` contiene le funzioni condivise. I vecchi script in `scripts\windows` restano wrapper compatibili o script Windows specifici per demo/check.

Gli script calcolano automaticamente la root del progetto dal proprio percorso, quindi funzionano anche se il terminale non e' aperto nella root. Il rilevamento JDK accetta Android Studio JBR o JDK 17/21 e ignora Java 26 nel `PATH`.

| script | path | purpose | notes |
|---|---|---|---|
| Check ambiente Android | `scripts\android\check_env.ps1` | Verifica JDK supportato, Gradle Wrapper, Android SDK Platform 37, Build Tools e adb. | Exit code non zero se l'ambiente minimo per Gradle non e' valido. |
| Gradle wrapper Android | `scripts\android\gradle.ps1` | Esegue `gradlew.bat` con JDK/SDK risolti dallo script. | Usare `--` prima degli argomenti Gradle. |
| Build installer Android | `scripts\android\build_installer.ps1` | Produce APK o AAB tramite Gradle Wrapper e opzionalmente installa APK via adb. | Supporta `-Flavor Full|Demo|Showcase`, `-BuildType`, `-OutputType`, `-Clean`, `-RerunTasks`, `-Install`; blocca release senza signing locale. |
| Start app Android | `scripts\android\start_app.ps1` | Installa l'APK selezionato e avvia `MainActivity` via adb. | Supporta `-Flavor Full|Demo|Showcase`, `-NoInstall`, `-ApkPath` e `-CleanInstall` per reinstallazione pulita. |
| Clean repository | `scripts\windows\clean_repo.ps1` | Rimuove output temporanei, cache e artifact generati dentro il repository. | Supporta `-DryRun`, `-StopGradle` e `-IncludeIde`; preserva `reports\demo-assets` e non tocca cache globali, SDK, keystore o configurazioni locali. |
| Quality check | `scripts\windows\quality.ps1` | Esegue Spotless/ktlint e Android Lint per linting, formatting e beautification. | Default check-only con prompt per autofix; supporta `-Apply`, `-NoPrompt`, `-SkipLint` e `-Flavor Full|Demo|Showcase`. |
| Check progetto | `scripts\windows\check_project.ps1` | Esegue lint, compile Kotlin debug, test unitari, build APK debug e, senza `-SkipReleaseBuild`, build APK release firmata. | Supporta `-Flavor Full|Demo|Showcase`; `-AndroidTests` esegue il connected test del flavor selezionato. |
| Audit vulnerabilita' dipendenze | `scripts\windows\check_dependency_vulnerabilities.ps1` | Interroga OSV sulle coordinate Maven presenti nei lockfile Gradle. | Separato dai check offline; non invia codice sorgente, solo group/artifact/version. |
| Asset demo unificati | `scripts\windows\create_demo_assets.ps1` | Genera screenshot verticali e video MP4 montati dagli scenari strumentati. | Richiede ffmpeg per i video; usa AVD `OnlyTechTakeDemo` o device fisico. Mappa completa in `docs\DEMO_ASSET_WORKFLOW.md`. |
| Registrazione demo device | `scripts\windows\record_demo_device.ps1` | Registra video MP4 manuali o automatici tramite `adb screenrecord`. | Supporta emulatore o device fisico, pulisce i dati app e riusa gli scenari strumentati mappati in `docs\DEMO_ASSET_WORKFLOW.md`. |

## Comandi per obiettivo

| Obiettivo | Comando |
|---|---|
| setup / env | `powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1` |
| clean repo (anteprima) | `powershell -ExecutionPolicy Bypass -File scripts\windows\clean_repo.ps1 -DryRun` |
| clean repo | `powershell -ExecutionPolicy Bypass -File scripts\windows\clean_repo.ps1` |
| quality check | `powershell -ExecutionPolicy Bypass -File scripts\windows\quality.ps1` |
| quality autofix | `powershell -ExecutionPolicy Bypass -File scripts\windows\quality.ps1 -Apply` |
| asset demo (screen + video) | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_assets.ps1` |
| asset demo su device USB | `powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_assets.ps1 -UseConnectedDevice` |
| registrazione demo USB guidata | `powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1` |

## Manutenzione asset demo

`docs\DEMO_ASSET_WORKFLOW.md` descrive il flusso completo, la corrispondenza tra test strumentati e output e il registro degli impatti. Dopo modifiche a UI, navigazione, copy, test tag, permessi, schema o seed demo, aggiornare insieme scenari, script, helper e `demo\video\operator-intake.story.json`; riportare in `PROJECT_STATUS.json` solo i residui ancora incompleti.
