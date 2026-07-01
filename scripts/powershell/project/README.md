# Documentazione Script Progetto (PowerShell)

Questa cartella contiene gli script per il mantenimento della qualità, della sicurezza e della salute generale del progetto.

---

## 1. quality.ps1
Esegue linting, controllo della formattazione e correzioni automatiche.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-Apply` | Switch | Applica automaticamente le correzioni di formattazione (Spotless). |
| `-SkipLint` | Switch | Salta l'analisi statica di Android Lint (più veloce). |
| `-Flavor` | String | Variante su cui eseguire i check (default: `Full`). |
| `-AllVariants` | Switch | Esegue i controlli su tutte le varianti (molto lento). |

### Cosa fa
Verifica che il codice rispetti gli standard di stile utilizzando Spotless (ktlint) ed esegue Android Lint per individuare bug potenziali o problemi di performance.

---

## 2. check_project.ps1
Esegue un controllo di salute completo del progetto.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-Flavor` | String | Variante dell'app da testare. |
| `-AndroidTests` | Switch | Esegue anche i test strumentati (richiede device). |
| `-SkipReleaseBuild` | Switch | Salta la verifica della build di release firmata. |

### Cosa fa
Combina diversi controlli (Lint, Compile, Unit Test, Assemble) in un'unica operazione per garantire che il progetto sia in uno stato stabile.

---

## 3. clean_repo.ps1
Esegue una pulizia profonda e sicura del repository su Windows.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-DryRun` | Switch | Mostra solo cosa verrebbe eliminato senza procedere. |
| `-Deep` | Switch | Rimuove anche le cache locali di Gradle e Kotlin (`.gradle`, `buildSrc/build`). |
| `-ResetIdeMetadata` | Switch | Ripristina i metadati di Android Studio (non l'intera cartella `.idea`). |
| `-StopGradle` | Switch | Ferma i daemon Gradle prima della pulizia. |

### Cosa fa
Elimina i file temporanei e i build output. Risolve problemi di "file in uso" o cache corrotte che spesso affliggono lo sviluppo Android su Windows.

---

## 4. check_dependency_vulnerabilities.ps1
Scansiona le dipendenze del progetto alla ricerca di vulnerabilità note.

### Parametri
| Parametro | Tipo | Descrizione |
| :--- | :--- | :--- |
| `-OsvApiUrl` | String | Endpoint dell'API OSV (default: `api.osv.dev`). |

### Cosa fa
Analizza i file di lock di Gradle e interroga il database OSV (Open Source Vulnerabilities) per verificare se le librerie utilizzate hanno problemi di sicurezza noti. Richiede connessione internet.
