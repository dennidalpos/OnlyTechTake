# Screenshot, video and brand tools

These PowerShell scripts capture the real Android application and prepare marketing outputs. Use synthetic data and a selected emulator/test device. Capture and distribution are separate review steps.

| Script | Behavior and principal options |
|---|---|
| `create_demo_assets.ps1` | Uses `demo/assets/profiles/*.asset.json`, runs instrumented scenarios, then processes screenshots. Options include `-AssetProfile`, `-Flavor`, `-Locale`, `-Dark`, `-UseConnectedDevice`, `-SkipBuild`, `-SkipTest`, `-OutputRoot`, `-NonInteractive`, `-NoDistribute`. |
| `record_demo_device.ps1` | Records scenarios from a test device; supports profile selection, seeded data, duration/trimming controls and WhatsApp-oriented encoding. FFmpeg performs supported post-processing. |
| `regenerate_public.ps1` | Regenerates public material across profiles. Its cleanup/distribution path removes or replaces existing asset files; it is not a safe verification command. |
| `generate_static_brands.ps1` | Derives raster icons/favicons and social graphics from SVG sources. Uses ImageMagick when available; reports manual work otherwise. |

Profiles define capture test classes/methods, device directories and output variants. Generated captures normally start under `asset/<profile>/`; distribution can write into `marketing/` and `landing/assets/`. `-NoDistribute` prevents public-folder distribution for the screenshot wrapper, but does not make device capture read-only.

Before running a capture, inspect its selected profile and output paths. Before publication, review provenance, privacy, actual feature behavior, screenshot language and destination. Retain originals and hashes; do not delete or overwrite existing official material without a concrete approved scope.

For prerequisites, see [Android scripts](../android/README.md). See [marketing assets](../../../marketing/README.md) for maintained destinations.
