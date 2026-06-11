# Demo Recording Guide

Repository: OnlyTechTakeRepairApp

## Prerequisites
- Physical device connected via USB.
- PowerShell from the repository root.
- `scripts\windows\record_demo_device.ps1`.
- App installed and data cleared before each recording (handled by script).
- `adb` available through Android Platform Tools.

## Recording Scenarios

### Demo 01: Setup Admin e gestione Operatori
1. Start recording with `powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -OutputFile demo_01_setup.mp4`.
2. Complete First Admin Setup (name, username, password).
3. Navigate to Operator Management.
4. Add a new Operator.
5. End recording (CTRL+C).

### Demo 02: Configurazione Categorie (3 livelli) e Template WhatsApp
1. Start recording with `powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -OutputFile demo_02_config.mp4`.
2. Go to Config > Categories.
3. Create a 3-level hierarchy (e.g., Smartphone -> Brand -> Model).
4. Go to Config > Templates.
5. Show/Edit a template.
6. End recording.

### Demo 03: Flusso Nuova Commessa (Cliente, Foto, Firma e Privacy)
1. Start recording with `powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -OutputFile demo_03_intake.mp4`.
2. Start New Order.
3. Enter Customer details.
4. Select Category.
5. Add Photos.
6. Capture Signature.
7. Confirm Privacy.
8. End recording.

### Demo 04: Gestione Operativa (Diagnosi, Preventivo e Notifiche WhatsApp)
1. Start recording with `powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -OutputFile demo_04_operation.mp4`.
2. Open the created order.
3. Record Diagnosis (Repairable).
4. Set Quote amount.
5. Prepare and "send" WhatsApp notification.
6. Record Customer Accept/Reject.
7. End recording.

### Demo 05: Chiusura Commessa, Consegna e PDF Riepilogo
1. Start recording with `powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -OutputFile demo_05_closure.mp4`.
2. Set Order to "Ready for Pickup".
3. Close Order (Completed).
4. Generate and share the status PDF from the order detail.
5. End recording.

### Demo 06: Funzioni Admin Avanzate (Export ZIP, Ripristino e Pulizia Dati)
1. Start recording with `powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -OutputFile demo_06_admin.mp4`.
2. Go to Config > Backup.
3. Export ZIP Backup.
4. Perform Data Reset (Clean app).
5. End recording.
