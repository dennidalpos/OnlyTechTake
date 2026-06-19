# Build, installazione e verifica Android su Windows

Guida operativa supportata per Windows + PowerShell. Gli script ufficiali in
`scripts\android` calcolano la root del progetto dal proprio percorso; i wrapper
in `scripts\windows` restano compatibili con i comandi storici. Il
comando diretto `.\gradlew.bat` va invece eseguito dalla root e usa la JVM gia'
configurata nel processo PowerShell. Il progetto compila con toolchain Java 21;
usare JDK/JBR 21 evita che Gradle debba risolverlo durante la prima build.

## Prerequisiti

- PowerShell.
- Android Studio recente.
- JDK/JBR 21 consigliato. JDK 17 e' accettato dagli script come JVM di avvio solo se Gradle puo' risolvere il toolchain Java 21.
- Android SDK Platform `android-37`.
- Android SDK Build Tools.
- Android Platform Tools per `adb`.
- Debug USB e autorizzazione PC sul device per installazione, start e instrumentation test.

Verifica ambiente:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1
```

Il comando termina con exit code non zero se manca l'ambiente minimo per Gradle.

## Check progetto

Check locale standard senza signing release:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild
```

Check locale standard con instrumentation test su device collegato:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild -AndroidTests
```

Check completo con release firmata:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full
```

Test unitari:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\gradle.ps1 -- --no-configuration-cache :app:testFullDebugUnitTest
```

Audit vulnerabilita' dipendenze tramite OSV:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_dependency_vulnerabilities.ps1
```

I task dello script `check_project.ps1` sono eseguiti in sequenza per ridurre i file lock Gradle/Kotlin su Windows.

## Setup fresh-install consigliato

Per consegna o verifica su device dopo modifiche incompatibili con dati locali:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -CleanInstall
```

Al primo avvio pulito l'app mostra il setup admin locale. I gate locali sopra sono allineati alla CI principale `.github/workflows/android.yml`; i test strumentati sono coperti dalla workflow dedicata `.github/workflows/android-instrumented.yml` oppure dal comando locale con `-AndroidTests` quando e' disponibile un device/emulatore.

## Build

I flavor Gradle disponibili sono `full`, `demo` e `showcase`; negli script il parametro e' `-Flavor Full|Demo|Showcase` e il default e' `Full`.

Debug APK Full:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk
```

Debug APK Demo:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Demo -BuildType Debug -OutputType Apk
```

Debug APK Showcase:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Showcase -BuildType Debug -OutputType Apk
```

Release APK Full:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Apk
```

Release AAB Full:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Aab
```

Rigenerazione senza cancellare `app\build`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Apk -RerunTasks
```

Clean + build:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Release -OutputType Apk -Clean
```

Build debug e installazione su device:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\build_installer.ps1 -Flavor Full -BuildType Debug -OutputType Apk -Install
```

## Output

- `app\build\outputs\apk\full\debug\app-full-debug.apk`
- `app\build\outputs\apk\demo\debug\app-demo-debug.apk`
- `app\build\outputs\apk\showcase\debug\app-showcase-debug.apk`
- `app\build\outputs\apk\full\release\app-full-release.apk`
- `app\build\outputs\apk\demo\release\app-demo-release.apk`
- `app\build\outputs\apk\showcase\release\app-showcase-release.apk`
- `app\build\outputs\bundle\fullRelease\app-full-release.aab`
- `app\build\outputs\bundle\demoRelease\app-demo-release.aab`
- `app\build\outputs\bundle\showcaseRelease\app-showcase-release.aab`

## Signing release

Il signing release è necessario per un APK finale installabile come release.

Creare `keystore.properties` nella root del progetto:

```properties
storeFile=C:\Percorso\locale\repair-app-release.jks
storePassword=CAMBIA_QUESTA_PASSWORD
keyAlias=repairapp
keyPassword=CAMBIA_QUESTA_PASSWORD
```

Regole:

- non committare `keystore.properties`;
- non committare file `.jks` o `.keystore`;
- non scrivere password o percorsi personali in README, docs o script;
- se `keystore.properties` manca o non punta a un keystore valido, la build release fallisce prima della compilazione release.

## Play Protect e distribuzione

Per ridurre gli avvisi Play Protect:

- distribuire build release firmate, non APK debug;
- mantenere stabile `applicationId` e certificato di firma tra le versioni;
- preferire Android App Bundle caricato su Play Console, Internal Testing o Internal App Sharing;
- se serve distribuire anche fuori Play, scaricare da Play Console l'APK universale firmato da Google oppure usare una chiave release gestita in modo coerente con Play App Signing.

Un APK installato manualmente da sorgenti sconosciute e firmato con una chiave locale non riconosciuta puo' comunque mostrare avvisi Play Protect anche se il codice dell'app e' legittimo.

## Installazione e start

Installare e avviare l'ultimo APK debug:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1
```

Installazione pulita e avvio dell'ultimo APK debug:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -CleanInstall
```

Questo e' il comando richiesto dopo modifiche incompatibili allo schema Room in
questa fase fresh-app: disinstalla il package, elimina i dati locali precedenti,
installa l'APK debug aggiornato e avvia `MainActivity`.

Avviare senza reinstallare:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -NoInstall
```

Installare un APK specifico:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\android\start_app.ps1 -Flavor Full -ApkPath app\build\outputs\apk\full\debug\app-full-debug.apk
```

Per il flavor Demo usare `-Flavor Demo`; per il flavor Showcase usare `-Flavor Showcase`. I package debug sono `com.onlytechtake.repairapp.debug`, `com.onlytechtake.repairapp.demo.debug` e `com.onlytechtake.repairapp.showcase.debug`.

## Troubleshooting

- `gradlew.bat non trovato`: eseguire dalla root del progetto.
- `Java non trovato` o `javac non trovato`: configurare JDK/JBR 21.
- Java 26 nel `PATH`: gli script lo ignorano e preferiscono Android Studio JBR o un JDK 17/21 supportato; la compilazione usa Java 21.
- Errore di risoluzione toolchain con JDK 17: consentire a Gradle di scaricare Java 21 oppure impostare `JAVA_HOME` su un JDK/JBR 21 locale.
- `Platform android-37 non trovata`: installare Android SDK Platform 37 da Android Studio SDK Manager.
- `Android SDK Build Tools non trovati`: installare Android SDK Build Tools da Android Studio SDK Manager.
- `adb non trovato`: installare Android Platform Tools o aggiungere `platform-tools` al `PATH`.
- Nessun device in `adb devices`: abilitare debug USB e autorizzare il computer sul device.
- Build release bloccata per signing: configurare `keystore.properties` e verificare che `storeFile` punti a un keystore esistente.
- File lock Gradle/Kotlin: eseguire `.\gradlew.bat --stop`, chiudere build concorrenti e rilanciare gli script in sequenza.
