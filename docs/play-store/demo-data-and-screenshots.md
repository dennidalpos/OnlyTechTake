# Demo Mode and Screenshot Guide

This document describes how to use the Demo Mode to capture clean screenshots for the Google Play Store and marketing materials.

## How to Start Demo Mode

1. **Build requirements**: Use the current `fullDebug` build for screenshots unless the release flow specifically requires another variant.
2. **Activation**:
   - Open the app.
   - On the **Login** screen, you will see a button "Avvia Modalità Demo" (Start Demo Mode).
   - Tap it to enter the app with a pre-configured demo account and realistic dummy data.
3. **Exit**: To exit Demo Mode, simply **Logout** from the app dashboard.

## Demo Data Details

The following realistic data is populated in Demo Mode:

- **Customer**: Marco Rossi
- **Phone**: +39 333 000 0000
- **Device**: iPhone 13
- **Reported Problem**: "Schermo rotto, touch parzialmente funzionante"
- **Diagnosis**: "Display da sostituire, preventivo approvato"
- **Order ID**: OTT-2026-001
- **Repair Price**: €89,00

## Recommended Screenshots

Capture the following screens in order to tell a complete story:

1. **Dashboard**: Show the overview of active repairs.
2. **Order List**: Show the list containing the demo order.
3. **Order Detail**: Show the full details of the iPhone 13 repair.
4. **Diagnosis/Result**: Show the repair estimate and diagnosis details.
5. **Timeline**: Show the progress events of the repair.

## Screenshot Checklist

Before taking screenshots, ensure:
- [ ] Device theme is set to the desired mode (Light/Dark).
- [ ] Status bar is clean (use Android Studio "Demo Mode" for status bar if possible).
- [ ] System navigation bar is hidden or matches the app style.
- [ ] No real personal data is visible.

## Post-Production Overlay Text (Suggestions)

| Screenshot | Suggested Overlay Text (Italian) | Suggested Overlay Text (English) |
| :--- | :--- | :--- |
| Dashboard | Gestisci il tuo laboratorio in un tap | Manage your lab in one tap |
| Order Detail | Dettagli completi per ogni riparazione | Full details for every repair |
| Timeline | Monitora ogni fase del lavoro | Track every stage of the work |
| Price/Estimate | Preventivi chiari e professionali | Clear and professional estimates |

## Privacy Note
Demo mode uses hardcoded data stored in `DemoMode.kt` and returns in-memory demo data from repositories while enabled. No data is sent to external servers.
