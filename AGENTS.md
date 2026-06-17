# AGENTS.md - Mobile App Development Guide

Istruzioni per agenti AI che lavorano su questo repository e, come base
riutilizzabile, su progetti mobile Android / Apple trattati come fresh project.

Ambiente locale primario: **Windows 11 + PowerShell**.

---

## 1. Scope & Precedence

1. La richiesta corrente dell'utente ha precedenza su questo file.
2. Eventuali `AGENTS.md` annidati hanno precedenza nella propria sottocartella.
3. Le convenzioni gia' presenti nel repository prevalgono su preferenze generiche.
4. In caso di conflitto, scegliere la modifica sicura meno invasiva e segnalarlo nel report finale.

Applicare queste regole solo ai file necessari alla richiesta. Non introdurre
refactor, dipendenze, migrazioni, API pubbliche, configurazioni o workflow nuovi
se non sono richiesti o chiaramente necessari.

---

## 2. Environment

- Usare PowerShell e comandi compatibili Windows 11 come default.
- Preferire path quoted, UTF-8, API cross-platform (`pathlib`, `java.nio.file`, `kotlin.io.path`, `Node path`) e separatori gestiti dal runtime.
- Non assumere Bash, WSL, GNU tools, `/tmp`, `/home`, `chmod`, `sed -i` o `rm -rf` in locale.
- Usare il Gradle Wrapper, gli script del repository e gli strumenti IDE-native quando disponibili.
- Non salvare nei file tracciati path personali, credenziali, keystore, token o configurazioni locali.

---

## 3. Fresh Project Policy

Trattare il progetto come fresh fino al primo lancio pubblico/produzione indicato
dal repository o dall'utente.

- Evitare debito legacy: niente shim, compat layer, workaround storici o supporto per formati obsoleti se non richiesti.
- Non aggiungere migrazioni dati o compatibilita' retroattiva prima del lancio, salvo approvazione esplicita.
- Preferire architettura pulita, diretta, testabile e allineata agli standard correnti della piattaforma.
- Rimuovere o aggiornare pattern obsoleti quando sono nel perimetro della modifica, senza fare riscritture ampie non richieste.
- Se il repository mostra vincoli reali gia' in uso, rispettarli e fare la modifica piu' piccola che mantiene il sistema coerente.

---

## 4. Architecture

- Organizzare il codice per feature e layer chiari: UI, ViewModel/state holder, use case quando utili, repository, data source, persistenza/servizi.
- Tenere le UI sottili: niente logica di business pesante in Composable, View, Controller o SwiftUI View.
- Usare dependency injection esistente prima di introdurre nuovi framework o container.
- Separare contratti domain/repository dalle implementazioni locali per permettere futuri backend o data source remoti senza rompere il dominio.
- Aggiungere astrazioni solo quando riducono complessita' reale, duplicazione significativa o accoppiamento concreto.
- Evitare nomi generici come `utils`, `helpers`, `common` se il repository non li usa gia' con uno scopo chiaro.
- Mantenere i file focalizzati su una responsabilita' e testabili con il framework gia' presente.

---

## 5. Platform Rules

### Android

- Preferire Kotlin, Gradle Wrapper, Jetpack Compose, Material 3 e le convenzioni Android correnti del repository.
- Usare componenti e tema del design system esistente prima dei componenti Material diretti.
- Spostare tutte le stringhe visibili all'utente in `strings.xml`; non introdurre hardcoded UI copy.
- Usare Room/DataStore/Storage Access Framework secondo le policy gia' presenti.
- Usare `FileProvider`, storage privato app e permessi runtime minimi; evitare permessi storage larghi se SAF e' sufficiente.
- Usare version catalog Gradle per nuove versioni; non spargere numeri versione nei build file.
- Validare flavor/build variant impattati dalla modifica.

### Apple / iOS

Applicare queste regole solo se il repository contiene davvero target iOS, Xcode,
Swift, SwiftUI, Kotlin Multiplatform o Compose Multiplatform.

- Non inventare cartelle, target o workflow Apple in un repository Android-only.
- Se presenti, rispettare struttura Xcode/SPM/KMP esistente, signing locale e asset catalog.
- Preferire Swift/SwiftUI moderni, string resources/localization, Keychain per segreti e storage sandboxed.
- Mantenere contratti condivisi stabili tra Android e iOS; evitare divergenze non necessarie nei flussi utente.

---

## 6. Data & Storage

- LOCAL_ONLY resta il default finche' non esiste un backend reale richiesto o documentato.
- Progettare repository e data source in modo sostituibile, ma non aggiungere stub cloud/API non usati.
- Ogni input esterno va validato; ogni output condiviso va prodotto tramite meccanismi sicuri della piattaforma.
- Non loggare dati sensibili, password, chiavi, token, contenuti personali o percorsi privati non necessari.
- Ogni cambio schema database richiede richiesta esplicita, documentazione e test fresh-install.
- Nei progetti fresh non aggiungere migrazioni conservative salvo richiesta esplicita o vincolo gia' documentato.

### Current Repo Profile

Questo repository, allo stato corrente, e' Android-only:

- Modulo: `:app`.
- Package: `com.onlytechtake.repairapp`.
- Flavor Gradle: `full`, `demo`, `showcase`.
- Stack: Kotlin, Compose, Material 3, Navigation Compose, Room, SQLCipher, DataStore, CameraX, Coroutines/Flow.
- Backend: assente; WhatsApp e' solo manuale.
- Database: Room `DATABASE_VERSION = 4`, schema esportati `1.json`, `2.json`, `4.json`.
- Persistenza: database locale cifrato SQLCipher, file privati in `filesDir/app_media`, export/import tramite SAF.
- Governance schema: non incrementare `DATABASE_VERSION`, non aggiungere migrazioni Room e non modificare schema senza richiesta esplicita dell'owner.
- Script canonici: `scripts\android`; `scripts\windows` contiene wrapper compatibili e pipeline demo/check.

---

## 7. UI/UX

- Seguire il design system del progetto, il tema app e i componenti condivisi.
- Preferire interfacce dense, operative e leggibili per app gestionali; evitare layout decorativi che rallentano il lavoro.
- Garantire stati completi: loading, empty, errore, conferma, successo, permessi negati e input non valido.
- Mantenere accessibilita', contrasto, touch target e testi localizzati.
- Dopo modifiche UI o flussi demo, aggiornare scenari strumentati e pipeline asset quando richiesto dal repository.

---

## 8. Workflow

1. Controllare `git status --short` prima di editare.
2. Leggere solo i file necessari: istruzioni applicabili, implementazione vicina, test, docs e config build.
3. Preservare modifiche utente non correlate; non fare revert di lavoro non proprio.
4. Fare modifiche atomiche, piccole e verificabili.
5. Aggiornare documentazione solo se cambiano comportamento, setup, architettura, release flow o asset demo.
6. Aggiornare `docs\DEMO_ASSET_WORKFLOW.md` ad ogni modifica di UI, navigazione, copy, test tag o dati demo che impatta la generazione di screenshot e video.
7. Aggiornare `PROJECT_STATUS.json` solo con residui reali, azionabili e ancora incompleti; non usarlo come changelog.
8. Non lasciare artifact temporanei, report generati o output build non richiesti nel working tree.

Per questo repository:

- Usare `scripts\android\check_env.ps1` per validare ambiente Android.
- Usare `scripts\android\gradle.ps1 -- <task>` quando serve Gradle con JDK/SDK risolti dallo script.
- Usare `scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild` come check locale standard senza signing release.
- Usare `docs\DEMO_ASSET_WORKFLOW.md` come fonte di verita' per screenshot/video demo, aggiornare il workflow se viene cambiato.

*Nota CI: In GitHub Actions è attiva la variabile `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true` per garantire la compatibilità con i futuri runtime.*

---

## 9. Verification

- Se cambia comportamento, aggiungere o aggiornare test usando il framework esistente.
- Eseguire il set minimo utile di lint, typecheck, unit test, build o test strumentati per il perimetro toccato.
- Per modifiche Android in questo repository, preferire:
  - `powershell -ExecutionPolicy Bypass -File scripts\android\check_env.ps1`
  - `powershell -ExecutionPolicy Bypass -File scripts\windows\check_project.ps1 -Flavor Full -SkipReleaseBuild`
  - test unitari mirati, per esempio `scripts\android\gradle.ps1 -- --no-configuration-cache :app:testFullDebugUnitTest`
- Eseguire test strumentati solo quando il cambio tocca UI, storage Android, permessi, SAF, CameraX, Room reale o flussi device.
- Per sole modifiche documentali, non serve build Gradle; verificare diff e formattazione.
- Se un check non puo' essere eseguito, indicare il motivo senza dichiararlo passato.

---

## 10. Documentation

- Mantenere `README.md` come ingresso rapido e `docs\README.md` come indice canonico della documentazione.
- Aggiornare documenti in `docs\` quando cambiano flussi funzionali, architettura, dati, sicurezza, backup, release o demo asset.
- Non aggiungere documentazione speculativa su backend, cloud, Apple/iOS o multipiattaforma se non c'e' implementazione o richiesta esplicita.
- Se una modifica lascia un rischio o lavoro residuo concreto, registrarlo in `PROJECT_STATUS.json` con priorita' e criterio di chiusura.

---

## 11. Security

- Non creare, stampare, committare o esporre segreti.
- Usare `.env.example`, file example o documentazione placeholder per configurazioni richieste.
- Tenere keystore, password, token, signing config locali e dati personali fuori dai file tracciati.
- Usare hashing password robusto, storage sicuro della piattaforma, query parametrizzate e validazione input.
- Prima di aggiungere dipendenze, verificare se esiste gia' una libreria o utility adatta; motivare ogni nuova dipendenza.
- Non introdurre integrazioni cloud, analytics, crash reporting o telemetria senza richiesta esplicita e documentazione privacy.

---

## 12. Final Reporting

Per task che modificano file, riportare:

1. Cosa e' cambiato.
2. File modificati.
3. Documentazione aggiornata o motivo per cui non serviva.
4. Check eseguiti e risultato.
5. Stato di `PROJECT_STATUS.json`.
6. Rischi o follow-up, inclusi impatti futuri su backend/cloud/multipiattaforma se rilevanti.

Essere fattuali. Non dichiarare il lavoro production-ready se i check pertinenti
non sono stati eseguiti o se restano limitazioni note.
