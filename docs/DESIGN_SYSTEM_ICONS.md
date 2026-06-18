# Guida all'Uso delle Icone - Design System

Per garantire coerenza visiva e facilità d'uso (affordance) in tutta l'applicazione OnlyTechTake, seguire rigorosamente questa mappatura tra icone e azioni.

L'app utilizza **Material 3 (M3)**. Dove possibile, preferire le icone della libreria `Icons.Default`.

---

## 1. Navigazione e Struttura UI

| Icona | Simbolo | Utilizzo | Posizionamento |
| :--- | :--- | :--- | :--- |
| `Icons.Default.Home` | Casa | Schermata principale (Dashboard) | Bottom Bar / Nav Rail |
| `Icons.Default.Search` | Lente | Ricerca globale o filtri | AppBar / SearchBar leading icon |
| `Icons.AutoMirrored.Filled.List` | Elenco | Liste di dati (es. Commesse) | Bottom Bar / Nav Rail |
| `Icons.Default.ArrowBack` | Freccia SX | Torna alla schermata precedente | AppBar (lato sinistro) |
| `Icons.Default.Close` | X | Chiudi popup, dialog o reset ricerca | In alto a destra o dentro campi input |
| `Icons.Default.MoreVert` | 3 Puntini | Menu contestuale (Kebab menu) | Action bar per azioni secondarie |

---

## 2. Azioni e Gestione Dati (CRUD)

| Icona | Simbolo | Utilizzo | Colore Consigliato |
| :--- | :--- | :--- | :--- |
| `Icons.Default.Add` | Più (+) | Crea un nuovo elemento o inserimento | `primary` |
| `Icons.Default.Edit` | Matita | Modifica un elemento esistente | `primary` o `onSurfaceVariant` |
| `Icons.Default.Delete` | Cestino | **Eliminazione definitiva** | `error` (Rosso) |
| `Icons.Default.Block` | Divieto | Sospensione/Blocco (non eliminazione) | `error` |
| `Icons.Default.Check` | Spunta | Conferma, Salva, Applica o Successo | `success` |
| `Icons.Default.ContentCopy` | Fogli | Copia negli appunti (es. codici recovery) | `onSurfaceVariant` |

---

## 3. Interazione e Comunicazione

| Icona | Simbolo | Utilizzo | Esempio |
| :--- | :--- | :--- | :--- |
| `Icons.Default.Share` | Pallini | Condivisione nativa Android | Invio PDF a terze app |
| `Icons.AutoMirrored.Filled.Message` | Messaggio | Invio comunicazioni (WhatsApp) | Invia stato al cliente |
| `Icons.Default.PhotoCamera` | Camera | Acquisizione foto o media | All'interno dei form commessa |
| `Icons.Default.AttachFile` | Graffetta | Selezione file da memoria locale | Allegati documenti |
| `Icons.Default.Refresh` | Rotazione | Ripristina, Ruota immagine o Ricarica | Editor foto o statistiche |

---

## 4. Impostazioni e Stato

| Icona | Simbolo | Utilizzo |
| :--- | :--- | :--- |
| `Icons.Default.Settings` | Ingranaggio | Pannello configurazione o Setup |
| `Icons.Default.AccountCircle` | Sagoma | Gestione Utenti, Login o Profilo |
| `Icons.Default.Info` | Lettera "i" | Tooltip, dettagli tecnici o documentazione |
| `Icons.Default.Storage` | Disco | Gestione memoria e pulizia cache |
| `Icons.Default.Backup` | Cloud/Up | Backup dati e esportazioni |

---

## Regole d'Oro per l'UI/UX

1.  **Touch Target**: Ogni icona cliccabile (`AppIconButton`) deve avere un'area di tocco minima di **48x48dp**.
2.  **Affiancamento Input**: Se un pulsante icona è affiancato a un `AppTextField`, deve avere un'altezza di **56dp** e un offset superiore di **8dp** per allinearsi al box di testo.
3.  **Feedback**: Le azioni distruttive (Cestino/Delete) devono **sempre** aprire un `AppDialog` di conferma prima di procedere.
4.  **Stato Disabilitato**: Se un'azione non è permessa, ridurre l'opacità dell'icona (alpha 0.38f) e disabilitare il click.
