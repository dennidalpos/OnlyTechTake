# Demo Asset Workflow

This document is the source of truth for OnlyTechTake screenshot and video generation. Update it whenever repository changes affect captured UI, navigation, copy, test tags, permissions, seed data, scenario timing, expected files, or output paths.

## Pipeline Map

### Shared preparation

Both public scripts:

1. Resolve the repository root and Android environment.
2. Load `scripts/windows/support/android_demo.ps1`.
3. Select the `OnlyTechTakeDemo` emulator or one connected physical device.
4. Build and install `app-full-debug.apk` and `app-full-debug-androidTest.apk` unless `-SkipBuild` is used.
5. Clear `com.onlytechtake.repairapp.debug` before running the selected scenario.
6. Execute Android instrumentation through `AndroidJUnitRunner`.

The shared helper owns device discovery, temporary verifier settings, APK installation, instrumentation execution, screenshot pulling, PNG conversion, and captioned slide creation.

### Screenshot and assembled-video flow

Command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\create_demo_assets.ps1
```

Flow:

1. `create_demo_assets.ps1` selects all assets or a requested `-AssetId` / `-CaseId`.
2. Instrumented tests write raw PNG files under the app external files directory.
3. The script pulls those directories with `adb`.
4. Raw screenshots are converted to 1080x1920 story PNG files.
5. For configurations with a story JSON, captioned slides are assembled into MP4 with `ffmpeg`.
6. Managed output is written below `reports/demo-assets`.

| Asset | Instrumented scenario | Device source | Repository output |
|---|---|---|---|
| Operator intake | `OperatorIntakeDemoScreenshotTest` | `demo-screenshots` | `screenshots/01-operatore-banco` |
| Order intake and closure | `SocialStoryDemoScreenshotTest#capturesOrderIntakeToClosureStories` | `demo-social-screenshots/01-commessa-nuovo-cliente-chiusura` | `screenshots/02-commessa-nuovo-cliente-chiusura` |
| Categories and templates | `SocialStoryDemoScreenshotTest#capturesCategoryAndTemplateStories` | `demo-social-screenshots/02-categorie-template` | `screenshots/03-categorie-template` |
| Template Anchors | Manual verification recommended (Settings -> Templates) | N/A | N/A |
| Customers | `SocialStoryDemoScreenshotTest#capturesPrivateAndBusinessCustomerStories` | `demo-social-screenshots/03-clienti-privato-azienda` | `screenshots/04-clienti-privato-azienda` |
| Maintenance and backup | `SocialStoryDemoScreenshotTest#capturesMaintenanceStories` | `demo-social-screenshots/05-manutenzione-backup` | `screenshots/05-manutenzione-backup` |

`demo/video/operator-intake.story.json` defines the assembled operator video. Any changed filename, order, caption, duration, resolution, or output name must be synchronized with the test and script configuration.

### Direct device-recording flow

Command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1
powershell -ExecutionPolicy Bypass -File scripts\windows\record_demo_device.ps1 -UseConnectedDevice -ScenarioId full-workflow -StartImmediately
```

Flow:

1. The script offers manual recording or an automated scenario.
2. It grants the required runtime permissions and optionally seeds demo data.
3. For automated flows, it starts `adb screenrecord` and launches the selected instrumented test with `demoRecording=true`.
4. The tests use shorter recording pauses while following the same navigation paths used for screenshots.
5. The script stops recording, pulls the MP4 into `reports/demo-assets/videos/device`, and normalizes it with `ffmpeg` when available.
6. Re-running one recording replaces only the selected output file, preserving other MP4 files already present in the device-video output directory.

| Recording choice | Instrumented scenario |
|---|---|
| Full workflow | `OperatorIntakeDemoScreenshotTest` |
| Social stories | `SocialStoryDemoScreenshotTest` |
| Config | `SocialStoryDemoScreenshotTest#capturesCategoryAndTemplateStories` |
| Customers | `SocialStoryDemoScreenshotTest#capturesPrivateAndBusinessCustomerStories` |
| Maintenance | `SocialStoryDemoScreenshotTest#capturesMaintenanceStories` |

## Change Impact Checklist

For every relevant repository change:

- Map each changed screen or workflow to the scenario table above.
- Treat generated asset directories as fully managed output: before recreating screenshots or videos, the script must delete and recreate the whole target directory instead of overwriting files in place.
- Update action lookup to stable `RepairAppTestTags`; keep text assertions only when validating user-visible copy.
- Synchronize expected screenshot filenames and device directories in `create_demo_assets.ps1`.
- Synchronize recording menu entries and test selectors in `record_demo_device.ps1`.
- Update shared build, package, runner, device, pull, or image-processing behavior in `android_demo.ps1` when applicable.
- Update story JSON captions, ordering, durations, and output names when captured content changes.
- Run the smallest affected case first, then the complete flow when shared navigation or helpers changed.
- Confirm generated PNG/MP4 files exist, are readable, have expected dimensions, and show the intended final state.
- Keep generated output under ignored paths and remove temporary artifacts.

## Asset Impact Register

This register lists only active demo-asset residuals. Do not keep completed rows here as history; completed evidence belongs in command output or release notes when explicitly needed, not in this workflow file.

| Change | Affected flow/assets | Required updates | Validation | Residual |
|---|---|---|---|---|
| Pro/Demo store asset split | Pro listing screenshots, Demo listing screenshots/video | Add flavor-aware screenshot/video generation; keep output paths separated by flavor; regenerate Pro and Demo assets with demo-only limits visible where relevant | not run | Tracked in `PROJECT_STATUS.json` block `B10` |

Use concise validation values such as `not run`, `compile only`, `screenshots verified`, or `screenshots and recording verified`. Do not mark a device flow verified unless it actually ran.

## Residual Sync

After updating the register:

1. Convert every unresolved, actionable residual into one prioritized todo in `PROJECT_STATUS.json`.
2. Do not copy completed history, dates, evidence, or check results into this document or the JSON file.
3. Remove the todo when the register records successful verification and no work remains.
