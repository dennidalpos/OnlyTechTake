# Documentazione Script Android (PowerShell)

Questa cartella contiene gli script per la gestione del ciclo di vita dell'applicazione Android (build, installazione, esecuzione).

---

## 1. build_installer.ps1
Genera APK o AAB e opzionalmente li installa sul dispositivo.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-Flavor` | String | Variante dell'app (`Full`, `Demo`, `Showcase`). |
| `-BuildType` | String | Tipo di build (`Debug`, `Release`). |
| `-OutputType` | String | Formato di output (`Apk`, `Aab`). |
| `-Clean` | Switch | Esegue un `clean` prima della build. |
| `-RerunTasks` | Switch | Forza la ri-esecuzione dei task Gradle. |
| `-Install` | Switch | Installa l'artifact generato sul dispositivo. |
| `-NonInteractive`| Switch | Salta le richieste di input utente. |

### Cosa fa
Compila il progetto Android utilizzando Gradle Wrapper, gestendo la selezione del flavor e del tipo di build. Può anche gestire l'installazione automatica post-compilazione.

---

## 2. check_env.ps1
Verifica che l'ambiente di sviluppo sia configurato correttamente per la CLI.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-NonInteractive`| Switch | Esegue i controlli senza messaggi interattivi. |

### Cosa fa
Controlla la presenza e la versione di JDK (richiesto 17 o 21) e Android SDK. Configura le variabili d'ambiente necessarie per l'esecuzione degli altri script.

---

## 3. gradle.ps1
Wrapper per Gradle che assicura l'uso del JDK corretto.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-Stop` | Switch | Ferma il daemon di Gradle (`--stop`). |
| `-NonInteractive`| Switch | Evita il menu di selezione dei task comuni. |

### Cosa fa
Esegue il comando `./gradlew` con l'ambiente pre-configurato. Se eseguito senza parametri, offre un menu con i task più frequenti (clean, assemble, test, lint).

---

## 4. start_app.ps1
Installa e avvia l'applicazione sul dispositivo collegato.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-Flavor` | String | Variante dell'app (`Full`, `Demo`, `Showcase`). |
| `-BuildType` | String | Tipo di build (`Debug`, `Release`). |
| `-ApkPath` | String | Percorso manuale del file APK (opzionale). |
| `-NoInstall` | Switch | Tenta di avviare l'app senza reinstallarla. |
| `-CleanInstall` | Switch | Disinstalla l'app prima di installare la nuova versione. |

### Cosa fa
Utilizza `adb` per installare il pacchetto APK e avviare l'activity principale dell'applicazione.
