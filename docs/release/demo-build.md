# Build Demo e Distribuzione Beta

Questa guida descrive come generare build Demo e Full dell'app OnlyTechTake Repair App per test autorizzati e distribuzione beta.

## Architettura Build

Il modulo Android usa due product flavor Gradle reali:

| Caratteristica | Flavor `demo` | Flavor `full` |
| :--- | :--- | :--- |
| Application ID release | `com.onlytechtake.repairapp.demo` | `com.onlytechtake.repairapp` |
| Application ID debug | `com.onlytechtake.repairapp.demo.debug` | `com.onlytechtake.repairapp.debug` |
| Nome app | OnlyTechTake Demo | OnlyTechTake |
| Limite commesse | 10 | Nessun limite hardcoded |
| Export/backup | Bloccati dal codice demo dove previsto | Disponibili secondo permessi admin |
| Uso | Test, beta shop, training | Pubblicazione e uso completo |

La release richiede `keystore.properties` locale configurato nella root. Il keystore e le password non devono essere committati.

## Comandi di Generazione

Usare PowerShell su Windows. Gli script rilevano JDK/SDK e lanciano i task Gradle variant-specific.

### APK Demo release

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Demo -BuildType Release -OutputType Apk
```

Output:

```text
app\build\outputs\apk\demo\release\app-demo-release.apk
```

### APK Full release

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Apk
```

Output:

```text
app\build\outputs\apk\full\release\app-full-release.apk
```

### AAB Full release per Play Console

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Aab
```

Output:

```text
app\build\outputs\bundle\fullRelease\app-full-release.aab
```

## Checklist Pre-Invio Demo

Prima di inviare un APK a un tester:

- Verificare `versionCode` e `versionName` in `app\build.gradle.kts`.
- Generare una build release firmata con keystore locale non tracciato.
- Installare su device pulito o cancellare i dati del package target.
- Non usare dati reali nei materiali demo o negli screenshot.
- Confermare che il tester sappia che l'app e' `LOCAL_ONLY` e che i dati restano sul device.

## Reset dei Dati

Per riportare l'app allo stato iniziale durante una demo:

1. Accedere come admin.
2. Aprire la sezione Admin.
3. Usare Reset totale dati.
4. Digitare `RESET` nel campo di conferma.

In alternativa, per test su device collegato via adb, usare:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -Flavor Demo -BuildType Release -CleanInstall
```
