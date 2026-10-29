# Istruzioni del repository: OnlyTechTake

<!-- secondbrain:repository:begin -->
Direttive comuni applicate: revisione **4.0** — 2026-10-29. <!-- secondbrain:source global=7585B84CED201216722B7E6646C3D2845045FEEE7E44C889E92E68A5E1975A4A workstation=2BC7FB0217FB690B8CEAB7E3DD85AAB916BA4CF3016533BC9E1D02477CE9321B -->SecondBrain: integrazione opzionale della conoscenza e delle direttive comuni. Risolvi SECONDBRAIN_HOME dallʼambiente utente della postazione; nessun percorso personale è conservato in questo repository. Se il clone privato non è disponibile, prosegui con le istruzioni tecniche locali e segnala lʼintegrazione non configurata; non creare memorie o stub sostitutivi. Dal clone SecondBrain usa scripts/prompts.ps1 -Context -RepositoryPath <checkout-corrente> per risolvere il dossier e il suo index.md. Carica indice, contesto sintetico, memoria essenziale e proiezione task pertinente; approfondimenti e storico soltanto quando servono. Memorie private, tracker interni, backup e output operativi risiedono in SecondBrain. Conserva qui codice, guide e asset pubblicabili. Le direttive non autorizzano commit, push, cancellazioni di artefatti o task applicativi impliciti.
<!-- secondbrain:repository:end -->

Revisione specifica del progetto 2.5 — 2026-10-25. Vincoli specifici del progetto; i contenuti comuni sono gestiti separatamente.

## Contesto

- **Scopo**: Applicazione mobile Android nativa per la gestione interventi tecnici, presa in carico merci con foto/video, firma digitale su schermo e report PDF per centri di assistenza tecnica.
- **Modalità**: Greenfield (semplificazione aggressiva per architettura minimale; eliminazione shim temporanei e dead code)
- **Target Applicativo**: Mobile Android (Tablet e Smartphone) con interfaccia touch reattiva e supporto bilingue (Inglese default, Italiano).
- **Stack & Toolchain**: Kotlin, Jetpack Compose, Android SDK, SQLCipher, Gradle (Kotlin DSL), JUnit, AndroidX Test e PowerShell (`pwsh`).

## Vincoli specifici

  * Validazione rigorosa degli input e schema dei dati per import/export.
  * **Segreti & Keystore**: Nessuna chiave privata o password di keystore nel repository. `keystore.properties.example` contiene placeholder; le credenziali reali risiedono in `keystore.properties` ignorato da Git.
  * Database locale crittografato SQLCipher per totale riservatezza e funzionamento locale offline-first.
  * Accessibilità WCAG 2.2 AA SC 2.5.8 (Target Size minimo 24x24 px, target touch primari raccomandati 44x44 px).
  * Funzionamento offline-first completo: nessuna dipendenza da connessione di rete per intake, consultazione o firma.

## Comandi e verifica

Comandi verificati staticamente contro i file del progetto; questa revisione delle direttive non certifica l'esecuzione di build o test applicativi. Consultare anche i gate della CI, quando presenti.

| Attività | Comando | Ambiente | Evidenza e limiti |
|---|---|---|---|
| **Build Debug** | `.\gradlew.bat assembleDebug` | pwsh / root | Compilazione APK in modalità Debug |
| **Targeted checks** | `.\gradlew.bat testDebugUnitTest` | pwsh / root | Esecuzione rapida degli unit test Android |
| **Full checks** | `.\gradlew.bat check` | pwsh / root | **Gate obbligatorio pre-commit** (lint Android + test unitari completi) |
| **Build Release** | `.\gradlew.bat assembleRelease` | pwsh / root | Compilazione APK release firmato |

## Riferimenti e collaborazione

- Consultare README e documentazione effettivamente presenti; versioni dai manifest/lockfile, non da ipotesi sullo stack.
- 02-areas/products/OnlyTechTake/memory.md e 02-areas/products/OnlyTechTake/context.md (nel dossier SecondBrain) conservano decisioni tecniche durevoli, gotcha e architettura; aggiornarli a fine task.
- Tracker autorevole nel dossier SecondBrain: `02-areas/products/OnlyTechTake/tracker.json`, dichiarato in repository.json. Conservare schema JSON v1 e storico; build e test restano indipendenti da SecondBrain.
- Eseguire i controlli previsti per la modifica prima di un commit richiesto. Nessun commit o push automatico.
- Preservare dati persistenti e configurazioni esterne; non estendere la manutenzione a operazioni distruttive non autorizzate.
