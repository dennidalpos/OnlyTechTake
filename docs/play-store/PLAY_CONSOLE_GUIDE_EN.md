# Play Console preparation

This guide covers Full and Demo. Local preparation does not submit forms or establish Google Play acceptance. Use the [release checklist](release-checklist.md), [Data Safety guide](DATA_SAFETY_GUIDE.md) and [Google's review preparation guidance](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en). The [Italian guide](PLAY_CONSOLE_GUIDE_IT.md) remains available.

## App access

Explain the local login flow in reviewer instructions. A clean Full installation starts administrator setup; reviewers create a local administrator and then sign in with those credentials. Do not provide real shop accounts. No developer-hosted server account is required.

Demo offers administrator setup or demo entry, allows one order and up to three active operators, restricts exports/restore and watermarks PDF output. Showcase is intended for demonstrations outside Play distribution. Verify these flows on the installed final artifact before submitting access instructions.

## App content

The inspected source has no advertising integration. The intended audience is repair professionals; the publisher confirms the actual audience and rating questionnaire. Opening WhatsApp manually does not make this app a proprietary chat service. Review the final artifact's permissions and SDK behavior rather than copying answers from a previous release.

Follow the canonical [Data Safety guide](DATA_SAFETY_GUIDE.md). Do not claim transport encryption from SQLCipher database encryption. ZIP password protection currently covers database entries only; configuration, media and DataStore remain outside that protection. PDF/XLSX/JSON exports have their own limits. Local reset cannot remove already exported copies.

## Privacy policy and permissions

Finalize the publisher's real privacy contact and a working public policy page, with access from the application. Preserve the [Italian policy draft](PRIVACY_POLICY_IT.md) until the publisher approves its details. A developer website and a policy URL are different fields.

The main manifest requests camera and microphone access for media capture and does not request `INTERNET` or broad storage access. Recheck the merged release manifest after dependency changes.

`android:hasFragileUserData="true"` can offer data retention on supported Android uninstall flows; it does not guarantee recovery. SQLCipher keys depend on the installation's Keystore, so do not promise restore to another device or after loss of key material. See [backup limitations](../BACKUP_FLOW.md).
