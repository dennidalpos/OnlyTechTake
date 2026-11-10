# Google Play Data Safety — OnlyTechTake

Use this guide for Full and Demo, which have separate package identifiers. The publisher must verify the final artifact and SDK behavior before submitting declarations. The [Google Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) was consulted on 2026-11-05; it does not establish this application's compliance.

## Declarations

Google distinguishes off-device collection from processing that remains on the device. Third-party SDKs must also be assessed. Transfers to another app can be sharing, including on-device transfers; exceptions such as expected user-initiated transfers must be checked for each flow. Internal-only testing is exempt from the Data Safety form; other covered tracks require declarations even for apps reporting no collection.

The inspected main manifest has no `INTERNET` permission and the source has no proprietary backend or analytics integration. The app explicitly opens WhatsApp, Android sharing and document-picker exports. A selected provider can send a file to cloud storage. These observations do not qualify the merged release manifest or all bundled SDKs.

A proposed answer of **No collection/sharing** remains conditional on the publisher verifying the final release and the manual-transfer exceptions. Local storage alone is not a declaration of collected data.

## Local data and protection

| Local contents | Purpose and limits |
|---|---|
| Customer contacts, business/tax identifiers and addresses | Customer records and manually initiated communication |
| Service-order descriptions, notes, estimates and payments | Job management |
| Photos, videos, optional microphone audio and signatures | Technical documentation and recorded acceptance |
| Local operator credentials and audit history | Authentication and traceability |
| PDF, XLSX, JSON and ZIP exports | Explicit document sharing and backup |

SQLCipher encrypts the database at rest. Media, signatures, PDF and DataStore files are separate private files; private app storage is not file encryption. ZIP password protection currently covers database entries only. XLSX/PDF/JSON export paths do not apply password encryption. See [backup and restore](../BACKUP_FLOW.md).

Local reset does not delete copies exported to other apps/providers. Local operator accounts are not developer-hosted cloud accounts. Database encryption does not demonstrate transport encryption. Check the final merged permissions, including `CAMERA` and `RECORD_AUDIO`.

## Before submission

Use the [English Play Console guide](PLAY_CONSOLE_GUIDE_EN.md) or its [Italian version](PLAY_CONSOLE_GUIDE_IT.md). Confirm the publisher's actual privacy contact, public policy URL, in-app access and artifact behavior. Do not invent publisher details or derive general compliance from this source inspection.
