# OnlyTechTake — Release notes v1.4.0

Draft notes matching the current release configuration (`versionCode = 1`). Google Play publication is handled by the owner and remains unverified.

## Features

- Local customer and repair-order management from intake through delivery.
- SQLCipher database encryption and private app storage for media/documents.
- Photo/video capture, handwritten signatures and PDF documents.
- Manual WhatsApp message preparation and Android document sharing.
- Full: order XLSX export and local ZIP backup/restore.
- Demo: one order, up to three active operators, restricted export/restore and watermarked PDFs.

## Requirements

Minimum API 26; target API 37. English fallback and Italian localization.

## Limitations

ZIP password protection currently covers database entries only, not configuration, media or DataStore. Standalone XLSX/PDF/JSON exports are not password encrypted by the app. Keep exported copies secure and delete them separately when needed.

Restore requires the original installation's usable database key and matching schema. New-device migration or recovery after key loss is not supported. Restore failure recovery and key preservation remain pending in `PROJECT_STATUS.json`; resolve these before production readiness claims.

WhatsApp and other chosen sharing/document-provider apps handle user-requested transfers under their own policies. Local AAB verification does not establish runtime behavior on target devices or Play approval.
