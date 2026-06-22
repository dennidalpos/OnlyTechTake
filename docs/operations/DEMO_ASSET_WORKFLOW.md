# Demo Asset Workflow

This document is the source of truth for OnlyTechTake screenshot and video generation. Update it whenever repository changes affect captured UI, navigation, copy, test tags, permissions, seed data, scenario timing, expected files, or output paths.

## Product Roles

Asset generation is flavor-aware and profile-driven. Flavor and commercial role must not be treated as synonyms:

- `Full` is the **Pro** product. It supports `store-pro` and `social-pro` profiles.
- `Demo` is a limited trial. It supports `store-demo` and `social-demo` profiles.
- `Showcase` is a guided presentation build. It supports the `showcase-representanza` profile.

WhatsApp must always be represented as a manual handoff: the app prepares the message, then the operator reviews and sends it in WhatsApp. Do not describe backend sync, proprietary cloud, WhatsApp Business API, automatic replies, or automatic sending unless they are actually implemented.

## Pipeline Map

### Shared preparation

Both public scripts:

1. Resolve the repository root and Android environment.
2. Load `scripts/powershell/support/android_demo.ps1`.
3. Select the `OnlyTechTakeDemo` emulator or one connected physical device.
4. Build and install the selected `Full`, `Demo`, or `Showcase` debug APK and matching Android-test APK unless `-SkipBuild` is used.
5. Clear the selected debug package before running the scenario.
6. Execute Android instrumentation through `AndroidJUnitRunner`.

### Asset Profiles (Manifests)

Asset generation is now driven by JSON manifests in `demo/assets/profiles/*.asset.json`. Each profile defines:
- `id`: Unique identifier (e.g., `store-pro`).
- `flavor`: Required product flavor (Full, Demo, Showcase).
- `testClass`: Instrumented test to execute.
- `cases`: Specific scenarios with expected files and output mappings.
- `outputs`: List of target platforms (instagram, facebook, whatsapp, play-store, linkedin) and variants (vertical, square, landscape).

### Screenshot and assembled-video flow

Command:

```powershell
# Generate Play Store assets for Pro
powershell -ExecutionPolicy Bypass -File scripts\powershell\assets\create_demo_assets.ps1 -AssetProfile store-pro -Flavor Full

# Generate Social assets for Demo
powershell -ExecutionPolicy Bypass -File scripts\powershell\assets\create_demo_assets.ps1 -AssetProfile social-demo -Flavor Demo
```

Flow:

1. `create_demo_assets.ps1` loads the requested `-AssetProfile` manifest.
2. Instrumented tests write raw PNG files under the app external files directory.
3. The script pulls those directories and processes images according to the defined `outputs`.
4. Output is written to `reports/generated-assets/<AssetProfile>/<Platform>/<Variant>/`.

| Profile | Target Flavor | Primary Test Class | Purpose |
|---|---|---|---|
| **store-pro** | Full | `StoreProScreenshotTest` | Play Store Pro screenshots and video. |
| **store-demo** | Demo | `StoreDemoScreenshotTest` | Play Store Demo screenshots. |
| **social-pro** | Full | `SocialProAssetTest` | Multichannel social assets (IG, FB, WA, LI). |
| **social-demo** | Demo | `SocialDemoAssetTest` | Limited social assets for trial version. |
| **showcase-representanza** | Showcase | `ShowcaseRepresentanzaScreenshotTest` | Presentation highlights and demo clips. |

### Direct device-recording flow

Command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\powershell\assets\record_demo_device.ps1 -AssetProfile store-pro
```

Flow:

1. The script offers manual recording or an automated scenario based on the selected profile.
2. For automated flows, it starts `adb screenrecord` and launches the profile's test class with `demoRecording=true`.
3. The script stops recording and pulls the MP4 into `reports/generated-assets/<AssetProfile>/device/raw/`.

## Change Impact Checklist

For every relevant repository change:

- Map each changed screen or workflow to the relevant test class and asset profile.
- Update profile manifests in `demo/assets/profiles/` if expected files or output paths change.
- Update action lookup to stable `RepairAppTestTags`; keep text assertions only when validating user-visible copy.
- Update story JSON captions, ordering, durations, and output names when captured content changes.
- Run the smallest affected case first, then the complete profile when shared navigation or helpers changed.
- Confirm generated PNG/MP4 files exist, are readable, have expected dimensions, and show the intended final state.
- Keep generated output under ignored paths and remove temporary artifacts.

## Asset Impact Register

This register lists only active demo-asset residuals.

| ID | Description | Impacted assets | Status |
|---|---|---|---|
| R02 | Nuova Home Pro/Demo/Showcase | Tutti | not run |
| R03 | AssetProfile e directory dinamiche | Tutti | not run |

Use concise validation values such as `not run`, `compile only`, `screenshots verified`, or `screenshots and recording verified`.

## Residual Sync

1. Convert unresolved residuals into todos in `PROJECT_STATUS.json`.
2. Remove the todo when the register records successful verification and no work remains.
