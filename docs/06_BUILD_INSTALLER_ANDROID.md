# Build, installazione e verifica Android su Windows

Guida operativa supportata per Windows + PowerShell. Gli script in
`scripts\windows` calcolano la root del progetto dal proprio percorso; il
comando diretto `.\gradlew.bat` va invece eseguito dalla root.

## Prerequisiti

- PowerShell.
- Android Studio recente.
- JDK 17+ o Android Studio JBR.
- Android SDK Platform `android-36`.
- Android SDK Build Tools.
- Android Platform Tools per `adb`.
- Debug USB e autorizzazione PC sul device per installazione, start e instrumentation test.

Verifica ambiente:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_android_env.ps1
```

Il comando termina con exit code non zero se manca l'ambiente minimo per Gradle.

## Check progetto

Check locale standard senza signing release:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild
```

Check locale standard con instrumentation test su device collegato:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -SkipReleaseBuild -AndroidTests
```

Check completo con release firmata:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1
```

Test unitari:

```powershell
.\gradlew.bat --no-configuration-cache :app:testDebugUnitTest
```

Audit vulnerabilita' dipendenze tramite OSV:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\check_dependency_vulnerabilities.ps1
```

I task dello script `check_project.ps1` sono eseguiti in sequenza per ridurre i file lock Gradle/Kotlin su Windows.

## Build

Debug APK:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Debug -OutputType Apk
```

Release APK:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Apk
```

Release AAB:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Aab
```

Rigenerazione senza cancellare `app\build`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Apk -RerunTasks
```

Clean + build:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Release -OutputType Apk -Clean
```

Build debug e installazione su device:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\build_android_installer.ps1 -BuildType Debug -OutputType Apk -Install
```

## Output

- `app\build\outputs\apk\debug\app-debug.apk`
- `app\build\outputs\apk\release\app-release.apk` quando il signing locale è configurato.
- `app\build\outputs\bundle\release\app-release.aab` quando il signing locale è configurato.

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

## Installazione e start

Installare e avviare l'ultimo APK debug:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\start_android_app.ps1
```

Avviare senza reinstallare:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\start_android_app.ps1 -NoInstall
```

Installare un APK specifico:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\start_android_app.ps1 -ApkPath app\build\outputs\apk\debug\app-debug.apk
```

## Troubleshooting

- `gradlew.bat non trovato`: eseguire dalla root del progetto.
- `Java non trovato` o `javac non trovato`: configurare JDK 17+ o Android Studio JBR.
- `Platform android-36 non trovata`: installare Android SDK Platform 36 da Android Studio SDK Manager.
- `Android SDK Build Tools non trovati`: installare Android SDK Build Tools da Android Studio SDK Manager.
- `adb non trovato`: installare Android Platform Tools o aggiungere `platform-tools` al `PATH`.
- Nessun device in `adb devices`: abilitare debug USB e autorizzare il computer sul device.
- Build release bloccata per signing: configurare `keystore.properties` e verificare che `storeFile` punti a un keystore esistente.
- File lock Gradle/Kotlin: eseguire `.\gradlew.bat --stop`, chiudere build concorrenti e rilanciare gli script in sequenza.
