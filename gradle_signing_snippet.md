# Configurazione Gradle signing release

Il progetto contiene gia' la configurazione signing opzionale in
`app/build.gradle.kts`. Per produrre un APK release firmato, creare solo un file
locale `keystore.properties` nella root.

Esempio:

```properties
storeFile=C:\Users\TUO_UTENTE\repair-app-release.jks
storePassword=CAMBIA_QUESTA_PASSWORD
keyAlias=repairapp
keyPassword=CAMBIA_QUESTA_PASSWORD
```

Non committare:

```text
keystore.properties
*.jks
*.keystore
```
