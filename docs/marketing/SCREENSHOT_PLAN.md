# Screenshot Plan e Guida Dati Demo

Strategia per la cattura e la produzione degli screenshot marketing per il Google Play Store e le landing page.

## Come catturare gli screenshot

1. **Modalità Demo**: Per catturare screenshot puliti con dati realistici senza dover inserire manualmente decine di record:
   - Aprire l'app nel flavor `Full` o `Demo`.
   - Nella schermata di **Login**, toccare il pulsante **"Avvia Modalità Demo"**.
   - L'app verrà popolata con dati fittizi in memoria (cliente "Marco Rossi", iPhone 13, ecc.).
2. **Setup Device**:
   - Orientamento: Verticale (9:16).
   - Risoluzione minima: 1080x1920 px.
   - Pulizia: Nascondere la barra delle notifiche o usare la "Modalità Demo" di Android Studio.
   - Tema: Coerente (Chiaro o Scuro) per tutto il set.
3. **Automazione**: Il repository include script in `scripts/windows/create_demo_assets.ps1` che utilizzano test strumentati per scattare screenshot automatici in percorsi predefiniti. Fare riferimento a `docs/DEMO_ASSET_WORKFLOW.md`.

## Piano Screenshot (Play Store)

Gli asset aggiornati generati localmente sono sotto `reports/demo-assets/screenshots`:

- `01-operatore-banco`: flusso operatore completo, login, intake, WhatsApp, PDF e chiusura.
- `02-commessa-nuovo-cliente-chiusura`: nuova commessa, avanzamento stati e timeline finale.
- `03-categorie-template`: configurazione categorie e template.
- `04-clienti-privato-azienda`: anagrafica cliente privato e azienda.
- `05-manutenzione-backup`: memoria, backup ed export Excel.

| # | Nome File | Schermata | Overlay Marketing (Testo Suggerito) | Dati Demo Consigliati |
|---|-----------|-----------|---------------------------|-----------------------|
| 1 | `01_dashboard` | Dashboard Home | **Tutto sotto controllo.** Statistiche e code in tempo reale. | 12 Utenti, 150 Clienti, 8 Commesse attive. |
| 2 | `02_new_order` | Step 1: Cliente | **Anagrafica rapida.** Gestisci privati e aziende. | Cliente: "Mario Rossi", Tipo: Privato. |
| 3 | `03_category` | Step 2: Categoria | **Catalogo flessibile.** Scegli il dispositivo da riparare. | Categoria: Smartphone > iPhone > Display. |
| 4 | `04_media` | Step 4: Media | **Documenta tutto.** Scatta foto ai danni estetici. | Foto di uno smartphone con vetro rotto. |
| 5 | `05_signature` | Step 5: Privacy | **Firma digitale.** Accettazione termini e privacy immediata. | Firma demo stilizzata "M. Rossi". |
| 6 | `06_detail` | Scheda Commessa | **Gestione workflow.** Avanzamento e note interne. | Stato: In Lavorazione. Note: "In attesa ricambio". |
| 7 | `07_whatsapp` | Anteprima WhatsApp | **Comunica velocemente.** Aggiorna il cliente con un click. | Messaggio: "Ciao Mario, il tuo iPhone è pronto!". |
| 8 | `08_backup` | Config Backup | **Dati sempre al sicuro.** Backup completi e export Excel. | Ultimo backup: "Oggi", Destinazione: provider documenti. |

## Checklist Grafica e Privacy

- [ ] **Risoluzione:** Almeno 1080x1920 px.
- [ ] **Privacy:** Verificare che nessun dato sensibile reale sia visibile (rubrica, notifiche, email admin).
- [ ] **No Trasparenza:** Le immagini per il Play Store (specialmente la Feature Graphic) non devono avere trasparenze.
- [ ] **Leggibilità:** Testi overlay grandi e contrastati, preferibilmente in alto o in basso per non coprire l'UI.

## Dati Demo di Riferimento

| Campo | Valore Demo |
| :--- | :--- |
| **Cliente** | Marco Rossi |
| **Telefono** | +39 333 000 0000 |
| **Dispositivo** | iPhone 13 Pro |
| **Problema** | Schermo rotto, touch parzialmente funzionante |
| **Diagnosi** | Display da sostituire, preventivo approvato |
| **Prezzo** | € 120,00 |
