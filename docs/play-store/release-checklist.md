# Google Play release checklist

Scope: Full (`com.onlytechtake.repairapp`) and Demo (`com.onlytechtake.repairapp.demo`). Showcase is outside Play distribution. The owner handles upload; local AAB preparation does not prove Play acceptance or device readiness. Pending work belongs only in `PROJECT_STATUS.json` as `todos` strings.

## Prepared local artifacts

Current artifacts generated on 2026-10-07 using AGP 9.3.1. The dependency catalog now specifies AGP 9.4.1; that update has not been verified with a new build. Rebuild both variants and refresh hashes before uploading artifacts from the updated checkout. Both pass `bundletool` 1.18.3 validation and JDK 21 `jarsigner -verify`; both declare version 1.4.0 (code 1), min SDK 26, target SDK 37 and are non-debuggable. These files remain ignored build outputs; regenerating them may change their hashes.

| Flavor | Artifact relative to repository | SHA-256 |
|---|---|---|
| Full | `app/build/outputs/bundle/fullRelease/app-full-release.aab` | `C5ED22808F3BA8B1B30A5C2E133B2568B6A66EBBB8B972E6B102BA1E2DA0D299` |
| Demo | `app/build/outputs/bundle/demoRelease/app-demo-release.aab` | `23F72BBEFF862A9B6672604B8B65493B100F4A9305C49E73F090CB07ED843581` |

Merged permissions include camera, microphone, wake lock, network-state access, boot receiver, foreground service and each package's internal dynamic-receiver permission; neither artifact requests `INTERNET`. Network-state access is not Internet access.

The signer is self-signed and has no timestamp; `jarsigner` reports these warnings while verifying both signatures. Google Play upload-key matching/enrollment remains to be checked in Console. Use the [official bundletool guidance](https://developer.android.com/tools/bundletool) for repeat inspection. Gradle release vital-lint checks passed during both builds; device tests and Play acceptance were not performed.
## Official references

Checked on 2026-10-07:

- [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en): new mobile apps and updates require API 36 or higher from 2026-08-31.
- [Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en): on-device processing is excluded from collection; qualifying user-initiated transfers are excluded from sharing. Internal-only testing is exempt from this section.
- [App review preparation](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en): privacy policy, reviewer access and other App content declarations must match app behavior.
- [Internal testing](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en): start with an internal test before wider distribution; acceptance and testing happen in Play Console.

## Local configuration

Read release values from `app/build.gradle.kts`: `versionCode = 1`, `versionName = "1.4.0"`, `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`, JVM toolchain 21. Do not alter package, SDK or versionCode to work around a Console error without the owner's decision.

The main manifest requests `CAMERA` and `RECORD_AUDIO`; no `INTERNET` or broad storage permission is declared there. Check the merged release manifest for SDK contributions. Demo allows one order and up to three active operators; exports and restore are restricted, and PDFs have a watermark. Confirm these limits against the final artifact.

Signing uses local ignored `keystore.properties` and its referenced keystore. Never copy their contents into documentation, logs or Git. Keep the upload key available for future updates.

If the installed Android Studio JBR is unsupported by the wrapper, set process `JAVA_HOME` to an existing JDK 21 before running it. A cached Gradle JDK is suitable if verified; the wrapper does not search that cache automatically. No system environment change is required.

Generate the two artifacts in sequence:

```powershell
./scripts/powershell/android/build_installer.ps1 -Flavor Full -BuildType Release -OutputType Aab -NonInteractive
./scripts/powershell/android/build_installer.ps1 -Flavor Demo -BuildType Release -OutputType Aab -NonInteractive
```

Expected outputs:

- `app/build/outputs/bundle/fullRelease/app-full-release.aab`
- `app/build/outputs/bundle/demoRelease/app-demo-release.aab`

Before handoff, verify each archive's package/version, merged permissions and signature. Record SHA-256 hashes for the exact files handed off. A successful bundle build does not validate runtime flows, Play signing enrollment or whether versionCode 1 has already been used.

## Owner upload and release checks

1. Select the correct separate Play apps and confirm package identifiers and unused versionCode. Enroll/verify Play App Signing and upload-key acceptance.
2. Upload the corresponding signed AAB to the chosen track. Use internal testing first unless a different release plan is agreed.
3. Use [reviewer instructions](PLAY_CONSOLE_GUIDE_IT.md) and the canonical [Data Safety guide](DATA_SAFETY_GUIDE.md). Complete App content and Data Safety when required by the track.
4. Finalize publisher privacy contact and a working public HTTPS policy URL, with policy access from the app. The [policy draft](PRIVACY_POLICY_IT.md) needs these publisher details. The landing privacy page still requires alignment before publication.
5. Review separate Full/Demo listing text and assets using [marketing assets](../../marketing/README.md). Use [release notes](../release/release-notes-1.4.0.md) matching the built version.
6. Install via Play on target devices and exercise setup/login, operators, photo/video permissions, PDF, manual WhatsApp/share and supported backup/restore. Review actual pre-launch findings; record actionable failures in the tracker.
7. Resolve the backup/key protection issues tracked as P1 before describing the release as production ready. Media/configuration/DataStore backup entries currently remain unencrypted, cross-device restore is unsupported, and restore rollback does not cover preferences. See [backup flow](../BACKUP_FLOW.md).

Close the upload task only after direct Console acceptance evidence. Remove verified completed entries immediately; retain unresolved tasks without completed history or metadata objects.
