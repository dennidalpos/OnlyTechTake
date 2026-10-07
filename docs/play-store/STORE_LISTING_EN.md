# Store listing — English

Drafts for review in Play Console. Full and Demo are separate apps; Showcase is outside store distribution.

## OnlyTechTake Pro

**App name:** OnlyTechTake Pro
**Short description:** Local order management, customer records and technical documentation.

**Full description:**
OnlyTechTake manages repair orders on your Android device, from intake and diagnosis through quotes, repair and delivery.

- Customer records, search and repair history.
- Photos, videos with audio and handwritten signatures on screen.
- Intake and summary PDFs, plus XLSX order exports.
- WhatsApp message templates opened and sent manually by the user.
- Local admin and operator accounts with permissions.
- Local ZIP backup and restore, subject to the limits below.

SQLCipher encrypts the database. Media, documents and preferences are separate private files. The inspected code has no proprietary backend, automatic cloud synchronization or analytics. Sharing and export happen at the user's request through the chosen app or document provider, which may be a cloud service.

**Backup limitations:** The current ZIP password protects database entries only; configuration, media and preferences remain unencrypted. Standalone XLSX/PDF/JSON exports are not encrypted by the app. Protect exported copies separately. Restore requires the original installation's database key and a compatible schema; migration to a new phone is not guaranteed. Restore qualification and recovery fixes remain necessary before production release.

**Proposed category:** Business / Productivity.

## OnlyTechTake Demo

**App name:** OnlyTechTake Demo
**Short description:** Try the local OnlyTechTake repair-order workflow.

**Full description:**
Try intake and management of one repair order, with customer records, photos/videos and demonstration PDFs.

- Maximum one order stored in the app.
- A local admin and up to three active operators.
- XLSX/ZIP exports and restore reserved for Full.
- Watermarked Demo PDFs.

The order limit includes closed orders, not just active ones. Local processing and manual sharing follow the Pro model described above.
